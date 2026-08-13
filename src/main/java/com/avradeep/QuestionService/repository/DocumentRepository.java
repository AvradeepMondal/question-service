package com.avradeep.QuestionService.repository;

import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.GenerationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends MongoRepository<Document, String> {

    // Find all documents uploaded for a quiz
    List<Document> findByQuizId(String quizId);

    // Find a specific document by quiz and id
    Optional<Document> findByIdAndQuizId(String documentId, String quizId);

    // Find all documents with a particular AI generation status
    List<Document> findByStatus(GenerationStatus status);

    // Delete all uploaded documents associated with a quiz
    void deleteByQuizId(String quizId);

}
