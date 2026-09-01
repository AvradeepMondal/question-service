package com.avradeep.QuestionService.rag.chunk;

import com.avradeep.QuestionService.entity.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerImpl implements TextChunker {

    private static final int MAX_CHUNK_SIZE = 3000;
    private static final int MIN_CHUNK_SIZE = 500;

    @Override
    public List<DocumentChunk> chunk(
            String documentId,
            String extractedText) {

        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Document ID cannot be null or empty."
            );
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                    "Extracted text cannot be null or empty."
            );
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        String[] paragraphs =
                extractedText.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();

        int chunkIndex = 0;

        for (String paragraph : paragraphs) {

            paragraph = paragraph.trim();

            if (paragraph.isBlank()) {
                continue;
            }

            if (currentChunk.length() + paragraph.length()
                    > MAX_CHUNK_SIZE
                    && currentChunk.length() >= MIN_CHUNK_SIZE) {

                chunks.add(
                        createChunk(
                                documentId,
                                chunkIndex++,
                                currentChunk.toString()
                        )
                );

                currentChunk.setLength(0);
            }

            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n\n");
            }

            currentChunk.append(paragraph);
        }

        if (!currentChunk.isEmpty()) {

            chunks.add(
                    createChunk(
                            documentId,
                            chunkIndex,
                            currentChunk.toString()
                    ));
        }

        return chunks;
    }

    private DocumentChunk createChunk(
            String documentId,
            int chunkIndex,
            String text) {

        return DocumentChunk.builder()
                .documentId(documentId)
                .chunkId(
                        documentId +
                                "_chunk_" +
                                chunkIndex
                )
                .chunkIndex(chunkIndex)
                .text(text.trim())
                .pageNumber(null)
                .build();
    }
}
