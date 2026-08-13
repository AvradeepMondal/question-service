package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.dto.ExtractedDocument;
import com.avradeep.QuestionService.entity.FileType;
import com.avradeep.QuestionService.exceptions.DocumentExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@Slf4j
public class TxtDocumentExtractor implements DocumentExtractor {

    @Override
    public ExtractedDocument extract(String filePath)
            throws IOException {

        log.info(
                "Extracting text from TXT: {}",
                filePath
        );

        try {

            String text =
                    Files.readString(
                            Path.of(filePath)
                    );

            log.info(
                    "TXT extraction completed. Extracted {} characters.",
                    text.length()
            );

            return ExtractedDocument.builder()
                    .text(text)
                    .pageCount(0)
                    .textCharacterCount(text.length())
                    .ocrCharacterCount(0)
                    .build();

        } catch (IOException e) {

            log.error(
                    "Failed to extract TXT: {}",
                    filePath,
                    e
            );

            throw new DocumentExtractionException(
                    "Unable to extract text from TXT.",
                    e
            );
        }
    }

    @Override
    public Set<FileType> supports() {

        return Set.of(FileType.TXT);
    }
}