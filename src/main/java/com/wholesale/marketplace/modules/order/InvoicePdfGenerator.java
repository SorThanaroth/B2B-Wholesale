package com.wholesale.marketplace.modules.order;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.modules.order.dto.OrderDetailDto;
import com.wholesale.marketplace.modules.order.dto.OrderItemDto;
import com.wholesale.marketplace.modules.order.dto.OrderSplitDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders an order into a clean, professional PDF invoice: branded header, order
 * meta, an itemized table grouped by supplier company, a totals box, and a
 * per-company settlement (payout + delivery) summary.
 */
@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    // Palette (0–1 RGB) — mirrors the frontend brand theme.
    private static final float[] NAVY = {0.122f, 0.227f, 0.373f};    // #1F3A5F
    private static final float[] TEAL = {0.122f, 0.545f, 0.651f};    // #1F8BA6
    private static final float[] HEAD_BG = {0.945f, 0.957f, 0.976f}; // light row
    private static final float[] MUTED = {0.42f, 0.45f, 0.50f};
    private static final float[] LINE = {0.85f, 0.87f, 0.90f};
    private static final float[] INK = {0.15f, 0.18f, 0.22f};

    private static final float MARGIN = 45f;
    private static final float BOTTOM = 60f;

    // Column right-edges for the items table (set per-document).
    private float colQty, colUnit, colAmount, contentRight;

    public byte[] generate(OrderDetailDto order) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            Cursor cur = new Cursor(doc);
            float width = cur.page.getMediaBox().getWidth();
            contentRight = width - MARGIN;
            colAmount = contentRight;
            colUnit = contentRight - 95;
            colQty = contentRight - 180;

            drawHeader(cur, bold, normal, width);
            drawMeta(cur, bold, normal, order);
            drawItems(cur, bold, normal, order);
            drawTotal(cur, bold, order);
            drawSettlement(cur, bold, normal, order);
            drawFooter(cur, normal);

            cur.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate invoice: " + ex.getMessage());
        }
    }

    // ----- sections ------------------------------------------------------

    private void drawHeader(Cursor cur, PDFont bold, PDFont normal, float width) throws Exception {
        float bandH = 84f;
        float top = cur.page.getMediaBox().getHeight();
        cur.rect(0, top - bandH, width, bandH, NAVY);
        cur.text(bold, 20, MARGIN, top - 38, "B2B Wholesale Marketplace", 1, 1, 1);
        cur.text(normal, 10, MARGIN, top - 56, "Multi-vendor wholesale  •  Unified QR checkout",
                0.8f, 0.86f, 0.94f);
        cur.textRight(bold, 22, contentRight, top - 44, "INVOICE", 1, 1, 1);
        cur.y = top - bandH - 28;
    }

    private void drawMeta(Cursor cur, PDFont bold, PDFont normal, OrderDetailDto order) throws Exception {
        float labelX = MARGIN, valueX = MARGIN + 95;
        cur.metaRow(bold, normal, labelX, valueX, "Invoice no.", "#" + shortId(order.id()));
        cur.metaRow(bold, normal, labelX, valueX, "Date", DATE.format(order.createdAt()));
        cur.metaRow(bold, normal, labelX, valueX, "Order status", pretty(order.status().name()));
        cur.metaRow(bold, normal, labelX, valueX, "Payment", pretty(order.paymentStatus().name()));
        cur.y -= 10;
    }

    private void drawItems(Cursor cur, PDFont bold, PDFont normal, OrderDetailDto order) throws Exception {
        cur.sectionTitle(bold, "Items");

        cur.ensureSpace(40);
        cur.rect(MARGIN, cur.y - 16, contentRight - MARGIN, 18, HEAD_BG);
        float baseline = cur.y - 12;
        cur.text(bold, 9, MARGIN + 6, baseline, "DESCRIPTION", MUTED[0], MUTED[1], MUTED[2]);
        cur.textRight(bold, 9, colQty, baseline, "QTY", MUTED[0], MUTED[1], MUTED[2]);
        cur.textRight(bold, 9, colUnit, baseline, "UNIT", MUTED[0], MUTED[1], MUTED[2]);
        cur.textRight(bold, 9, colAmount - 6, baseline, "AMOUNT", MUTED[0], MUTED[1], MUTED[2]);
        cur.y -= 22;

        // Group line items by supplier company.
        Map<String, List<OrderItemDto>> byCompany = new LinkedHashMap<>();
        for (OrderItemDto item : order.items()) {
            byCompany.computeIfAbsent(safe(item.companyName(), "Supplier"), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<OrderItemDto>> group : byCompany.entrySet()) {
            cur.ensureSpace(30);
            cur.text(bold, 9.5f, MARGIN + 2, cur.y, group.getKey(), TEAL[0], TEAL[1], TEAL[2]);
            cur.y -= 15;

            BigDecimal groupTotal = BigDecimal.ZERO;
            for (OrderItemDto item : group.getValue()) {
                cur.ensureSpace(18);
                cur.text(normal, 9.5f, MARGIN + 10, cur.y, clip(item.productName(), 52), INK[0], INK[1], INK[2]);
                cur.textRight(normal, 9.5f, colQty, cur.y, String.valueOf(item.quantity()), INK[0], INK[1], INK[2]);
                cur.textRight(normal, 9.5f, colUnit, cur.y, money(item.unitPrice()), INK[0], INK[1], INK[2]);
                cur.textRight(normal, 9.5f, colAmount - 6, cur.y, money(item.subtotal()), INK[0], INK[1], INK[2]);
                cur.y -= 15;
                groupTotal = groupTotal.add(item.subtotal());
            }
            cur.textRight(normal, 9, colUnit, cur.y, "Subtotal", MUTED[0], MUTED[1], MUTED[2]);
            cur.textRight(bold, 9, colAmount - 6, cur.y, money(groupTotal), INK[0], INK[1], INK[2]);
            cur.y -= 10;
            cur.hline(cur.y);
            cur.y -= 12;
        }
    }

    private void drawTotal(Cursor cur, PDFont bold, OrderDetailDto order) throws Exception {
        cur.ensureSpace(40);
        float boxW = 220, boxH = 30, boxX = contentRight - boxW;
        cur.rect(boxX, cur.y - boxH + 8, boxW, boxH, NAVY);
        cur.text(bold, 11, boxX + 12, cur.y - 8, "TOTAL", 1, 1, 1);
        cur.textRight(bold, 13, contentRight - 12, cur.y - 9, money(order.totalAmount()), 1, 1, 1);
        cur.y -= boxH + 16;
    }

    private void drawSettlement(Cursor cur, PDFont bold, PDFont normal, OrderDetailDto order) throws Exception {
        if (order.splits().isEmpty()) return;
        cur.sectionTitle(bold, "Settlement by supplier");
        cur.ensureSpace(20);
        float baseline = cur.y;
        cur.text(bold, 8.5f, MARGIN + 2, baseline, "SUPPLIER", MUTED[0], MUTED[1], MUTED[2]);
        cur.text(bold, 8.5f, MARGIN + 230, baseline, "DELIVERY", MUTED[0], MUTED[1], MUTED[2]);
        cur.text(bold, 8.5f, MARGIN + 320, baseline, "PAYOUT", MUTED[0], MUTED[1], MUTED[2]);
        cur.textRight(bold, 8.5f, colAmount - 6, baseline, "SHARE", MUTED[0], MUTED[1], MUTED[2]);
        cur.y -= 8;
        cur.hline(cur.y);
        cur.y -= 13;

        for (OrderSplitDto split : order.splits()) {
            cur.ensureSpace(16);
            cur.text(normal, 9.5f, MARGIN + 2, cur.y, clip(safe(split.companyName(), "(unknown)"), 34), INK[0], INK[1], INK[2]);
            cur.text(normal, 9, MARGIN + 230, cur.y, pretty(split.fulfillmentStatus().name()), INK[0], INK[1], INK[2]);
            cur.text(normal, 9, MARGIN + 320, cur.y, pretty(split.paymentStatus().name()), INK[0], INK[1], INK[2]);
            cur.textRight(normal, 9.5f, colAmount - 6, cur.y, money(split.subtotal()), INK[0], INK[1], INK[2]);
            cur.y -= 15;
        }
    }

    private void drawFooter(Cursor cur, PDFont normal) throws Exception {
        cur.hline(BOTTOM + 22);
        cur.text(normal, 8, MARGIN, BOTTOM + 8,
                "Merchants pay once via a unified QR code; each supplier receives its exact share. "
                        + "Suppliers are settled after delivery is confirmed.",
                MUTED[0], MUTED[1], MUTED[2]);
    }

    // ----- formatting helpers -------------------------------------------

    private static String money(BigDecimal v) {
        return "$" + String.format("%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    private static String pretty(String enumName) {
        String s = enumName.toLowerCase().replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    /**
     * Stateful drawing cursor that owns the current page + content stream and the
     * vertical position, and transparently starts a new page when space runs out.
     */
    private final class Cursor {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        Cursor(PDDocument doc) throws Exception {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws Exception {
            if (cs != null) cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void ensureSpace(float needed) throws Exception {
            if (y - needed < BOTTOM) {
                newPage();
            }
        }

        void text(PDFont font, float size, float x, float y, String s, float r, float g, float b) throws Exception {
            cs.setNonStrokingColor(r, g, b);
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(s == null ? "" : s);
            cs.endText();
        }

        void textRight(PDFont font, float size, float xRight, float y, String s,
                       float r, float g, float b) throws Exception {
            String t = s == null ? "" : s;
            float w = font.getStringWidth(t) / 1000f * size;
            text(font, size, xRight - w, y, t, r, g, b);
        }

        void rect(float x, float y, float w, float h, float[] rgb) throws Exception {
            cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        void hline(float atY) throws Exception {
            cs.setStrokingColor(LINE[0], LINE[1], LINE[2]);
            cs.setLineWidth(0.6f);
            cs.moveTo(MARGIN, atY);
            cs.lineTo(contentRight, atY);
            cs.stroke();
        }

        void sectionTitle(PDFont bold, String title) throws Exception {
            ensureSpace(30);
            text(bold, 12, MARGIN, y, title, NAVY[0], NAVY[1], NAVY[2]);
            y -= 18;
        }

        void metaRow(PDFont bold, PDFont normal, float labelX, float valueX,
                     String label, String value) throws Exception {
            ensureSpace(16);
            text(normal, 9.5f, labelX, y, label, MUTED[0], MUTED[1], MUTED[2]);
            text(bold, 9.5f, valueX, y, value, INK[0], INK[1], INK[2]);
            y -= 15;
        }

        void close() throws Exception {
            if (cs != null) cs.close();
        }
    }
}
