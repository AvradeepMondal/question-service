package com.avradeep.QuestionService.rag.vectorstore;

import com.avradeep.QuestionService.entity.DocumentChunk;

import java.util.List;

public interface VectorStoreService {

    void storeChunks(List<DocumentChunk> chunks);
}
