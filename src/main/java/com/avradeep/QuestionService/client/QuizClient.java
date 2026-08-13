package com.avradeep.QuestionService.client;

import com.avradeep.QuestionService.dto.QuizQuestionCountResponse;
import com.avradeep.QuestionService.dto.UpdateQuestionCountRequest;
import com.avradeep.QuestionService.security.FeignTokenPropagationConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
}
