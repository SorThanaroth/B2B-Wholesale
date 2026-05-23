package com.wholesale.marketplace.modules.order;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.modules.order.dto.OrderDetailDto;
import com.wholesale.marketplace.modules.order.dto.OrderItemDto;
import com.wholesale.marketplace.modules.order.dto.OrderSplitDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Renders an order into a simple, itemized-by-company PDF invoice (Section 7.1). */
@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public byte[] generate(OrderDetailDto order) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = line(cs, bold, 18, margin, y, "B2B Wholesale Marketplace");
                y = line(cs, normal, 11, margin, y - 4, "TAX INVOICE / RECEIPT");
                y -= 10;
                y = line(cs, normal, 10, margin, y, "Invoice No : " + order.id());
                y = line(cs, normal, 10, margin, y, "Date       : " + DATE.format(order.createdAt()));
                y = line(cs, normal, 10, margin, y, "Status     : " + order.status() + " / " + order.paymentStatus());
                y -= 8;

                y = line(cs, bold, 12, margin, y, "Items");
                y = line(cs, normal, 9, margin, y, String.format("%-34s %6s %12s %12s",
                        "Product", "Qty", "Unit Price", "Subtotal"));
                y = hr(cs, margin, y, page.getMediaBox().getWidth() - margin);
                for (OrderItemDto item : order.items()) {
                    y = line(cs, normal, 9, margin, y, String.format("%-34s %6d %12s %12s",
                            clip(item.productName(), 34), item.quantity(),
                            item.unitPrice().toPlainString(), item.subtotal().toPlainString()));
                }
                y -= 6;
                y = line(cs, bold, 11, margin, y, "TOTAL: " + order.totalAmount().toPlainString() + " USD");
                y -= 12;

                y = line(cs, bold, 12, margin, y, "Settlement by Company");
                y = hr(cs, margin, y, page.getMediaBox().getWidth() - margin);
                for (OrderSplitDto split : order.splits()) {
                    y = line(cs, normal, 9, margin, y, String.format("%-40s %12s   [%s]",
                            clip(nullSafe(split.companyName()), 40), split.subtotal().toPlainString(),
                            split.paymentStatus()));
                }
                y -= 16;
                line(cs, normal, 8, margin, y,
                        "Merchants pay once via unified QR; each company receives its exact share.");
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate invoice: " + ex.getMessage());
        }
    }

    private static float line(PDPageContentStream cs, PDType1Font font, float size,
                              float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 6);
    }

    private static float hr(PDPageContentStream cs, float x1, float y, float x2) throws Exception {
        cs.moveTo(x1, y + 3);
        cs.lineTo(x2, y + 3);
        cs.stroke();
        return y - 4;
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    private static String nullSafe(String s) {
        return s == null ? "(unknown company)" : s;
    }
}
