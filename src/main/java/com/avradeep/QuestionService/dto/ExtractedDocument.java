package com.avradeep.QuestionService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDocument {

    private String text;

    private int pageCount;

    private int textCharacterCount;

    private int ocrCharacterCount;
}