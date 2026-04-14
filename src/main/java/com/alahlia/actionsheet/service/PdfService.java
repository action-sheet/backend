package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.ActionSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PDF generation service using Apache PDFBox + Java2D ActionSheetRenderer.
 * Migrated from legacy PDFGenerator.java + ActionSheetRenderer.java.
 * Renders a pixel-perfect A4 action sheet document matching the legacy Swing form.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    @Value("${app.files-path}")
    private String filesPath;

    @Value("${app.data-path:./data}")
    private String dataPath;

    // A4 Dimensions at 72 DPI
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    // Margins
    private static final int MARGIN_LEFT = 40;
    private static final int MARGIN_RIGHT = 40;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Generate PDF for action sheet using the legacy ActionSheetRenderer layout.
     * Renders to a BufferedImage at 2x scale (144 DPI) then embeds as JPEG in PDF.
     * If attachments exist, merges them as additional pages.
     */
    public String generatePdf(ActionSheet sheet) {
        try (PDDocument document = new PDDocument()) {
            // Page 1: Generated action sheet
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Extract form data
            Map<String, Object> formData = sheet.getFormData() != null ? sheet.getFormData() : Map.of();

            // Render to BufferedImage at 1.25x scale (90 DPI - good quality, smaller file)
            double scale = 1.25;
            int width = (int)(PAGE_WIDTH * scale);
            int height = (int)(PAGE_HEIGHT * scale);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();

            g2.scale(scale, scale);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Fill background white
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

            // Draw the action sheet document
            drawDocument(g2, formData, sheet);
            g2.dispose();

            // Embed in PDF with optimized JPEG compression (0.65 = smaller file, still readable)
            PDImageXObject pdImage = JPEGFactory.createFromImage(document, image, 0.65f);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0,
                        page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }

            // Pages 2+: Merge attached documents
            List<PDDocument> attachedDocs = new ArrayList<>();
            try {
                if (sheet.getAttachments() != null && !sheet.getAttachments().isEmpty()) {
                    for (String fileName : sheet.getAttachments()) {
                        File attachedDoc = new File("data/attachments/" + sheet.getId() + "/" + fileName);
                        if (attachedDoc.exists() && fileName.toLowerCase().endsWith(".pdf")) {
                            try {
                                // Load the attached PDF and keep it open until we're done
                                PDDocument attachedPdf = PDDocument.load(attachedDoc);
                                attachedDocs.add(attachedPdf); // Keep reference to close later
                                
                                // Import pages (limit to first 20 pages to control file size)
                                int pageCount = 0;
                                for (PDPage attachedPage : attachedPdf.getPages()) {
                                    if (pageCount >= 20) {
                                        log.warn("Attachment {} exceeds 20 pages, truncating", fileName);
                                        break;
                                    }
                                    document.importPage(attachedPage);
                                    pageCount++;
                                }
                                log.debug("Merged PDF attachment: {} ({} pages)", fileName, pageCount);
                            } catch (Exception e) {
                                log.warn("Failed to merge PDF attachment {}: {}", fileName, e.getMessage());
                            }
                        }
                    }
                }

                // Save PDF with compression (while attached docs are still open)
                String pdfPath = saveDocument(document, sheet);
                log.info("✅ PDF generated: {} (size: {} KB)", pdfPath, new File(pdfPath).length() / 1024);
                return pdfPath;
                
            } finally {
                // Close all attached documents after saving
                for (PDDocument attachedDoc : attachedDocs) {
                    try {
                        attachedDoc.close();
                    } catch (Exception e) {
                        log.debug("Error closing attached PDF: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ PDF generation failed for sheet {}: {}", sheet.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Core rendering method — pixel-perfect reproduces the legacy ActionSheetRenderer.
     */
    private void drawDocument(Graphics2D g2, Map<String, Object> data, ActionSheet sheet) {
        // --- Fonts ---
        Font fontRegular = new Font("SansSerif", Font.PLAIN, 10);
        Font fontBold = new Font("SansSerif", Font.BOLD, 10);
        Font fontHeader = new Font("SansSerif", Font.ITALIC, 9);
        Font fontDoctype = new Font("SansSerif", Font.BOLD | Font.ITALIC, 14);
        Font fontArabic = new Font("Dialog", Font.BOLD, 11);
        Font fontHandwriting = new Font("SansSerif", Font.ITALIC, 14);

        g2.setColor(Color.BLACK);

        // --- 1. HEADER ---
        g2.setFont(fontHeader);
        g2.drawString("AL-AHLIA CONTRACTING GROUP (W.L.L.)", 40, 50);

        g2.setFont(fontArabic);
        String arabicName = "\u0627\u0644\u0645\u062C\u0645\u0648\u0639\u0629 \u0627\u0644\u0623\u0647\u0644\u064A\u0629 \u0644\u0644\u0645\u0642\u0627\u0648\u0644\u0627\u062A";
        FontMetrics fmArabic = g2.getFontMetrics();
        g2.drawString(arabicName, PAGE_WIDTH - 40 - fmArabic.stringWidth(arabicName), 50);

        // Center: Logo
        int logoX = (PAGE_WIDTH - 65) / 2;
        int logoY = 30;
        int logoW = 65;
        int logoH = 45;

        try {
            ClassPathResource logoResource = new ClassPathResource("static/acg_logo.jpg");
            if (logoResource.exists()) {
                try (InputStream is = logoResource.getInputStream()) {
                    BufferedImage logo = ImageIO.read(is);
                    g2.drawImage(logo, logoX, logoY, logoW, logoH, null);
                    g2.setStroke(new BasicStroke(0.5f));
                    g2.drawRect(logoX, logoY, logoW, logoH);
                }
            } else {
                g2.drawRect(logoX, logoY, 60, 40);
                g2.drawString("ACG", logoX + 15, logoY + 25);
            }
        } catch (IOException e) {
            log.warn("Failed to load logo image: {}", e.getMessage());
            g2.drawRect(logoX, logoY, 60, 40);
            g2.drawString("ACG", logoX + 15, logoY + 25);
        }

        // --- 2. TITLE BOX ---
        int titleY = 90;
        int titleW = 140;
        int titleH = 20;
        int titleX = (PAGE_WIDTH - titleW) / 2;
        g2.drawRect(titleX, titleY, titleW, titleH);

        g2.setFont(fontDoctype);
        String title = "ACTION SHEET";
        FontMetrics fmTitle = g2.getFontMetrics();
        g2.drawString(title, titleX + (titleW - fmTitle.stringWidth(title)) / 2, titleY + 15);

        // --- 3. TOP TYPE GRID ---
        int typeY = 125;
        int[] colWidths = {50, 20, 35, 20, 35, 20, 45, 20};
        String[] headers = {"LETTER", "", "FAX", "", "COPY", "", "E.MAIL", ""};

        int gridX = 140;
        int gridH = 18;

        g2.setStroke(new BasicStroke(1.0f));
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));

        int currentX = gridX;
        for (int i = 0; i < colWidths.length; i++) {
            g2.drawRect(currentX, typeY, colWidths[i], gridH);

            String text = headers[i];
            if (!text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, currentX + (colWidths[i] - fm.stringWidth(text)) / 2, typeY + 13);
            } else {
                boolean isChecked = false;
                if (i == 1) isChecked = getBool(data, "isLetter");
                else if (i == 3) isChecked = getBool(data, "isFax");
                else if (i == 5) isChecked = getBool(data, "isCopy");
                else if (i == 7) isChecked = getBool(data, "isEmail");

                if (isChecked) {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    g2.drawString("\u2713", currentX + 5, typeY + 14);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                }
            }
            currentX += colWidths[i];
        }

        // --- 4. INFO ROWS (Dynamic heights) ---
        int currentY = 160;
        int minRowH = 25;
        int lineHeight = 16;

        // ROW 1: Original To | Date Received
        String otText = getStr(data, "originalTo", "");
        String drText = getStr(data, "dateReceived", "");
        int row1H = minRowH;

        g2.drawRect(MARGIN_LEFT, currentY, CONTENT_WIDTH, row1H);
        g2.drawLine(MARGIN_LEFT + 80, currentY, MARGIN_LEFT + 80, currentY + row1H);
        g2.drawLine(320, currentY, 320, currentY + row1H);

        g2.setFont(fontRegular);
        g2.drawString("Original To", MARGIN_LEFT + 5, currentY + 16);
        g2.setFont(fontBold);
        g2.drawString(otText, MARGIN_LEFT + 90, currentY + 16);

        g2.setFont(fontRegular);
        g2.drawString("Date Received :", 325, currentY + 16);
        g2.setFont(fontBold);
        g2.drawString(drText, 415, currentY + 16);

        g2.setFont(fontArabic);
        String arOT = "\u0627\u0644\u0627\u0635\u0644 \u0625\u0644\u0649 :";
        g2.drawString(arOT, 315 - g2.getFontMetrics().stringWidth(arOT), currentY + 16);
        String arDR = "\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0627\u0633\u062A\u0644\u0627\u0645 :";
        g2.drawString(arDR, PAGE_WIDTH - MARGIN_RIGHT - 5 - g2.getFontMetrics().stringWidth(arDR), currentY + 16);

        currentY += row1H;

        // ROW 2: Ref No | Document Date
        String rnText = getStr(data, "refNo", "");
        String ddText = getStr(data, "documentDate", "");

        g2.drawRect(MARGIN_LEFT, currentY, CONTENT_WIDTH, minRowH);
        g2.drawLine(MARGIN_LEFT + 80, currentY, MARGIN_LEFT + 80, currentY + minRowH);
        g2.drawLine(320, currentY, 320, currentY + minRowH);

        g2.setFont(fontRegular);
        g2.drawString("Ref. No.", MARGIN_LEFT + 5, currentY + 16);
        g2.setFont(fontBold);
        g2.drawString(rnText, MARGIN_LEFT + 90, currentY + 16);

        g2.setFont(fontRegular);
        g2.drawString("Document Date :", 325, currentY + 16);
        g2.setFont(fontBold);
        g2.drawString(ddText, 415, currentY + 16);

        g2.setFont(fontArabic);
        String arRN = "\u0631\u0642\u0645 \u0627\u0644\u0643\u062A\u0627\u0628 :";
        g2.drawString(arRN, 315 - g2.getFontMetrics().stringWidth(arRN), currentY + 16);
        String arDD = "\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0643\u062A\u0627\u0628 :";
        g2.drawString(arDD, PAGE_WIDTH - MARGIN_RIGHT - 5 - g2.getFontMetrics().stringWidth(arDD), currentY + 16);

        currentY += minRowH;

        // ROW 3: From (Full Width)
        String fromText = getStr(data, "from", "");
        g2.drawRect(MARGIN_LEFT, currentY, CONTENT_WIDTH, minRowH);

        g2.setFont(fontRegular);
        g2.drawString("From", MARGIN_LEFT + 5, currentY + 16);
        g2.drawString(":", MARGIN_LEFT + 80, currentY + 16);
        g2.setFont(fontBold);
        g2.drawString(fromText, MARGIN_LEFT + 90, currentY + 16);

        g2.setFont(fontArabic);
        String arFrom = "\u0645\u0646 :";
        g2.drawString(arFrom, PAGE_WIDTH - MARGIN_RIGHT - 20 - g2.getFontMetrics().stringWidth(arFrom), currentY + 16);

        currentY += minRowH;

        // ROW 4: Subject (80px height)
        int subjectH = 80;
        g2.drawRect(MARGIN_LEFT, currentY, CONTENT_WIDTH, subjectH);

        g2.setFont(fontRegular);
        g2.drawString("Subject", MARGIN_LEFT + 5, currentY + 16);
        g2.drawString(":", MARGIN_LEFT + 80, currentY + 16);

        int lineStart = 120;
        int lineEnd = PAGE_WIDTH - MARGIN_RIGHT - 40;
        g2.drawLine(lineStart, currentY + 18, lineEnd, currentY + 18);
        g2.drawLine(lineStart, currentY + 43, lineEnd, currentY + 43);
        g2.drawLine(lineStart, currentY + 68, lineEnd, currentY + 68);

        String subjectText = getStr(data, "subject", sheet.getTitle());
        g2.setFont(fontBold);
        drawWrappedText(g2, subjectText, lineStart + 10, lineStart + 10,
                currentY + 15, lineEnd - lineStart - 10, lineEnd - lineStart - 10, 25, 3);

        g2.setFont(fontArabic);
        String arSubject = "\u0627\u0644\u0645\u0648\u0636\u0648\u0639 :";
        g2.drawString(arSubject, PAGE_WIDTH - MARGIN_RIGHT - 5 - g2.getFontMetrics().stringWidth(arSubject), currentY + 16);

        currentY += subjectH;

        // --- 5. DISTRIBUTION GRID ---
        int gridRowH = 22;
        int numGridRows = 3;

        int[] gridCols = {80, 32, 32, 32, 32, 42, 32, 32, 32, 32, 32, 40, 50};
        String[] gridHeaders = {"", "G.M.", "D. GM", "EX. M's", "ACC.", "PM / PE", "M.C.", "L.A.", "O.M.", "E/M", "PL. E", "Others", ""};

        g2.drawRect(MARGIN_LEFT, currentY, CONTENT_WIDTH, gridRowH * numGridRows);
        g2.drawLine(MARGIN_LEFT, currentY + gridRowH, PAGE_WIDTH - MARGIN_RIGHT, currentY + gridRowH);
        g2.drawLine(MARGIN_LEFT + 90, currentY + gridRowH * 2, PAGE_WIDTH - MARGIN_RIGHT - 55, currentY + gridRowH * 2);

        int gx = MARGIN_LEFT;
        for (int i = 0; i < gridCols.length; i++) {
            gx += gridCols[i];
            if (i < gridCols.length - 1) {
                g2.drawLine(gx, currentY, gx, currentY + gridRowH * numGridRows);
            }

            String h = gridHeaders[i];
            if (!h.isEmpty()) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(h, gx - gridCols[i] + (gridCols[i] - fm.stringWidth(h)) / 2, currentY + 15);
            }
        }

        g2.setFont(fontRegular);
        g2.drawString("For Action", MARGIN_LEFT + 5, currentY + gridRowH + 15);
        g2.drawString("Copy to", MARGIN_LEFT + 5, currentY + gridRowH * 2 + 15);

        g2.setFont(fontArabic);
        int lastColX = PAGE_WIDTH - MARGIN_RIGHT - 50;
        String arCopyTo = "\u0635\u0648\u0631\u0629 \u0625\u0644\u0649";
        FontMetrics fmAr = g2.getFontMetrics();
        g2.drawString(arCopyTo, lastColX + (50 - fmAr.stringWidth(arCopyTo)) / 2, currentY + gridRowH + 15);

        // Tick marks
        for (int i = 1; i <= 11; i++) {
            int dataIdx = i - 1;
            int cellW = gridCols[i];
            int px = MARGIN_LEFT;
            for (int k = 0; k < i; k++) px += gridCols[k];
            int tickX = px + cellW / 2 - 5;

            if (getBool(data, "forAction_" + dataIdx)) {
                g2.drawString("\u2713", tickX, currentY + gridRowH + 16);
            }
            if (getBool(data, "sendCopy_" + dataIdx)) {
                g2.drawString("\u2713", tickX, currentY + gridRowH * 2 + 16);
            }
        }

        // --- 6. RESPONSE SECTION ---
        int responseY = currentY + gridRowH * 3 + 10;

        g2.setFont(fontRegular);
        g2.drawString("Response :", MARGIN_LEFT + 5, responseY + 12);
        g2.setFont(fontArabic);
        String respArabic = "\u0627\u0644\u0631\u062F :";
        g2.drawString(respArabic, PAGE_WIDTH - MARGIN_RIGHT - 30 - g2.getFontMetrics().stringWidth(respArabic), responseY + 12);

        int lineSpace = 25;
        int startLineY = responseY + 15;
        int numLines = 10;

        for (int k = 0; k < numLines; k++) {
            int ly = startLineY + k * lineSpace;
            g2.drawLine(MARGIN_LEFT + 80, ly, PAGE_WIDTH - MARGIN_RIGHT, ly);
            if (k > 0) {
                g2.drawLine(MARGIN_LEFT, ly, PAGE_WIDTH - MARGIN_RIGHT, ly);
            }
        }

        // Response text
        String responseText = getStr(data, "response", "");
        g2.setColor(Color.BLACK);
        g2.setFont(fontHandwriting);
        drawWrappedText(g2, responseText, MARGIN_LEFT + 150, MARGIN_LEFT + 5, startLineY + 20,
                PAGE_WIDTH - MARGIN_RIGHT - (MARGIN_LEFT + 150),
                PAGE_WIDTH - MARGIN_RIGHT - (MARGIN_LEFT + 5),
                lineSpace, numLines);
    }

    // ═══ Save helpers ═══

    private String saveDocument(PDDocument document, ActionSheet sheet) throws Exception {
        // Try project-specific folder first
        File saveDir = null;

        if (sheet.getProjectId() != null && !sheet.getProjectId().isEmpty()) {
            // Look for ActionSheets subfolder within project
            String projectPath = dataPath + File.separator + "projects";
            File projectsDir = new File(projectPath);
            if (projectsDir.exists()) {
                File[] dirs = projectsDir.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File dir : dirs) {
                        File actionSheetsDir = new File(dir, "ActionSheets");
                        if (actionSheetsDir.exists()) {
                            saveDir = actionSheetsDir;
                            break;
                        }
                    }
                }
            }
        }

        if (saveDir == null) {
            saveDir = new File(filesPath);
        }
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        String fileName = "ActionSheet_" + sheet.getId() + ".pdf";
        File pdfFile = new File(saveDir, fileName);

        // Check if file is locked
        if (pdfFile.exists() && !isFileWritable(pdfFile)) {
            String timestamp = new java.text.SimpleDateFormat("HHmmss").format(new java.util.Date());
            fileName = "ActionSheet_" + sheet.getId() + "_" + timestamp + ".pdf";
            pdfFile = new File(saveDir, fileName);
        }

        document.save(pdfFile);
        return pdfFile.getAbsolutePath();
    }

    private boolean isFileWritable(File file) {
        if (!file.exists()) return true;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw");
             java.nio.channels.FileChannel channel = raf.getChannel()) {
            java.nio.channels.FileLock lock = channel.tryLock();
            if (lock != null) {
                lock.release();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══ Text drawing helpers (from legacy ActionSheetRenderer) ═══

    private void drawWrappedText(Graphics2D g2, String text, int startX, int subsequentX, int startY,
                                  int firstLineWidth, int subsequentLineWidth, int lineSpacing, int maxLines) {
        if (text == null || text.isEmpty()) return;

        FontMetrics fm = g2.getFontMetrics();
        String normalizedText = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] paragraphs = normalizedText.split("\n");
        int lineCount = 0;
        int currentY = startY;

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) continue;

                int cX = (lineCount == 0) ? startX : subsequentX;
                int cW = (lineCount == 0) ? firstLineWidth : subsequentLineWidth;

                if (fm.stringWidth(word) > cW) {
                    if (currentLine.length() > 0) {
                        g2.drawString(currentLine.toString(), cX, currentY);
                        lineCount++;
                        currentY += lineSpacing;
                        if (lineCount >= maxLines) return;
                        currentLine = new StringBuilder();
                    }
                    StringBuilder part = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (fm.stringWidth(part.toString() + c) <= cW) {
                            part.append(c);
                        } else {
                            g2.drawString(part.toString(), (lineCount == 0) ? startX : subsequentX, currentY);
                            lineCount++;
                            currentY += lineSpacing;
                            if (lineCount >= maxLines) return;
                            part = new StringBuilder(String.valueOf(c));
                        }
                    }
                    currentLine = part;
                } else {
                    String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                    if (fm.stringWidth(testLine) <= cW) {
                        if (currentLine.length() > 0) currentLine.append(" ");
                        currentLine.append(word);
                    } else {
                        g2.drawString(currentLine.toString(), cX, currentY);
                        lineCount++;
                        currentY += lineSpacing;
                        if (lineCount >= maxLines) return;
                        currentLine = new StringBuilder(word);
                    }
                }
            }

            if (currentLine.length() > 0) {
                g2.drawString(currentLine.toString(), (lineCount == 0) ? startX : subsequentX, currentY);
                lineCount++;
                currentY += lineSpacing;
                if (lineCount >= maxLines) return;
            } else if (paragraph.isEmpty()) {
                lineCount++;
                currentY += lineSpacing;
                if (lineCount >= maxLines) return;
            }
        }
    }

    // ═══ Data extraction helpers ═══

    private String getStr(Map<String, Object> data, String key, String defaultValue) {
        if (data.containsKey(key) && data.get(key) != null) {
            return data.get(key).toString();
        }
        return defaultValue;
    }

    private boolean getBool(Map<String, Object> data, String key) {
        if (data.containsKey(key) && data.get(key) != null) {
            Object val = data.get(key);
            if (val instanceof Boolean) return (Boolean) val;
            return Boolean.parseBoolean(val.toString());
        }
        return false;
    }

    /**
     * Delete PDF file
     */
    public boolean deletePdf(String pdfPath) {
        if (pdfPath == null) return false;
        try {
            File file = new File(pdfPath);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            log.error("Failed to delete PDF: {}", pdfPath, e);
        }
        return false;
    }
}
