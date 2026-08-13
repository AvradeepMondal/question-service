package com.avradeep.QuestionService.util.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
@Slf4j
public class TesseractOcrService implements OcrService {

    @Value("${tesseract.datapath}")
    private String tessdataPath;

    @Value("${tesseract.language:eng}")
    private String language;

    @Override
    public String extractText(BufferedImage image) {

        try {

            log.debug("Starting OCR on image.");

            Tesseract tesseract = new Tesseract();

            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(3);
            String text =
                    tesseract.doOCR(image);

            log.debug(
                    "OCR completed. Extracted {} characters.",
                    text != null ? text.length() : 0
            );

            return text != null ? text : "";

        } catch (TesseractException e) {

            log.error(
                    "OCR extraction failed.",
                    e
            );

            throw new RuntimeException(
                    "Failed to extract text using OCR.",
                    e
            );
        }
    }
}