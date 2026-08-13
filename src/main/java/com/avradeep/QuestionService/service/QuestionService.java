package com.avradeep.QuestionService.service;



import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Question;

import java.util.List;

public interface QuestionService {

    // -------------- CRUD --------------
    List<AdminQuestionDto> getQuestionsByQuizId(String quizId);

    void addQuestion(CreateQuestionRequest request);

    void updateQuestion(String questionId, CreateQuestionRequest request);

    void deleteQuestion(String questionId);

    // -------------- AI Methods -----------
    List<GeneratedQuestionResponse> generateQuestions(GenerateQuestionRequest request);

    List<AdminQuestionDto> approveGeneratedQuestions(
            ApproveGeneratedQuestionsRequest
                    request);
}