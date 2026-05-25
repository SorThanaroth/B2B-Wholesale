# B2B Wholesale Marketplace — Backend (MVP v1.0)

Spring Boot REST API implementing the *B2B Wholesale Marketplace System Report*: a
multi-vendor wholesale platform where merchants browse products from many supplier
companies, pay once via a **unified QR code**, and the system splits the payment into
a per-company **settlement** record behind the scenes.

- **Stack:** Java 21, Spring Boot 4, Spring Security + JWT, Spring Data JPA, Flyway,
  PostgreSQL 15, Redis, PDFBox (invoices), ZXing (QR), Docker Compose.
- **Architecture:** modular monolith — each domain lives under
  `com.wholesale.marketplace.modules.<module>` with its own entities, repository,
  service, controller, and DTOs.

## Quick start

```bash
# 1. Start Postgres + Redis (Postgres is published on host port 5544)
docker compose up -d

# 2. Run the API (defaults to :8080; override if that port is busy)
SERVER_PORT=8081 ./mvnw spring-boot:run     # PowerShell: $env:SERVER_PORT=8081; .\mvnw spring-boot:run
```

On first boot against an empty DB, Flyway creates the schema and a seeder loads demo
data (disable with `app.seed.enabled=false`):

| Role     | Email                | Password         | Notes                              |
|----------|----------------------|------------------|------------------------------------|
| Admin    | `admin@b2b.local`    | `Admin@12345`    | Full platform management           |
| Merchant | `merchant@b2b.local` | `Merchant@12345` | Wholesale buyer                    |
| Supplier | `supplier@b2b.local` | `Supplier@12345` | Rep for *Angkor Water Co.* (v1.1)  |

Health check: `GET /actuator/health`.

> **Roles & accounts (v1.1).** Three roles: **MERCHANT** (buyer), **SUPPLIER** (a company/seller —
> `users.company_id`, migration `V3`), **ADMIN**.
>
> *Two ways to get an account:*
> - **Self-registration** (`POST /api/v1/auth/register`) with a `role` of `MERCHANT` or `SUPPLIER`.
>   A supplier signup also creates its company inline (`companyName` + `bankAccount`). All
>   self-registered accounts are created **PENDING** (supplier companies **INACTIVE**) and **cannot
>   sign in until an admin approves them** — `register` returns a pending message, not tokens.
>   Login on a non-approved account returns a clear `403` (`DisabledException` → "pending approval").
> - **Admin-created** (active immediately): `POST /api/v1/admin/users/merchant` and
>   `POST /api/v1/admin/users/supplier` (bound to an existing company).
>
> *Approval:* `PUT /api/v1/admin/users/{id}/status` → `ACTIVE`. Approving a SUPPLIER also activates
> its company automatically. List by role with `GET /api/v1/admin/users?role=SUPPLIER`.
>
> Supplier portal endpoints live under `/api/v1/supplier/**` (own products/orders/settlements,
> company-scoped). The demo admin/merchant are seeded only on a fresh DB; the demo **supplier is
> backfilled idempotently** on every boot (so `supplier@b2b.local` always logs in as SUPPLIER).
>
> *Supplier review (v1.1).* Suppliers submit company KYC detail at registration — business
> registration no., phone, address, description (migration `V4`) plus settlement bank account.
> Admins review the full company via `GET /api/v1/companies/{id}/admin` (any status) before approving.

> **Note on roles:** the login response's `role` is taken verbatim from the `users.role` column. If a
> supplier email logs in as MERCHANT, that row is MERCHANT — typically because it was created by an
> older build or the server/DB wasn't rebuilt/migrated. Rebuild + restart so Flyway applies `V3`/`V4`
> and the seeder backfills the demo supplier; create custom suppliers via self-registration (role
> SUPPLIER) or the admin **Suppliers** screen.

## CI/CD & Docker

- **`Dockerfile`** — multi-stage build (Temurin 21 JDK → JRE), produces a runnable jar image on `:8082`.
- **`.github/workflows/ci.yml`** (at this Backend repo's root) — on push/PR: JDK 21 + Maven cache,
  spins up **Postgres + Redis** service containers, runs `./mvnw -B verify` (the `contextLoads` test
  boots the full app), and uploads the jar. On push to `main`/`master` it builds and pushes the image
  to **GHCR** (`ghcr.io/<owner>/<repo>`).

  > GitHub Actions only runs workflows from `.github/workflows/` at a repository root, so this assumes
  > **Backend is its own Git repo**. If instead Backend + Frontend share one repo, move both `ci.yml`
  > files to the root `.github/workflows/` and scope them with `paths:` + `working-directory`.

## End-to-end smoke test

With the app running, `python smoke_test.py` exercises the full happy path
(login → browse → cart with min-qty enforcement → checkout/QR → payment callback →
settlement split → invoice → admin dashboard). Point it at your port via the `BASE`
constant if not on 8081.

## The payment / settlement flow (Section 8)

1. `POST /api/v1/orders/checkout` turns the active cart into an **Order**, snapshot
   **order_items**, and **one `order_company_splits` row per company**; it reserves
   stock and returns a single unified QR (EMVCo payload + PNG) for the grand total.
2. The merchant pays; the bank calls `POST /api/v1/payments/callback` (public webhook).
   To simulate it: `{"reference":"<order qr_token>","status":"PAID"}`.
3. On `PAID` the order flips to `PAID` and **every split → `PENDING_SETTLEMENT`**.
4. An admin reviews `GET /api/v1/admin/settlements` and marks each payout
   `PUT /api/v1/admin/settlements/{splitId}/settle` → `SETTLED`.

The KHQR/Bakong gateway is mocked (`MockKhqrGateway` implements the `PaymentGateway`
interface) so the flow runs without real bank credentials; a real adapter can drop in.

## API surface

Base URL `/api/v1`. Bearer JWT on everything except `/auth/**` and `/payments/callback`.
Admin-only routes are guarded by role. Implements Sections 9.1–9.11 of the report:
auth, users/profile/addresses, companies, categories, products (filter/search + CSV
import), cart, orders (+ invoice PDF), payments, settlements, admin user management,
and dashboards/reports. See the report for the full endpoint table.

## Configuration (env overrides)

| Var | Default |
|-----|---------|
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://localhost:5544/b2b_wholesale` |
| `DB_USER` / `DB_PASSWORD` | `user` / `password` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `JWT_SECRET` | dev secret (set in prod!) |

## Notes / follow-ups

- Email delivery (verification / password reset) is **simulated** — the link is logged.
- Swagger/OpenAPI (`springdoc`) was deferred pending a Spring Boot 4-compatible release.
- Real KHQR/Bakong integration, notifications, and the React SPA are the next passes.
