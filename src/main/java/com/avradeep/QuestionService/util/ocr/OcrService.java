package com.avradeep.QuestionService.util.ocr;

import java.awt.image.BufferedImage;

public interface OcrService {

    String extractText(BufferedImage image);

}
