package com.gestion.services;

import com.gestion.entities.RepasDetaille;
import com.gestion.entities.User;
import com.gestion.tools.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Générateur de facture PDF premium pour les commandes de repas.
 * Même design dark premium que les autres PDF LAMA EXPEDITION.
 */
public class CartInvoiceGenerator {

    /**
     * Génère une facture PDF pour une commande de repas.
     *
     * @param outputFile Fichier de sortie
     * @param items      Map repas -> quantité
     * @param subtotal   Sous-total avant réduction
     * @param discount   Montant de la réduction
     * @param total      Total TTC après réduction
     * @param promoCode  Code promo appliqué (null si aucun)
     * @param paymentRef Référence de paiement Stripe
     */
    public static void generate(File outputFile,
            Map<RepasDetaille, Integer> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String promoCode,
            String paymentRef) throws Exception {

        // Couleurs premium
        java.awt.Color cBgDark = new java.awt.Color(10, 15, 30);
        java.awt.Color cAccent = new java.awt.Color(56, 189, 147);
        java.awt.Color cAccent2 = new java.awt.Color(99, 102, 241);
        java.awt.Color cWhite = new java.awt.Color(255, 255, 255);
        java.awt.Color cLightGray = new java.awt.Color(200, 210, 225);
        java.awt.Color cMidGray = new java.awt.Color(148, 163, 184);
        java.awt.Color cCardBg = new java.awt.Color(22, 30, 55);
        java.awt.Color cCardAlt = new java.awt.Color(28, 38, 65);
        java.awt.Color cSep = new java.awt.Color(40, 55, 85);
        java.awt.Color cGreen = new java.awt.Color(34, 197, 94);

        // Fonts
        com.lowagie.text.Font fBrand = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fSub = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.NORMAL, cMidGray);
        com.lowagie.text.Font fSection = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Font fLabel = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,
                com.lowagie.text.Font.BOLD, cLightGray);
        com.lowagie.text.Font fValue = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fCell = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.NORMAL, cLightGray);
        com.lowagie.text.Font fPriceCell = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Font fTotalBig = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16,
                com.lowagie.text.Font.BOLD, cGreen);
        com.lowagie.text.Font fFooter = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7,
                com.lowagie.text.Font.ITALIC, cMidGray);
        com.lowagie.text.Font fPromo = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, new java.awt.Color(249, 115, 22));

        // Document
        com.lowagie.text.Document doc = new com.lowagie.text.Document(
                com.lowagie.text.PageSize.A4, 40, 40, 40, 40);
        com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(
                doc, new FileOutputStream(outputFile));
        doc.open();

        com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContent();
        com.lowagie.text.pdf.PdfContentByte under = writer.getDirectContentUnder();
        float pw = com.lowagie.text.PageSize.A4.getWidth();
        float ph = com.lowagie.text.PageSize.A4.getHeight();

        // 1. Fond sombre
        under.setColorFill(cBgDark);
        under.rectangle(0, 0, pw, ph);
        under.fill();

        // 2. Cadre décoratif
        canvas.setColorStroke(cAccent2);
        canvas.setLineWidth(1.5f);
        canvas.roundRectangle(20, 20, pw - 40, ph - 40, 8);
        canvas.stroke();
        canvas.setColorStroke(cAccent);
        canvas.setLineWidth(0.5f);
        canvas.roundRectangle(24, 24, pw - 48, ph - 48, 6);
        canvas.stroke();

        // 3. Bandes accent
        canvas.setColorFill(cAccent);
        canvas.rectangle(20, ph - 26, pw - 40, 6);
        canvas.fill();
        canvas.setColorFill(cAccent2);
        canvas.rectangle(20, 20, pw - 40, 6);
        canvas.fill();

        // 4. Logo
        try {
            InputStream logoStream = CartInvoiceGenerator.class.getResourceAsStream("/images/lamma-logo.png");
            if (logoStream != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = logoStream.read(tmp)) != -1)
                    buf.write(tmp, 0, n);
                buf.flush();
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(buf.toByteArray());
                logo.scaleToFit(60, 60);
                logo.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                doc.add(logo);
            }
        } catch (Exception ignored) {
        }

        // 5. En-tête
        com.lowagie.text.Paragraph brand = new com.lowagie.text.Paragraph("LAMA EXPEDITION", fBrand);
        brand.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(brand);

        doc.add(new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("FACTURE DE COMMANDE", fTitle);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(title);

        String invoiceId = "INV-" + System.currentTimeMillis() % 100000;
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph(
                "N° " + invoiceId + " — " + dateStr, fSub);
        sub.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(sub);

        doc.add(com.lowagie.text.Chunk.NEWLINE);

        // 6. Infos client
        User user = Session.getInstance().getCurrentUser();
        if (user != null) {
            com.lowagie.text.Paragraph clientTitle = new com.lowagie.text.Paragraph("▪  CLIENT", fSection);
            doc.add(clientTitle);

            com.lowagie.text.pdf.PdfPTable clientTable = new com.lowagie.text.pdf.PdfPTable(2);
            clientTable.setWidthPercentage(100);
            clientTable.setSpacingBefore(4f);

            String[][] clientInfo = {
                    { "NOM", user.getName() != null ? user.getName() : "-" },
                    { "EMAIL", user.getEmail() != null ? user.getEmail() : "-" }
            };
            for (String[] ci : clientInfo) {
                com.lowagie.text.pdf.PdfPCell lc = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Paragraph(ci[0], fLabel));
                lc.setBackgroundColor(cCardBg);
                lc.setBorderColor(cSep);
                lc.setBorderWidth(0.5f);
                lc.setPadding(8f);
                clientTable.addCell(lc);

                com.lowagie.text.pdf.PdfPCell vc = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Paragraph(ci[1], fValue));
                vc.setBackgroundColor(cCardBg);
                vc.setBorderColor(cSep);
                vc.setBorderWidth(0.5f);
                vc.setPadding(8f);
                clientTable.addCell(vc);
            }
            doc.add(clientTable);
            doc.add(com.lowagie.text.Chunk.NEWLINE);
        }

        // 7. Table des articles
        com.lowagie.text.Paragraph artTitle = new com.lowagie.text.Paragraph("▪  ARTICLES COMMANDÉS", fSection);
        doc.add(artTitle);
        doc.add(new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 40, 15, 20, 25 });

        String[] headers = { "Article", "Qté", "Prix unit.", "Total" };
        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell hc = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(h, fHeader));
            hc.setBackgroundColor(cAccent2);
            hc.setBorderColor(cSep);
            hc.setBorderWidth(0.5f);
            hc.setPadding(8);
            hc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(hc);
        }

        int row = 0;
        for (Map.Entry<RepasDetaille, Integer> entry : items.entrySet()) {
            RepasDetaille dish = entry.getKey();
            int qty = entry.getValue();
            BigDecimal lineTotal = dish.getPrix().multiply(BigDecimal.valueOf(qty));
            java.awt.Color rowBg = (row % 2 == 0) ? cCardBg : cCardAlt;

            com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(dish.getNom(), fCell));
            c1.setBackgroundColor(rowBg);
            c1.setBorderColor(cSep);
            c1.setBorderWidth(0.5f);
            c1.setPadding(7);
            table.addCell(c1);

            com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(String.valueOf(qty), fCell));
            c2.setBackgroundColor(rowBg);
            c2.setBorderColor(cSep);
            c2.setBorderWidth(0.5f);
            c2.setPadding(7);
            c2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(c2);

            com.lowagie.text.pdf.PdfPCell c3 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(String.format("%.2f €", dish.getPrix()), fPriceCell));
            c3.setBackgroundColor(rowBg);
            c3.setBorderColor(cSep);
            c3.setBorderWidth(0.5f);
            c3.setPadding(7);
            c3.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            table.addCell(c3);

            com.lowagie.text.pdf.PdfPCell c4 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(String.format("%.2f €", lineTotal), fPriceCell));
            c4.setBackgroundColor(rowBg);
            c4.setBorderColor(cSep);
            c4.setBorderWidth(0.5f);
            c4.setPadding(7);
            c4.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            table.addCell(c4);
            row++;
        }
        doc.add(table);
        doc.add(com.lowagie.text.Chunk.NEWLINE);

        // 8. Totaux
        com.lowagie.text.Paragraph totTitle = new com.lowagie.text.Paragraph("▪  RÉCAPITULATIF", fSection);
        doc.add(totTitle);
        doc.add(new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.pdf.PdfPTable totTable = new com.lowagie.text.pdf.PdfPTable(2);
        totTable.setWidthPercentage(60);
        totTable.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        totTable.setWidths(new float[] { 50, 50 });

        // Sous-total
        addTotalRow(totTable, "Sous-total", String.format("%.2f €", subtotal), fLabel, fValue, cCardBg, cSep);

        // Promo
        if (promoCode != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totTable, "Promo (" + promoCode + ")", "-" + String.format("%.2f €", discount),
                    fLabel, fPromo, cCardBg, cSep);
        }

        // Total
        addTotalRow(totTable, "TOTAL TTC", String.format("%.2f €", total), fLabel, fTotalBig, cCardAlt, cSep);

        doc.add(totTable);
        doc.add(com.lowagie.text.Chunk.NEWLINE);

        // 9. Référence paiement
        if (paymentRef != null) {
            com.lowagie.text.Paragraph refP = new com.lowagie.text.Paragraph(
                    "Réf. paiement : " + paymentRef, fSub);
            refP.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(refP);
            doc.add(com.lowagie.text.Chunk.NEWLINE);
        }

        // 10. Footer
        com.lowagie.text.Paragraph fb1 = new com.lowagie.text.Paragraph("LAMA EXPEDITION™", fBrand);
        fb1.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(fb1);

        com.lowagie.text.Paragraph fb2 = new com.lowagie.text.Paragraph(
                "Document confidentiel — " + dateStr + " — Merci pour votre commande !", fFooter);
        fb2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        doc.add(fb2);

        doc.close();
    }

    private static void addTotalRow(com.lowagie.text.pdf.PdfPTable table, String label, String value,
            com.lowagie.text.Font labelFont, com.lowagie.text.Font valueFont,
            java.awt.Color bg, java.awt.Color border) {
        com.lowagie.text.pdf.PdfPCell lc = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Paragraph(label, labelFont));
        lc.setBackgroundColor(bg);
        lc.setBorderColor(border);
        lc.setBorderWidth(0.5f);
        lc.setPadding(8f);
        lc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        table.addCell(lc);

        com.lowagie.text.pdf.PdfPCell vc = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Paragraph(value, valueFont));
        vc.setBackgroundColor(bg);
        vc.setBorderColor(border);
        vc.setBorderWidth(0.5f);
        vc.setPadding(8f);
        vc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        table.addCell(vc);
    }
}
