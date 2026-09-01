package com.avradeep.QuestionService.rag.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RagRetrievalService {

    List<Document> retrieve(
            String documentId,
            String query,
            int topK
    );
}
