package com.avradeep.QuestionService.client;

import com.avradeep.QuestionService.dto.AiGenerateQuestionRequest;
import com.avradeep.QuestionService.dto.GeneratedQuestionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "AI-SERVICE")
public interface AIClient {

    @PostMapping("/internal/ai/questions/generate")
    List<GeneratedQuestionResponse> generateQuestions(
            @RequestBody
            AiGenerateQuestionRequest request);
}
