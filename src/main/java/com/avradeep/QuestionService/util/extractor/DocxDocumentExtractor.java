package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.dto.ExtractedDocument;
import com.avradeep.QuestionService.entity.FileType;
import com.avradeep.QuestionService.exceptions.DocumentExtractionException;
import com.avradeep.QuestionService.util.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocxDocumentExtractor implements DocumentExtractor {

    private final OcrService ocrService;

    @Override
    public ExtractedDocument extract(String filePath) {

        log.info(
                "Starting DOCX content extraction from {}",
                filePath
        );

        StringBuilder documentText =
                new StringBuilder();

        StringBuilder ocrText =
                new StringBuilder();

        int imageCount = 0;

        try (
                FileInputStream fis =
                        new FileInputStream(filePath);

                XWPFDocument document =
                        new XWPFDocument(fis)
        ) {

            // =========================================
            // 1. Extract normal paragraphs
            // =========================================

            log.debug("Extracting DOCX paragraphs...");

            for (XWPFParagraph paragraph :
                    document.getParagraphs()) {

                String text = paragraph.getText();

                if (text != null && !text.isBlank()) {

                    documentText
                            .append(text)
                            .append("\n");
                }
            }

            // =========================================
            // 2. Extract tables
            // =========================================

            log.debug("Extracting DOCX tables...");

            for (XWPFTable table :
                    document.getTables()) {

                for (XWPFTableRow row :
                        table.getRows()) {

                    for (XWPFTableCell cell :
                            row.getTableCells()) {

                        String text =
                                cell.getText();

                        if (text != null &&
                                !text.isBlank()) {

                            documentText
                                    .append(text)
                                    .append(" ");
                        }
                    }

                    documentText.append("\n");
                }
            }

            // =========================================
            // 3. Extract embedded images
            // =========================================

            log.debug("Extracting embedded DOCX images...");

            for (XWPFPictureData pictureData :
                    document.getAllPictures()) {

                imageCount++;

                log.debug(
                        "Processing DOCX image {}",
                        imageCount
                );

                byte[] imageBytes =
                        pictureData.getData();

                BufferedImage image =
                        ImageIO.read(
                                new ByteArrayInputStream(
                                        imageBytes
                                )
                        );

                if (image == null) {

                    log.warn(
                            "Unable to read DOCX image {}",
                            imageCount
                    );

                    continue;
                }

                // =====================================
                // 4. OCR
                // =====================================

                String extractedImageText =
                        ocrService.extractText(image);

                if (extractedImageText != null &&
                        !extractedImageText.isBlank()) {

                    ocrText
                            .append("\n[IMAGE ")
                            .append(imageCount)
                            .append(" OCR]\n")
                            .append(extractedImageText)
                            .append("\n");
                }
            }

            // =========================================
            // 5. Combine normal text + OCR text
            // =========================================

            String combinedText = """
                    ===== DOCUMENT TEXT =====
                    
                    %s
                    
                    ===== OCR CONTENT =====
                    
                    %s
                    """.formatted(
                    documentText,
                    ocrText
            );

            log.info(
                    "DOCX extraction completed. " +
                            "Text characters: {}, " +
                            "OCR characters: {}, " +
                            "Images processed: {}, " +
                            "Total characters: {}",
                    documentText.length(),
                    ocrText.length(),
                    imageCount,
                    combinedText.length()
            );

            return ExtractedDocument.builder()
                    .text(combinedText)
                    .textCharacterCount(
                            documentText.length()
                    )
                    .ocrCharacterCount(
                            ocrText.length()
                    )
                    .build();

        } catch (IOException e) {

            log.error(
                    "Failed to extract DOCX content from {}",
                    filePath,
                    e
            );

            throw new DocumentExtractionException(
                    "Unable to extract content from DOCX.",
                    e
            );
        }
    }

    @Override
    public Set<FileType> supports() {

        return Set.of(FileType.DOCX);
    }
}
