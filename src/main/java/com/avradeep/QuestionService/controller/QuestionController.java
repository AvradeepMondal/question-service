package com.avradeep.QuestionService.controller;


import com.avradeep.QuestionService.dto.AdminQuestionDto;
import com.avradeep.QuestionService.dto.QuestionEvaluationDto;
import com.avradeep.QuestionService.dto.UserQuestionDto;
import com.avradeep.QuestionService.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/{quizId}")
    public ResponseEntity<List<UserQuestionDto>> getQuestions(@PathVariable String quizId) {

        List<UserQuestionDto> questions = questionService.getQuestionsByQuizId(quizId)
                .stream()
                .map(q -> UserQuestionDto.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .options(q.getOptions())
                        .build())
                .toList();

        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{quizId}/evaluation")
    public ResponseEntity<List<QuestionEvaluationDto>> getQuestionsForEvaluation(
            @PathVariable String quizId) {

        List<QuestionEvaluationDto> questions =
                questionService.getQuestionsByQuizId(quizId)
                        .stream()
                        .map(q -> QuestionEvaluationDto.builder()
                                .id(q.getId())
                                .quizId(q.getQuizId())
                                .questionText(q.getQuestionText())
                                .options(q.getOptions())
                                .correctAnswer(q.getCorrectAnswer())
                                .build())
                        .toList();

        return ResponseEntity.ok(questions);
    }
}