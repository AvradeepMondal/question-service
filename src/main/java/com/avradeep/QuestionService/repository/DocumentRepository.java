package com.avradeep.QuestionService.repository;

import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.DocumentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends MongoRepository<Document, String> {

    // Find document by SHA-256 hash
    Optional<Document> findByFileHash(String fileHash);

    // Find documents by document processing status
    List<Document> findByStatus(DocumentStatus status);

}
