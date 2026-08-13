package com.avradeep.QuestionService.util.ocr;

import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

@Component
public class DefaultImagePreprocessor
        implements ImagePreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage image) {

        BufferedImage gray =
                new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY
                );

        Graphics2D graphics =
                gray.createGraphics();

        graphics.drawImage(
                image,
                0,
                0,
                null
        );

        graphics.dispose();

        return gray;
    }
}
