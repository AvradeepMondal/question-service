package com.avradeep.QuestionService.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private String documentId;

    private String chunkId;

    private int chunkIndex;

    private String text;

    private Integer pageNumber;
}
