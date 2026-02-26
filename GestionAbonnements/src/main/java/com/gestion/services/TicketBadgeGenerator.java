package com.gestion.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Génère un badge PDF premium pour une participation.
 * Design professionnel avec logo, filigrane, cadre décoratif et QR/barcode.
 */
public class TicketBadgeGenerator {

    // ─── Couleurs premium ────────────────────────────────────────────────────
    private static final Color COLOR_BG_DARK = new Color(10, 15, 30);
    private static final Color COLOR_ACCENT = new Color(56, 189, 147);
    private static final Color COLOR_ACCENT2 = new Color(99, 102, 241);
    private static final Color COLOR_GOLD = new Color(251, 191, 36);
    private static final Color COLOR_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_LIGHT_GRAY = new Color(200, 210, 225);
    private static final Color COLOR_MID_GRAY = new Color(148, 163, 184);
    private static final Color COLOR_CARD_BG = new Color(22, 30, 55);
    private static final Color COLOR_CARD_BG_ALT = new Color(28, 38, 65);
    private static final Color COLOR_SEPARATOR = new Color(40, 55, 85);
    private static final Color COLOR_WATERMARK = new Color(30, 40, 65);

    // ─── Fonts ───────────────────────────────────────────────────────────────
    private static final Font FONT_BRAND = new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_ACCENT);
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 26, Font.BOLD, COLOR_WHITE);
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_MID_GRAY);
    private static final Font FONT_LABEL = new Font(Font.HELVETICA, 8, Font.BOLD, COLOR_LIGHT_GRAY);
    private static final Font FONT_VALUE = new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_WHITE);
    private static final Font FONT_VALUE_LG = new Font(Font.HELVETICA, 20, Font.BOLD, COLOR_ACCENT);
    private static final Font FONT_CODE = new Font(Font.COURIER, 9, Font.NORMAL, COLOR_ACCENT);
    private static final Font FONT_FOOTER = new Font(Font.HELVETICA, 7, Font.ITALIC, COLOR_MID_GRAY);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_ACCENT);
    private static final Font FONT_BADGE_TAG = new Font(Font.HELVETICA, 8, Font.BOLD, COLOR_WHITE);

    public static void generateBadge(
            File outputFile,
            String ticketCode,
            String userName,
            String eventName,
            long participationId,
            LocalDateTime eventDate,
            String lieu,
            String statut,
            String typeParticipation,
            int nbParticipants,
            String montant) throws IOException, DocumentException, WriterException {

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            PdfContentByte canvas = writer.getDirectContent();
            PdfContentByte under = writer.getDirectContentUnder();
            float pw = PageSize.A4.getWidth();
            float ph = PageSize.A4.getHeight();

            // ── 1. Fond sombre ──────────────────────────────────────────
            under.setColorFill(COLOR_BG_DARK);
            under.rectangle(0, 0, pw, ph);
            under.fill();

            // ── 2. Filigrane diagonal "LAMA EXPEDITION" ─────────────────
            under.saveState();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.04f);
            under.setGState(gs);
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            under.setColorFill(COLOR_WHITE);
            under.beginText();
            under.setFontAndSize(bf, 72);
            under.showTextAligned(Element.ALIGN_CENTER,
                    "LAMA EXPEDITION", pw / 2, ph / 2, 35);
            under.endText();
            under.restoreState();

            // ── 3. Cadre décoratif ──────────────────────────────────────
            canvas.setColorStroke(COLOR_ACCENT2);
            canvas.setLineWidth(1.5f);
            canvas.roundRectangle(20, 20, pw - 40, ph - 40, 8);
            canvas.stroke();

            // coin intérieur accent
            canvas.setColorStroke(COLOR_ACCENT);
            canvas.setLineWidth(0.5f);
            canvas.roundRectangle(24, 24, pw - 48, ph - 48, 6);
            canvas.stroke();

            // ── 4. Bandes d'accent en haut ──────────────────────────────
            canvas.setColorFill(COLOR_ACCENT);
            canvas.rectangle(20, ph - 26, pw - 40, 6);
            canvas.fill();
            canvas.setColorFill(COLOR_ACCENT2);
            canvas.rectangle(20, ph - 30, (pw - 40) * 0.6f, 4);
            canvas.fill();

            // ── 5. Bandes d'accent en bas ───────────────────────────────
            canvas.setColorFill(COLOR_ACCENT2);
            canvas.rectangle(20, 20, pw - 40, 6);
            canvas.fill();
            canvas.setColorFill(COLOR_ACCENT);
            canvas.rectangle(20 + (pw - 40) * 0.4f, 20, (pw - 40) * 0.6f, 4);
            canvas.fill();

            // ── 6. Logo ─────────────────────────────────────────────────
            addLogo(doc);

            // ── 7. En-tête ──────────────────────────────────────────────
            addHeader(doc, ticketCode, userName, eventName, statut);

            // ── 8. Séparateur ───────────────────────────────────────────
            addSeparator(doc);

            // ── 9. Grille d'infos ───────────────────────────────────────
            addInfoGrid(doc, participationId, eventDate, lieu, statut,
                    typeParticipation, nbParticipants, montant);

            // ── 10. Section codes ───────────────────────────────────────
            addCodesSection(doc, ticketCode, userName, eventName,
                    participationId, eventDate, lieu, statut, typeParticipation,
                    nbParticipants, montant);

            // ── 11. Pied de page ────────────────────────────────────────
            addFooter(doc, ticketCode);

            doc.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static void addLogo(Document doc) throws DocumentException {
        try {
            InputStream logoStream = TicketBadgeGenerator.class
                    .getResourceAsStream("/images/lamma-logo.png");
            if (logoStream != null) {
                byte[] logoBytes = readAllBytes(logoStream);
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Image.ALIGN_CENTER);
                doc.add(logo);
                doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));
            }
        } catch (Exception e) {
            // Logo not found — continue without it
        }
    }

    private static void addHeader(Document doc, String ticketCode,
            String userName, String eventName, String statut)
            throws DocumentException {

        // Brand name
        Paragraph brand = new Paragraph("LAMA EXPEDITION", FONT_BRAND);
        brand.setAlignment(Element.ALIGN_CENTER);
        doc.add(brand);

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        // Title
        Paragraph badge = new Paragraph("BADGE DE PARTICIPATION", FONT_TITLE);
        badge.setAlignment(Element.ALIGN_CENTER);
        doc.add(badge);

        // Subtitle
        Paragraph sub = new Paragraph("Document officiel — Accès événement", FONT_SUBTITLE);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);

        doc.add(Chunk.NEWLINE);

        // Status badge
        Color statusColor = getStatusColor(statut);
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(30);
        PdfPCell statusCell = new PdfPCell(new Paragraph(
                safe(statut).toUpperCase(), FONT_BADGE_TAG));
        statusCell.setBackgroundColor(statusColor);
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setPadding(6f);
        statusTable.addCell(statusCell);
        doc.add(statusTable);

        doc.add(Chunk.NEWLINE);

        // Event name
        Paragraph evtLabel = new Paragraph("ÉVÉNEMENT", FONT_LABEL);
        evtLabel.setAlignment(Element.ALIGN_CENTER);
        doc.add(evtLabel);

        Paragraph evtName = new Paragraph(
                eventName != null ? eventName.toUpperCase() : "N/A", FONT_VALUE_LG);
        evtName.setAlignment(Element.ALIGN_CENTER);
        doc.add(evtName);

        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 6)));

        // Participant name
        Paragraph userLabel = new Paragraph("PARTICIPANT", FONT_LABEL);
        userLabel.setAlignment(Element.ALIGN_CENTER);
        doc.add(userLabel);

        Font userFont = new Font(Font.HELVETICA, 18, Font.BOLD, COLOR_WHITE);
        Paragraph userName2 = new Paragraph(
                userName != null ? userName : "Utilisateur inconnu", userFont);
        userName2.setAlignment(Element.ALIGN_CENTER);
        doc.add(userName2);

        doc.add(Chunk.NEWLINE);
    }

    private static Color getStatusColor(String statut) {
        if (statut == null)
            return COLOR_ACCENT2;
        String s = statut.toUpperCase();
        if (s.contains("CONFIRM"))
            return new Color(34, 150, 100);
        if (s.contains("ATTENTE"))
            return new Color(217, 119, 6);
        if (s.contains("ANNUL"))
            return new Color(220, 38, 38);
        return COLOR_ACCENT2;
    }

    private static void addSeparator(Document doc) throws DocumentException {
        PdfPTable sep = new PdfPTable(3);
        sep.setWidthPercentage(80);
        try {
            sep.setWidths(new float[] { 45, 10, 45 });
        } catch (DocumentException ignored) {
        }

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(COLOR_ACCENT);
        left.setBorder(Rectangle.NO_BORDER);
        left.setFixedHeight(2f);
        sep.addCell(left);

        PdfPCell mid = new PdfPCell(new Paragraph("◆",
                new Font(Font.HELVETICA, 6, Font.BOLD, COLOR_GOLD)));
        mid.setBorder(Rectangle.NO_BORDER);
        mid.setHorizontalAlignment(Element.ALIGN_CENTER);
        mid.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sep.addCell(mid);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(COLOR_ACCENT2);
        right.setBorder(Rectangle.NO_BORDER);
        right.setFixedHeight(2f);
        sep.addCell(right);

        doc.add(sep);
        doc.add(Chunk.NEWLINE);
    }

    private static void addInfoGrid(Document doc,
            long participationId,
            LocalDateTime eventDate,
            String lieu, String statut,
            String typeParticipation,
            int nbParticipants,
            String montant) throws DocumentException {

        Paragraph sectionTitle = new Paragraph(
                "▪  DÉTAILS DE LA PARTICIPATION", FONT_SECTION);
        doc.add(sectionTitle);
        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        addInfoCell(table, "N° PARTICIPATION", "#" + participationId, false);
        addInfoCell(table, "STATUT", safe(statut), true);
        addInfoCell(table, "TYPE", safe(typeParticipation), false);
        addInfoCell(table, "DATE ÉVÉNEMENT",
                eventDate != null ? eventDate.format(fmt) : "À définir", true);
        addInfoCell(table, "LIEU", safe(lieu), false);
        addInfoCell(table, "PARTICIPANTS", String.valueOf(nbParticipants), true);
        addInfoCell(table, "MONTANT", safe(montant), false);
        addInfoCell(table, "GÉNÉRÉ LE",
                LocalDateTime.now().format(fmt), true);
        addInfoCell(table, "VALIDITÉ", "Journée de l'événement", false);

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private static void addInfoCell(PdfPTable table, String label,
            String value, boolean alt) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(alt ? COLOR_CARD_BG_ALT : COLOR_CARD_BG);
        cell.setBorderColor(COLOR_SEPARATOR);
        cell.setBorderWidth(1f);
        cell.setPadding(10f);

        cell.addElement(new Paragraph(label, FONT_LABEL));
        cell.addElement(new Paragraph(value, FONT_VALUE));
        table.addCell(cell);
    }

    private static void addCodesSection(Document doc, String ticketCode,
            String userName, String eventName,
            long participationId, LocalDateTime eventDate, String lieu,
            String statut, String typeParticipation,
            int nbParticipants, String montant)
            throws DocumentException, WriterException, IOException {

        Paragraph sectionTitle = new Paragraph(
                "▪  CODES D'IDENTIFICATION", FONT_SECTION);
        doc.add(sectionTitle);
        doc.add(new Paragraph(" ", new Font(Font.HELVETICA, 4)));

        PdfPTable codesTable = new PdfPTable(2);
        codesTable.setWidthPercentage(100);
        codesTable.setWidths(new float[] { 55f, 45f });

        // Barcode cell
        PdfPCell barcodeCell = new PdfPCell();
        barcodeCell.setBackgroundColor(COLOR_CARD_BG);
        barcodeCell.setBorderColor(COLOR_SEPARATOR);
        barcodeCell.setPadding(15f);

        barcodeCell.addElement(new Paragraph("CODE-BARRES (Code128)", FONT_LABEL));
        try {
            byte[] barcodeBytes = generateBarcode(ticketCode, 340, 80);
            Image barcodeImg = Image.getInstance(barcodeBytes);
            barcodeImg.scaleToFit(300, 70);
            barcodeImg.setAlignment(Image.ALIGN_CENTER);
            barcodeCell.addElement(barcodeImg);
        } catch (Exception e) {
            barcodeCell.addElement(new Paragraph("Barcode indisponible", FONT_FOOTER));
        }
        Paragraph codeText = new Paragraph(ticketCode, FONT_CODE);
        codeText.setAlignment(Element.ALIGN_CENTER);
        barcodeCell.addElement(codeText);
        codesTable.addCell(barcodeCell);

        // QR cell
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBackgroundColor(COLOR_CARD_BG);
        qrCell.setBorderColor(COLOR_SEPARATOR);
        qrCell.setPadding(15f);

        qrCell.addElement(new Paragraph("QR CODE", FONT_LABEL));
        String qrContent = buildQrContent(ticketCode, userName, eventName,
                participationId, eventDate, lieu, statut, typeParticipation,
                nbParticipants, montant);
        try {
            byte[] qrBytes = generateQrCode(qrContent, 180, 180);
            Image qrImg = Image.getInstance(qrBytes);
            qrImg.scaleToFit(150, 150);
            qrImg.setAlignment(Image.ALIGN_CENTER);
            qrCell.addElement(qrImg);
        } catch (Exception e) {
            qrCell.addElement(new Paragraph("QR indisponible", FONT_FOOTER));
        }
        Paragraph scanTip = new Paragraph(
                "Scanner pour vérifier la validité", FONT_FOOTER);
        scanTip.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(scanTip);
        codesTable.addCell(qrCell);

        doc.add(codesTable);
        doc.add(Chunk.NEWLINE);
    }

    private static void addFooter(Document doc, String ticketCode)
            throws DocumentException {

        // Separator
        addSeparator(doc);

        // Footer lines
        Font footBold = new Font(Font.HELVETICA, 8, Font.BOLD, COLOR_ACCENT);
        Paragraph brand = new Paragraph("LAMA EXPEDITION™", footBold);
        brand.setAlignment(Element.ALIGN_CENTER);
        doc.add(brand);

        Paragraph footer1 = new Paragraph(
                "Ce badge est personnel et non transférable. "
                        + "Toute falsification est passible de poursuites.",
                FONT_FOOTER);
        footer1.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer1);

        Paragraph footer2 = new Paragraph(
                "Présentez ce document à l'accueil de l'événement. — Code: "
                        + ticketCode,
                FONT_FOOTER);
        footer2.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer2);

        Paragraph footer3 = new Paragraph(
                "Généré par LAMA EXPEDITION™ le "
                        + LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                FONT_FOOTER);
        footer3.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CODES ZXing
    // ─────────────────────────────────────────────────────────────────────────

    private static byte[] generateBarcode(String content, int w, int h)
            throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix m = new MultiFormatWriter()
                .encode(content, BarcodeFormat.CODE_128, w, h, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(m, "PNG", out);
        return out.toByteArray();
    }

    private static byte[] generateQrCode(String content, int w, int h)
            throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix m = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, w, h, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(m, "PNG", out);
        return out.toByteArray();
    }

    private static String buildQrContent(String ticketCode, String userName,
            String eventName, long participationId,
            LocalDateTime eventDate, String lieu, String statut,
            String typeParticipation, int nbParticipants, String montant) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = eventDate != null ? eventDate.format(fmt) : "N/A";
        String genDate = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        return "=== LAMA EXPEDITION ==="
                + "\n=== BADGE DE PARTICIPATION ==="
                + "\nCODE TICKET   : " + safe(ticketCode)
                + "\nPARTICIPATION : #" + participationId
                + "\n"
                + "\nPARTICIPANT   : " + safe(userName)
                + "\nÉVÉNEMENT     : " + safe(eventName)
                + "\nDATE          : " + dateStr
                + "\nLIEU          : " + safe(lieu)
                + "\n"
                + "\nTYPE          : " + safe(typeParticipation)
                + "\nSTATUT        : " + safe(statut)
                + "\nPARTICIPANTS  : " + nbParticipants
                + "\nMONTANT       : " + safe(montant)
                + "\n"
                + "\nGÉNÉRÉ LE    : " + genDate
                + "\n============================";
    }

    private static String safe(String val) {
        return val != null && !val.isEmpty() ? val : "N/A";
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
