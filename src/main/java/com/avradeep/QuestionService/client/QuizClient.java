package com.avradeep.QuestionService.client;

import com.avradeep.QuestionService.dto.QuizQuestionCountResponse;
import com.avradeep.QuestionService.dto.QuizResponse;
import com.avradeep.QuestionService.dto.UpdateQuestionCountRequest;
import com.avradeep.QuestionService.security.FeignTokenPropagationConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "QUIZ-SERVICE",
        configuration = FeignTokenPropagationConfig.class
)
public interface QuizClient {

    @PutMapping("/api/quiz/question-count/{quizId}")
    QuizQuestionCountResponse updateQuestionCount(
            @PathVariable("quizId") String quizId,
            @RequestBody UpdateQuestionCountRequest request
    );

    @GetMapping("/api/quiz/{quizId}")
    QuizResponse getQuizById(
            @PathVariable("quizId") String quizId
    );

    ///api/quiz/{quizId}/documents/{documentId}

    @PostMapping("/api/quiz/{quizId}/documents/{documentId}")
    void addDocumentToQuiz(
            @PathVariable("quizId") String quizId,
            @PathVariable("documentId") String documentId
    );

    @GetMapping("/api/quiz/quizzes/{quizId}/documents")
    List<String> getDocumentIdsByQuizId(
            @PathVariable("quizId") String quizId
    );

    @DeleteMapping("/api/quiz/quizzes/{quizId}/documents/{documentId}")
    void removeDocumentFromQuiz(
            @PathVariable("quizId") String quizId,
            @PathVariable("documentId") String documentId
    );
}
