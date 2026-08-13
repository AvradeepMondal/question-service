package com.avradeep.QuestionService.repository;

import com.avradeep.QuestionService.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {

    // Fetch all questions of a quiz
    List<Question> findByQuizId(String quizId);

    // Delete all questions of a quiz(FEIGN-CLIENT)
    void deleteByQuizId(String quizId);

    // Check if a quiz has any questions
    boolean existsByQuizId(String quizId);

    // Find a specific question of a quiz
    Optional<Question> findByIdAndQuizId(String questionId, String quizId);

    // Count total questions in a quiz
    long countByQuizId(String quizId);
}
