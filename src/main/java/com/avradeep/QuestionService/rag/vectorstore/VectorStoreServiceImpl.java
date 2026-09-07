package com.avradeep.QuestionService.rag.vectorstore;

import com.avradeep.QuestionService.entity.DocumentChunk;
import com.avradeep.QuestionService.rag.document.DocumentConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreServiceImpl
        implements VectorStoreService {

    private static final int BATCH_SIZE = 50;

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

        log.info(
                "Starting vector storage. Total documents: {}",
                documents.size()
        );

        for (int i = 0; i < documents.size(); i += BATCH_SIZE) {

            int end =
                    Math.min(
                            i + BATCH_SIZE,
                            documents.size()
                    );

            List<Document> batch =
                    documents.subList(i, end);

            log.info(
                    "Storing embedding batch: {} - {} / {}",
                    i + 1,
                    end,
                    documents.size()
            );

            vectorStore.add(batch);
        }

        log.info(
                "All vector batches stored successfully. Total: {}",
                documents.size()
        );
    }
}
