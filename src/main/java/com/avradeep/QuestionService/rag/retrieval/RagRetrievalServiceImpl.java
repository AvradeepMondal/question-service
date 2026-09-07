package com.avradeep.QuestionService.rag.retrieval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagRetrievalServiceImpl
        implements RagRetrievalService {

    private final VectorStore vectorStore;

    @Override
    public List<Document> retrieve(
            String documentId,
            String query,
            int topK) {

        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty.");
        }

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty.");
        }

        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be greater than zero.");
        }

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression(
                                "documentId == '" +
                                        documentId +
                                        "'")
                        .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        for (Document doc : results) {
            log.info("========== RETRIEVED CHUNK ==========");
            log.info("Metadata: {}", doc.getMetadata());
            log.info("Content: {}", doc.getText());
        }

        return results;
    }
}