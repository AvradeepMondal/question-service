package com.avradeep.QuestionService.rag.document;

import com.avradeep.QuestionService.entity.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentConverterImpl implements DocumentConverter{

    @Override
    public Document convert(DocumentChunk chunk) {

        if (chunk == null) {
            throw new IllegalArgumentException(
                    "Document chunk cannot be null."
            );
        }

        if (chunk.getText() == null ||
                chunk.getText().isBlank()) {

            throw new IllegalArgumentException(
                    "Document chunk text cannot be null or empty."
            );
        }

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("chunkId", chunk.getChunkId());
        metadata.put("chunkIndex", chunk.getChunkIndex());

        if (chunk.getPageNumber() != null) {
            metadata.put(
                    "pageNumber",
                    chunk.getPageNumber()
            );
        }

        return Document.builder()
                .id(chunk.getChunkId())
                .text(chunk.getText())
                .metadata(metadata)
                .build();
    }

    @Override
    public List<Document> convertAll(
            List<DocumentChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Document chunks cannot be null or empty."
            );
        }

        return chunks.stream()
                .map(this::convert)
                .toList();
    }
}
