package com.avradeep.QuestionService.controller;


import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    @GetMapping("/getAllQuestions")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<AdminQuestionDto>> getAllQuestions(
            @RequestBody String quizId
    ){
        ;
        return ResponseEntity.ok(questionService.getQuestionsByQuizId(quizId));
    }

    // ✅ Add question
    @PostMapping("/add-question")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> addQuestion(@RequestBody CreateQuestionRequest request) {

        questionService.addQuestion(request);

        return ResponseEntity.ok("Question added");
    }

    // ✅ Update question
    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> updateQuestion(@PathVariable String questionId,
                                                 @RequestBody CreateQuestionRequest request) {

        questionService.updateQuestion(questionId, request);

        return ResponseEntity.ok("Question updated");
    }

    // ✅ Delete question
    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> deleteQuestion(@PathVariable String questionId) {

        questionService.deleteQuestion(questionId);

        return ResponseEntity.ok("Question deleted");
    }

    /**
     * Generate questions using AI from an uploaded document
     */

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<List<GeneratedQuestionResponse>> generateQuestions(
            @RequestBody GenerateQuestionRequest request) {

        return ResponseEntity.ok(
                questionService.generateQuestions(request)
        );
    }

    /**
     * Approve AI generated questions and save them permanently
     */

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/approve")
    public ResponseEntity<List<AdminQuestionDto>> approveQuestions(

            @RequestBody ApproveGeneratedQuestionsRequest request) {

        return ResponseEntity.ok(
                questionService.approveGeneratedQuestions(request)
        );
    }
}
