package com.avradeep.QuestionService.rag.chunk;

import com.avradeep.QuestionService.entity.DocumentChunk;

import java.util.List;

public interface TextChunker {
    List<DocumentChunk> chunk(
            String documentId,
            String extractedText
    );
}
