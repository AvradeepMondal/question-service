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
public class UserQuestionDto {
    private String id;

    private String quizId;

    private String questionText;

    private List<String> options;
}
