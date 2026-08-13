package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.dto.ExtractedDocument;
import com.avradeep.QuestionService.entity.FileType;
import com.avradeep.QuestionService.util.ocr.ImagePreprocessor;
import com.avradeep.QuestionService.util.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentExtractor implements DocumentExtractor {

    private final OcrService ocrService;
    private final ImagePreprocessor imagePreprocessor;

    /*
     * DPI used when rendering PDF pages for OCR.
     *
     * 250 DPI is a good starting point:
     * - Better OCR quality than low-resolution rendering
     * - Less expensive than 300 DPI
     *
     * Increase to 300 if OCR quality is still poor.
     */
    private static final float OCR_DPI = 300f;

    /*
     * Minimum amount of native text required to consider
     * a PDF page text-rich enough that OCR is unnecessary.
     */
    private static final int MIN_NATIVE_TEXT_LENGTH = 50;

    @Override
    public ExtractedDocument extract(String filePath)
            throws IOException {

        log.info(
                "Starting PDF content extraction: {}",
                filePath
        );

        File file = new File(filePath);

        if (!file.exists()) {

            log.error(
                    "PDF file not found: {}",
                    filePath
            );

            throw new IOException(
                    "PDF file not found: " + filePath
            );
        }

        try (PDDocument document = Loader.loadPDF(file)) {

            int pageCount =
                    document.getNumberOfPages();

            log.info(
                    "PDF loaded successfully. Total pages: {}",
                    pageCount
            );

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            PDFRenderer renderer =
                    new PDFRenderer(document);

            StringBuilder combinedText =
                    new StringBuilder();

            int totalNativeCharacters = 0;
            int totalOcrCharacters = 0;

            int ocrPagesProcessed = 0;
            int ocrPagesAccepted = 0;
            int ocrPagesRejected = 0;

            // =====================================================
            // Process each page
            // =====================================================

            for (int pageIndex = 0;
                 pageIndex < pageCount;
                 pageIndex++) {

                int pageNumber =
                        pageIndex + 1;

                log.debug(
                        "Processing PDF page {}/{}",
                        pageNumber,
                        pageCount
                );

                // =================================================
                // 1. Extract native PDF text
                // =================================================

                textStripper.setStartPage(
                        pageNumber
                );

                textStripper.setEndPage(
                        pageNumber
                );

                String pageText =
                        textStripper.getText(
                                document
                        );

                if (pageText == null) {
                    pageText = "";
                }

                pageText = pageText.trim();

                totalNativeCharacters +=
                        pageText.length();

                // =================================================
                // 2. Determine whether OCR is required
                // =================================================

                boolean nativeTextSufficient =
                        isNativeTextSufficient(
                                pageText
                        );

                String pageOcrText = "";

                if (nativeTextSufficient) {

                    log.debug(
                            "Page {} has sufficient native text " +
                                    "({} chars). OCR skipped.",
                            pageNumber,
                            pageText.length()
                    );

                } else {

                    log.debug(
                            "Page {} has insufficient native text " +
                                    "({} chars). Starting OCR.",
                            pageNumber,
                            pageText.length()
                    );

                    // =============================================
                    // 3. Render complete PDF page
                    // =============================================

                    BufferedImage renderedPage =
                            renderer.renderImageWithDPI(
                                    pageIndex,
                                    OCR_DPI
                            );

                    ocrPagesProcessed++;

                    // =============================================
                    // 4. Preprocess page image
                    // =============================================

                    BufferedImage processedImage =
                            imagePreprocessor.preprocess(
                                    renderedPage
                            );

                    // =============================================
                    // 5. Perform OCR
                    // =============================================

                    String ocrText =
                            ocrService.extractText(
                                    processedImage
                            );

                    if (ocrText != null) {

                        pageOcrText =
                                ocrText.trim();
                    }

                    // =============================================
                    // 6. OCR quality check
                    // =============================================

                    if (!pageOcrText.isBlank()) {

                        totalOcrCharacters +=
                                pageOcrText.length();

                        if (isReliableOcr(pageOcrText)) {

                            ocrPagesAccepted++;

                            log.debug(
                                    "OCR accepted for page {}. " +
                                            "Characters: {}",
                                    pageNumber,
                                    pageOcrText.length()
                            );

                        } else {

                            ocrPagesRejected++;

                            log.warn(
                                    "OCR rejected for page {} " +
                                            "because the extracted text " +
                                            "appears unreliable. " +
                                            "Characters: {}",
                                    pageNumber,
                                    pageOcrText.length()
                            );

                            pageOcrText = "";
                        }

                    } else {

                        ocrPagesRejected++;

                        log.warn(
                                "OCR returned no usable text for page {}",
                                pageNumber
                        );
                    }
                }

                // =================================================
                // 7. Build page-aware document content
                // =================================================

                if (!pageText.isBlank() ||
                        !pageOcrText.isBlank()) {

                    combinedText
                            .append("\n\n")
                            .append("====================\n")
                            .append("[PAGE ")
                            .append(pageNumber)
                            .append("]\n")
                            .append("====================\n\n");

                    // ---------------------------------------------
                    // Native text
                    // ---------------------------------------------

                    if (!pageText.isBlank()) {

                        combinedText
                                .append("[NATIVE TEXT]\n")
                                .append(pageText)
                                .append("\n\n");
                    }

                    // ---------------------------------------------
                    // OCR text
                    // ---------------------------------------------

                    if (!pageOcrText.isBlank()) {

                        combinedText
                                .append("[OCR CONTENT]\n")
                                .append(pageOcrText)
                                .append("\n");
                    }
                }

                log.debug(
                        "Page {} completed. Native chars: {}, " +
                                "OCR chars: {}, OCR used: {}",
                        pageNumber,
                        pageText.length(),
                        pageOcrText.length(),
                        !pageOcrText.isBlank()
                );
            }

            // =====================================================
            // Final extraction statistics
            // =====================================================

            log.info(
                    "PDF extraction completed. " +
                            "Pages: {}, " +
                            "OCR pages processed: {}, " +
                            "OCR pages accepted: {}, " +
                            "OCR pages rejected: {}, " +
                            "Native chars: {}, " +
                            "OCR chars: {}, " +
                            "Combined chars: {}",
                    pageCount,
                    ocrPagesProcessed,
                    ocrPagesAccepted,
                    ocrPagesRejected,
                    totalNativeCharacters,
                    totalOcrCharacters,
                    combinedText.length()
            );

            return ExtractedDocument.builder()
                    .text(
                            combinedText.toString()
                    )
                    .pageCount(
                            pageCount
                    )
                    .textCharacterCount(
                            totalNativeCharacters
                    )
                    .ocrCharacterCount(
                            totalOcrCharacters
                    )
                    .build();
        }
    }

    // =============================================================
    // Native text quality check
    // =============================================================

    private boolean isNativeTextSufficient(
            String text) {

        if (text == null ||
                text.isBlank()) {

            return false;
        }

        String cleaned =
                text.trim();

        /*
         * Very small native text is usually not enough
         * to represent the actual page content.
         */
        if (cleaned.length() <
                MIN_NATIVE_TEXT_LENGTH) {

            return false;
        }

        /*
         * Count alphabetic characters.
         *
         * This prevents pages containing only things like
         * page numbers, symbols, or isolated characters
         * from being treated as useful native text.
         */
        long alphabeticCharacters =
                cleaned.chars()
                        .filter(Character::isLetter)
                        .count();

        double alphabeticRatio =
                (double) alphabeticCharacters
                        / cleaned.length();

        return alphabeticRatio >= 0.30;
    }

    // =============================================================
    // OCR quality check
    // =============================================================

    private boolean isReliableOcr(
            String text) {

        if (text == null ||
                text.isBlank()) {

            return false;
        }

        String cleaned =
                text.trim();

        /*
         * Very small OCR output is usually not
         * useful educational content.
         */
        if (cleaned.length() < 20) {

            return false;
        }

        long alphabeticCharacters =
                cleaned.chars()
                        .filter(Character::isLetter)
                        .count();

        long digits =
                cleaned.chars()
                        .filter(Character::isDigit)
                        .count();

        long suspiciousCharacters =
                cleaned.chars()
                        .filter(c ->
                                !Character.isLetterOrDigit(c)
                                        && !Character.isWhitespace(c)
                                        && !".,;:!?()'\"-/%+=<>[]"
                                        .contains(
                                                String.valueOf(
                                                        (char) c
                                                )
                                        )
                        )
                        .count();

        double alphabeticRatio =
                (double) alphabeticCharacters
                        / cleaned.length();

        double suspiciousRatio =
                (double) suspiciousCharacters
                        / cleaned.length();

        /*
         * Require a reasonable amount of alphabetic
         * content and prevent extremely noisy OCR.
         */
        boolean hasEnoughAlphabeticContent =
                alphabeticRatio >= 0.30;

        boolean notTooManySuspiciousCharacters =
                suspiciousRatio <= 0.20;

        /*
         * Avoid accepting OCR that consists mostly
         * of numbers/symbols.
         */
        boolean notOnlyNumbers =
                alphabeticCharacters >= 10;

        return hasEnoughAlphabeticContent
                && notTooManySuspiciousCharacters
                && notOnlyNumbers;
    }

    @Override
    public Set<FileType> supports() {

        return Set.of(
                FileType.PDF
        );
    }
}