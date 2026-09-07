package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.dto.ExtractedDocument;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.DocumentChunk;
import com.avradeep.QuestionService.entity.DocumentStatus;
import com.avradeep.QuestionService.rag.chunk.TextChunker;
import com.avradeep.QuestionService.rag.vectorstore.VectorStoreService;
import com.avradeep.QuestionService.repository.DocumentRepository;
import com.avradeep.QuestionService.util.extractor.DocumentExtractor;
import com.avradeep.QuestionService.util.extractor.DocumentExtractorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingServiceImpl
        implements DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentExtractorFactory extractorFactory;
    private final TextChunker textChunker;
    private final VectorStoreService vectorStoreService;

    @Async
    @Override
    public void processDocument(String documentId) {

        log.info(
                "Starting document processing. documentId: {}",
                documentId);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found: "
                                                + documentId));

        try {

            // =========================================
            // 1. Mark as PROCESSING
            // =========================================

            document.setStatus(
                    DocumentStatus.PROCESSING);

            documentRepository.save(document);

            log.info(
                    "Document marked as PROCESSING. documentId: {}",
                    documentId);

            // =========================================
            // 2. Extract document
            // =========================================

            DocumentExtractor extractor =
                    extractorFactory.getExtractor(
                            document.getFileType());

            log.info(
                    "Starting document extraction. documentId: {}",
                    documentId);

            ExtractedDocument extractedDocument =
                    extractor.extract(
                            document.getFilePath());

            String extractedText =
                    extractedDocument.getText();

            if (extractedText == null ||
                    extractedText.isBlank()) {

                throw new IllegalStateException(
                        "No usable content extracted from document.");
            }

            log.info(
                    "Document extraction completed. " +
                            "documentId: {}, pages: {}, characters: {}",
                    documentId,
                    extractedDocument.getPageCount(),
                    extractedText.length());

            // =========================================
            // 3. Save extracted text
            // =========================================

            document.setExtractedText(
                    extractedText);

            documentRepository.save(document);

            // =========================================
            // 4. Chunk document
            // =========================================

            List<DocumentChunk> chunks =
                    textChunker.chunk(
                            documentId,
                            extractedText);

            log.info(
                    "Document chunking completed. " +
                            "documentId: {}, chunks: {}",
                    documentId,
                    chunks.size());

            // =========================================
            // 5. Generate embeddings
            //    + Store vectors
            // =========================================

            vectorStoreService.storeChunks(
                    chunks);

            log.info(
                    "Document chunks embedded and stored. " +
                            "documentId: {}, chunks: {}",
                    documentId,
                    chunks.size());

            // =========================================
            // 6. Mark document READY
            // =========================================

            document.setStatus(
                    DocumentStatus.READY);

            documentRepository.save(document);

            log.info(
                    "Document processing completed successfully. " +
                            "Document is READY. documentId: {}",
                    documentId);

        } catch (Exception e) {

            log.error(
                    "Document processing failed. documentId: {}",
                    documentId,
                    e);

            // =========================================
            // 7. Mark document FAILED
            // =========================================

            document.setStatus(DocumentStatus.FAILED);

            documentRepository.save(document);

            log.info(
                    "Document marked as FAILED. documentId: {}",
                    documentId);
        }
    }
}
