package com.avradeep.QuestionService.util.ocr;

import java.awt.image.BufferedImage;

public interface ImagePreprocessor {

    BufferedImage preprocess(BufferedImage image);
}
