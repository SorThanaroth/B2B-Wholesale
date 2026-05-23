"""End-to-end smoke test for the B2B Wholesale Marketplace API.
Exercises the full happy path from the report: login -> browse -> cart
(with min-qty enforcement) -> checkout/QR -> payment callback -> settlement.
"""
import json, urllib.request, urllib.error, sys

BASE = "http://localhost:8081/api/v1"
ok = 0
fail = 0

def call(method, path, token=None, body=None, raw=False):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            content = r.read()
            return r.status, (content if raw else (json.loads(content) if content else {}))
    except urllib.error.HTTPError as e:
        content = e.read()
        try:
            return e.code, json.loads(content)
        except Exception:
            return e.code, content.decode(errors="replace")

def check(name, cond, detail=""):
    global ok, fail
    if cond:
        ok += 1; print(f"  PASS  {name}")
    else:
        fail += 1; print(f"  FAIL  {name}  {detail}")

print("=== 1) Merchant login ===")
st, login = call("POST", "/auth/login", body={"email": "merchant@b2b.local", "password": "Merchant@12345"})
check("login 200", st == 200, st)
mtok = login.get("accessToken")
check("got access token", bool(mtok))
check("got refresh token", bool(login.get("refreshToken")))
check("role MERCHANT", login.get("role") == "MERCHANT", login.get("role"))

print("=== 2) Browse catalog ===")
st, prods = call("GET", "/products?size=10", mtok)
check("products 200", st == 200, st)
items = prods.get("content", [])
check("catalog non-empty", len(items) >= 4, len(items))
# pick one product from each of two different companies
by_company = {}
for p in items:
    by_company.setdefault(p["companyId"], p)
companies = list(by_company.values())
check("products span >=2 companies", len(companies) >= 2, len(companies))
p1, p2 = companies[0], companies[1]
print(f"     product A: {p1['name']} (min {p1['minOrderQty']} {p1['unit']}) @ {p1['price']} [{p1['companyName']}]")
print(f"     product B: {p2['name']} (min {p2['minOrderQty']} {p2['unit']}) @ {p2['price']} [{p2['companyName']}]")

print("=== 3) Wholesale min-qty enforcement (expect 400 below minimum) ===")
st, resp = call("POST", "/cart/items", mtok, {"productId": p1["id"], "quantity": 1})
check("below-min rejected (400)", st == 400, f"{st} {resp}")

print("=== 4) Add valid quantities from two companies ===")
st, cart = call("POST", "/cart/items", mtok, {"productId": p1["id"], "quantity": p1["minOrderQty"]})
check("add A 200", st == 200, st)
st, cart = call("POST", "/cart/items", mtok, {"productId": p2["id"], "quantity": p2["minOrderQty"]})
check("add B 200", st == 200, st)
check("cart grouped by 2 companies", len(cart.get("companies", [])) == 2, cart.get("companies"))
grand = cart.get("grandTotal")
print(f"     cart grandTotal = {grand}, groups = {[(g['companyName'], g['subtotal']) for g in cart['companies']]}")

print("=== 5) Checkout -> unified QR ===")
st, qr = call("POST", "/orders/checkout", mtok)
check("checkout 201", st == 201, f"{st} {qr}")
order_id = qr.get("orderId"); qr_token = qr.get("qrToken")
check("got orderId", bool(order_id))
check("QR payload looks like EMVCo", str(qr.get("qrPayload", "")).startswith("000201"), qr.get("qrPayload", "")[:20])
check("QR image is PNG data URI", str(qr.get("qrImageDataUri", "")).startswith("data:image/png;base64,"))
check("checkout amount equals cart total", str(qr.get("amount")) == str(grand), f"{qr.get('amount')} vs {grand}")

print("=== 6) Payment status before pay (PENDING) ===")
st, stt = call("GET", f"/payments/{order_id}/status", mtok)
check("status 200", st == 200, st)
check("payment PENDING", stt.get("paymentStatus") == "PENDING", stt.get("paymentStatus"))

