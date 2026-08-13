package com.avradeep.QuestionService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneratedQuestionResponse {

    private String questionText;

    private List<String> options;

    private int correctAnswer;
}
