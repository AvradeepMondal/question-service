package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.entity.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractorFactory {

    private final List<DocumentExtractor> extractors;

    public DocumentExtractor getExtractor(FileType fileType) {

        log.info("Finding document extractor for file type: {}", fileType);

        DocumentExtractor extractor = extractors.stream()
                .filter(e -> e.supports().contains(fileType))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No DocumentExtractor found for file type: {}", fileType);
                    return new RuntimeException(
                            "Unsupported document type: " + fileType
                    );
                });

        log.info("Using {} for {}", extractor.getClass().getSimpleName(), fileType);

        return extractor;
    }
}
