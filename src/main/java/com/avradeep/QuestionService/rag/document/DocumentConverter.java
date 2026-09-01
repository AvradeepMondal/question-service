package com.avradeep.QuestionService.rag.document;

import com.avradeep.QuestionService.entity.DocumentChunk;
import org.springframework.ai.document.Document;

import java.util.List;

public interface DocumentConverter {

    Document convert(DocumentChunk chunk);

    List<Document> convertAll(List<DocumentChunk> chunks);
}