print("=== 7) Simulate gateway callback (PAID) ===")
st, cb = call("POST", "/payments/callback", body={"reference": qr_token, "status": "PAID"})
check("callback 200", st == 200, f"{st} {cb}")

print("=== 8) Payment + order now PAID, splits PENDING_SETTLEMENT ===")
st, stt = call("GET", f"/payments/{order_id}/status", mtok)
check("payment PAID", stt.get("paymentStatus") == "PAID", stt.get("paymentStatus"))
check("order status PAID", stt.get("orderStatus") == "PAID", stt.get("orderStatus"))
st, detail = call("GET", f"/orders/{order_id}", mtok)
check("order detail 200", st == 200, st)
splits = detail.get("splits", [])
check("two company splits", len(splits) == 2, len(splits))
check("all splits PENDING_SETTLEMENT",
      all(s["paymentStatus"] == "PENDING_SETTLEMENT" for s in splits),
      [s["paymentStatus"] for s in splits])
sum_splits = sum(float(s["subtotal"]) for s in splits)
check("splits sum == order total", abs(sum_splits - float(detail["totalAmount"])) < 0.001,
      f"{sum_splits} vs {detail['totalAmount']}")

print("=== 9) Idempotent duplicate callback ===")
st, cb = call("POST", "/payments/callback", body={"reference": qr_token, "status": "PAID"})
check("duplicate callback still 200 (no-op)", st == 200, st)

print("=== 10) Invoice PDF ===")
st, pdf = call("GET", f"/orders/{order_id}/invoice", mtok, raw=True)
check("invoice 200", st == 200, st)
check("invoice is a PDF", isinstance(pdf, (bytes, bytearray)) and pdf[:4] == b"%PDF", pdf[:8] if isinstance(pdf, bytes) else pdf)

print("=== 11) Admin login + settlement dashboard ===")
st, alogin = call("POST", "/auth/login", body={"email": "admin@b2b.local", "password": "Admin@12345"})
check("admin login 200", st == 200, st)
atok = alogin.get("accessToken")
check("admin role ADMIN", alogin.get("role") == "ADMIN", alogin.get("role"))
st, setts = call("GET", "/admin/settlements?status=PENDING_SETTLEMENT", atok)
check("settlements 200", st == 200, st)
check("settlements listed", setts.get("totalElements", 0) >= 2, setts.get("totalElements"))

print("=== 12) Merchant blocked from admin endpoint (403) ===")
st, _ = call("GET", "/admin/settlements", mtok)
check("merchant -> admin = 403", st == 403, st)

print("=== 13) Settle one split ===")
order_splits = call("GET", f"/admin/settlements/{order_id}", atok)[1]
split_id = order_splits[0]["splitId"]
st, settled = call("PUT", f"/admin/settlements/{split_id}/settle", atok)
check("settle 200", st == 200, f"{st} {settled}")
check("split SETTLED", settled.get("status") == "SETTLED", settled.get("status"))

print("=== 14) Admin dashboard reflects activity ===")
st, dash = call("GET", "/admin/dashboard", atok)
check("dashboard 200", st == 200, st)
check("revenue > 0", float(dash.get("totalRevenue", 0)) > 0, dash.get("totalRevenue"))
check("paidOrders >= 1", dash.get("paidOrders", 0) >= 1, dash.get("paidOrders"))
print(f"     dashboard = {dash}")

print("=== 15) Register a brand-new merchant ===")
import random
em = f"new{random.randint(1000,9999)}@b2b.local"
st, reg = call("POST", "/auth/register", body={"fullName": "New Merchant", "email": em, "password": "Secret@123"})
check("register 201", st == 201, f"{st} {reg}")
check("new merchant gets tokens", bool(reg.get("accessToken")))

print(f"\n==== RESULT: {ok} passed, {fail} failed ====")
sys.exit(1 if fail else 0)
