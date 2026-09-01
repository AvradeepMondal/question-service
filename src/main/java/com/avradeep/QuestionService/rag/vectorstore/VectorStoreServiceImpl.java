package com.avradeep.QuestionService.rag.vectorstore;

import com.avradeep.QuestionService.entity.DocumentChunk;
import com.avradeep.QuestionService.rag.document.DocumentConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorStore vectorStore;

    private final DocumentConverter documentConverter;

    @Override
    public void storeChunks(List<DocumentChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Document chunks cannot be null or empty."
            );
        }

        List<Document> documents =
                documentConverter.convertAll(chunks);

        vectorStore.add(documents);
    }
}
