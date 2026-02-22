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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Génère un badge PDF premium pour une participation.
 * Contient : infos participation, nom utilisateur, nom événement,
 * code-barres Code128, QR code.
 */
public class TicketBadgeGenerator {

    // ─── Couleurs de la palette premium ───────────────────────────────────────
    private static final Color COLOR_BG_DARK = new Color(10, 15, 30);
    private static final Color COLOR_ACCENT = new Color(56, 189, 147); // vert nature
    private static final Color COLOR_ACCENT2 = new Color(99, 102, 241); // indigo
    private static final Color COLOR_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_LIGHT_GRAY = new Color(200, 210, 225);
    private static final Color COLOR_CARD_BG = new Color(22, 30, 55);
    private static final Color COLOR_SEPARATOR = new Color(40, 55, 85);

    // ─── Fonts ────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 28, Font.BOLD, COLOR_WHITE);
    private static final Font FONT_LABEL = new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_LIGHT_GRAY);
    private static final Font FONT_VALUE = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_WHITE);
    private static final Font FONT_CODE = new Font(Font.COURIER, 9, Font.NORMAL, COLOR_ACCENT);
    private static final Font FONT_FOOTER = new Font(Font.HELVETICA, 8, Font.ITALIC, COLOR_LIGHT_GRAY);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_ACCENT);

    /**
     * Génère un badge PDF complet.
     *
     * @param outputFile        fichier de destination
     * @param ticketCode        code unique du ticket (ex: TKT-1234567-42)
     * @param userName          nom complet de l'utilisateur
     * @param eventName         nom de l'événement
     * @param participationId   ID de la participation
     * @param eventDate         date de l'événement (peut être null)
     * @param lieu              lieu de l'événement
     * @param statut            statut de la participation (CONFIRME, EN_ATTENTE…)
     * @param typeParticipation type (SIMPLE, GROUPE, HEBERGEMENT)
     * @param nbParticipants    nombre de participants
     * @param montant           montant payé
     */
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

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            PdfContentByte canvas = writer.getDirectContent();

            // ── 1. Fond sombre pleine page ────────────────────────────────
            canvas.setColorFill(COLOR_BG_DARK);
            canvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
            canvas.fill();

            // ── 2. Bande d'accent en haut ────────────────────────────────
            canvas.setColorFill(COLOR_ACCENT);
            canvas.rectangle(0, PageSize.A4.getHeight() - 6, PageSize.A4.getWidth(), 6);
            canvas.fill();
            canvas.setColorFill(COLOR_ACCENT2);
            canvas.rectangle(0, PageSize.A4.getHeight() - 10, PageSize.A4.getWidth() * 0.6f, 4);
            canvas.fill();

            // ── 3. Bande d'accent en bas ─────────────────────────────────
            canvas.setColorFill(COLOR_ACCENT2);
            canvas.rectangle(0, 0, PageSize.A4.getWidth(), 6);
            canvas.fill();
            canvas.setColorFill(COLOR_ACCENT);
            canvas.rectangle(PageSize.A4.getWidth() * 0.4f, 0, PageSize.A4.getWidth() * 0.6f, 4);
            canvas.fill();

            // ── 4. En-tête ────────────────────────────────────────────────
            addHeader(doc, ticketCode, userName, eventName);

            // ── 5. Séparateur ─────────────────────────────────────────────
            addSeparator(doc, canvas);

            // ── 6. Grille d'infos ─────────────────────────────────────────
            addInfoGrid(doc, participationId, eventDate, lieu, statut,
                    typeParticipation, nbParticipants, montant);

            // ── 7. Section codes ─────────────────────────────────────────
            addCodesSection(doc, ticketCode, userName, eventName,
                    participationId, eventDate, lieu, statut, typeParticipation,
                    nbParticipants, montant);

            // ── 8. Pied de page ──────────────────────────────────────────
            addFooter(doc, ticketCode);

            doc.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE MISE EN PAGE
    // ─────────────────────────────────────────────────────────────────────────

    private static void addHeader(Document doc, String ticketCode,
            String userName, String eventName)
            throws DocumentException {

        doc.add(Chunk.NEWLINE);

        // Badge icon + titre principal
        Paragraph badge = new Paragraph("🎟  BADGE DE PARTICIPATION", FONT_TITLE);
        badge.setAlignment(Element.ALIGN_CENTER);
        doc.add(badge);

        doc.add(Chunk.NEWLINE);

        // Nom de l'événement — gros, accentué
        Paragraph evtLabel = new Paragraph("ÉVÉNEMENT", FONT_LABEL);
        evtLabel.setAlignment(Element.ALIGN_CENTER);
        doc.add(evtLabel);

        Font evtFont = new Font(Font.HELVETICA, 22, Font.BOLD, COLOR_ACCENT);
        Paragraph evtName = new Paragraph(eventName != null ? eventName.toUpperCase() : "N/A", evtFont);
        evtName.setAlignment(Element.ALIGN_CENTER);
        doc.add(evtName);

        doc.add(Chunk.NEWLINE);

        // Nom du participant
        Paragraph userLabel = new Paragraph("PARTICIPANT", FONT_LABEL);
        userLabel.setAlignment(Element.ALIGN_CENTER);
        doc.add(userLabel);

        Font userFont = new Font(Font.HELVETICA, 18, Font.BOLD, COLOR_WHITE);
        Paragraph userName2 = new Paragraph(userName != null ? userName : "Utilisateur inconnu", userFont);
        userName2.setAlignment(Element.ALIGN_CENTER);
        doc.add(userName2);

        doc.add(Chunk.NEWLINE);
    }

    private static void addSeparator(Document doc, PdfContentByte canvas)
            throws DocumentException {
        // Ligne de séparation stylisée via table
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_SEPARATOR);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(2f);
        sep.addCell(cell);
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

        Paragraph sectionTitle = new Paragraph("◼  DÉTAILS DE LA PARTICIPATION", FONT_SECTION);
        doc.add(sectionTitle);
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        addInfoCell(table, "N° PARTICIPATION", "#" + participationId);
        addInfoCell(table, "STATUT", statut != null ? statut : "N/A");
        addInfoCell(table, "TYPE", typeParticipation != null ? typeParticipation : "N/A");
        addInfoCell(table, "DATE ÉVÉNEMENT",
                eventDate != null ? eventDate.format(fmt) : "À définir");
        addInfoCell(table, "LIEU", lieu != null ? lieu : "Non précisé");
        addInfoCell(table, "PARTICIPANTS", String.valueOf(nbParticipants));
        addInfoCell(table, "MONTANT", montant != null ? montant : "Gratuit");
        addInfoCell(table, "GÉNÉRÉ LE",
                LocalDateTime.now().format(fmt));
        addInfoCell(table, "VALIDITÉ", "Journée de l'événement");

        doc.add(table);
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
    }

    private static void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_CARD_BG);
        cell.setBorderColor(COLOR_SEPARATOR);
        cell.setBorderWidth(1f);
        cell.setPadding(10f);

        Paragraph lbl = new Paragraph(label, FONT_LABEL);
        Paragraph val = new Paragraph(value, FONT_VALUE);
        cell.addElement(lbl);
        cell.addElement(val);
        table.addCell(cell);
    }

    private static void addCodesSection(Document doc, String ticketCode,
            String userName, String eventName,
            long participationId, LocalDateTime eventDate, String lieu,
            String statut, String typeParticipation,
            int nbParticipants, String montant)
            throws DocumentException, WriterException, IOException {

        Paragraph sectionTitle = new Paragraph("◼  CODES D'IDENTIFICATION", FONT_SECTION);
        doc.add(sectionTitle);
        doc.add(Chunk.NEWLINE);

        // Conteneur en 2 colonnes : barcode à gauche, QR à droite
        PdfPTable codesTable = new PdfPTable(2);
        codesTable.setWidthPercentage(100);
        codesTable.setWidths(new float[] { 55f, 45f });

        // ── Colonne gauche : Code-barres Code128 ─────────────────────────────
        PdfPCell barcodeCell = new PdfPCell();
        barcodeCell.setBackgroundColor(COLOR_CARD_BG);
        barcodeCell.setBorderColor(COLOR_SEPARATOR);
        barcodeCell.setPadding(15f);

        Paragraph bcTitle = new Paragraph("CODE-BARRES (Code128)", FONT_LABEL);
        barcodeCell.addElement(bcTitle);

        try {
            byte[] barcodeBytes = generateBarcode(ticketCode, 340, 80);
            Image barcodeImg = Image.getInstance(barcodeBytes);
            barcodeImg.scaleToFit(300, 70);
            barcodeImg.setAlignment(Image.ALIGN_CENTER);
            barcodeCell.addElement(barcodeImg);
        } catch (Exception e) {
            barcodeCell.addElement(new Paragraph("⚠ Barcode indisponible", FONT_FOOTER));
        }

        Paragraph codeText = new Paragraph(ticketCode, FONT_CODE);
        codeText.setAlignment(Element.ALIGN_CENTER);
        barcodeCell.addElement(codeText);

        codesTable.addCell(barcodeCell);

        // ── Colonne droite : QR Code ─────────────────────────────────────────
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBackgroundColor(COLOR_CARD_BG);
        qrCell.setBorderColor(COLOR_SEPARATOR);
        qrCell.setPadding(15f);

        Paragraph qrTitle = new Paragraph("QR CODE", FONT_LABEL);
        qrCell.addElement(qrTitle);

        // Construire le contenu encodé dans le QR
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
            qrCell.addElement(new Paragraph("⚠ QR indisponible", FONT_FOOTER));
        }

        Paragraph scanTip = new Paragraph("Scanner pour vérifier la validité", FONT_FOOTER);
        scanTip.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(scanTip);

        codesTable.addCell(qrCell);
        doc.add(codesTable);
        doc.add(Chunk.NEWLINE);
    }

    private static void addFooter(Document doc, String ticketCode)
            throws DocumentException {
        doc.add(Chunk.NEWLINE);

        // Ligne séparatrice fine
        PdfPTable sepFoot = new PdfPTable(1);
        sepFoot.setWidthPercentage(100);
        PdfPCell fc = new PdfPCell();
        fc.setBackgroundColor(COLOR_ACCENT2);
        fc.setBorder(Rectangle.NO_BORDER);
        fc.setFixedHeight(1.5f);
        sepFoot.addCell(fc);
        doc.add(sepFoot);

        doc.add(Chunk.NEWLINE);

        Paragraph footer1 = new Paragraph(
                "Ce badge est personnel et non transférable. Toute falsification est passible de poursuites.",
                FONT_FOOTER);
        footer1.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer1);

        Paragraph footer2 = new Paragraph(
                "En cas de problème, présentez ce document à l'accueil de l'événement. — Code: " + ticketCode,
                FONT_FOOTER);
        footer2.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer2);

        Paragraph footer3 = new Paragraph(
                "Généré par GestionAbonnements™ • " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                FONT_FOOTER);
        footer3.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRATION DES CODES (ZXing)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère un code-barres Code128 en PNG (retourné en byte[]).
     */
    private static byte[] generateBarcode(String content, int width, int height)
            throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Génère un QR code en PNG (retourné en byte[]).
     */
    private static byte[] generateQrCode(String content, int width, int height)
            throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Construit le contenu du QR code avec TOUTES les informations du ticket.
     * Quand scanné, l'utilisateur voit toutes les données lisiblement.
     */
    private static String buildQrContent(String ticketCode, String userName,
            String eventName, long participationId,
            LocalDateTime eventDate, String lieu, String statut,
            String typeParticipation, int nbParticipants, String montant) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = eventDate != null ? eventDate.format(fmt) : "N/A";
        String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        return "=== BADGE DE PARTICIPATION ==="
                + "\nCODE TICKET   : " + safe(ticketCode)
                + "\nPARTICIPATION : #" + participationId
                + "\n"
                + "\nPARTICIPANT   : " + safe(userName)
                + "\nÉVÉNEMENT      : " + safe(eventName)
                + "\nDATE          : " + dateStr
                + "\nLIEU          : " + safe(lieu)
                + "\n"
                + "\nTYPE          : " + safe(typeParticipation)
                + "\nSTATUT        : " + safe(statut)
                + "\nPARTICIPANTS  : " + nbParticipants
                + "\nMONTANT       : " + safe(montant)
                + "\n"
                + "\nGÉNÉRÉ LE      : " + genDate
                + "\n==============================";
    }

    private static String safe(String val) {
        return val != null && !val.isEmpty() ? val : "N/A";
    }
}
