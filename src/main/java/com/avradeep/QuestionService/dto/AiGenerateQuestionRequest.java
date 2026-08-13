package com.avradeep.QuestionService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
*   QUESTION-SERVICE TO AI-SERVICE 
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiGenerateQuestionRequest {

    private String extractedText;

    private Difficulty difficulty;

    private int numberOfQuestions;
}
