package com.avradeep.QuestionService.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateQuestionRequest {

    private String documentId;

    private int numberOfQuestions;

    private Difficulty difficulty;
}
