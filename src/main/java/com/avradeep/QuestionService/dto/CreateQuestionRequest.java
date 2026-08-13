package com.avradeep.QuestionService.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateQuestionRequest {
    private String quizId;
    private String questionText;
    private List<String> options;
    private int correctAnswer;
}
