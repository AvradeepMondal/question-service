package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentUploadResponse uploadDocument(
            String quizId,
            MultipartFile file);

    List<DocumentDto> getAllDocument(String quizId);

    void deleteByQuizId(DeleteDocumentRequest request);
}
