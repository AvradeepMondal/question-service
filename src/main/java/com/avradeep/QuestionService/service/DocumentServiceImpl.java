package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.client.QuizClient;
import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.DocumentStatus;
import com.avradeep.QuestionService.entity.FileType;
import com.avradeep.QuestionService.repository.DocumentRepository;
import com.avradeep.QuestionService.util.extractor.FileValidator;
import com.avradeep.QuestionService.util.hash.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final String UPLOAD_DIR = "uploads";

    private final DocumentRepository documentRepository;
    private final FileValidator fileValidator;
    private final DocumentProcessingService documentProcessingService;
    private final QuizClient quizClient;

    @Override
    public DocumentUploadResponse uploadDocument(
            String quizId,
            MultipartFile file) {

        log.info(
                "Received document upload request for quizId: {}",
                quizId);

        fileValidator.validate(file);

        log.info(
                "File validation successful. File Name: {}",
                file.getOriginalFilename());

        try {

            // ==============================
            // 1. Calculate SHA-256
            // ==============================

            String fileHash =
                    FileHashUtil.calculateSHA256(file);

            log.info(
                    "SHA-256 calculated for file: {}",
                    file.getOriginalFilename());

            // ==============================
            // 2. Check existing document
            // ==============================

            Optional<Document> existingDocument =
                    documentRepository.findByFileHash(fileHash);

            if (existingDocument.isPresent()) {

                Document document =
                        existingDocument.get();

                log.info(
                        "Document already exists. Reusing documentId: {}",
                        document.getId());

                // Attach existing document to quiz
                quizClient.addDocumentToQuiz(
                        quizId,
                        document.getId());

                return DocumentUploadResponse.builder()
                        .documentId(document.getId())
                        .quizId(quizId)
                        .fileName(document.getFileName())
                        .fileType(document.getFileType())
                        .status(document.getStatus())
                        .build();
            }

            // ==============================
            // 3. Create upload directory
            // ==============================

            File directory =
                    new File(UPLOAD_DIR);

            if (!directory.exists()) {

                directory.mkdirs();

                log.info(
                        "Upload directory created at: {}",
                        directory.getAbsolutePath());
            }

            // ==============================
            // 4. Store physical file
            // ==============================

            String originalFileName =
                    file.getOriginalFilename();

            String uniqueFileName =
                    UUID.randomUUID()
                            + "_"
                            + originalFileName;

            Path filePath =
                    Paths.get(
                            UPLOAD_DIR,
                            uniqueFileName);

            Files.copy(
                    file.getInputStream(),
                    filePath);

            log.info(
                    "File stored successfully at: {}",
                    filePath.toAbsolutePath());

            // ==============================
            // 5. Create Document
            // ==============================

            Document document =
                    Document.builder()
                            .fileName(originalFileName)
                            .fileType(
                                    FileType.fromFileName(
                                            originalFileName))
                            .filePath(filePath.toString())
                            .fileHash(fileHash)
                            .status(DocumentStatus.UPLOADED)
                            .build();

            Document savedDocument =
                    documentRepository.save(document);

            log.info(
                    "Document metadata saved successfully. DocumentId: {}",
                    savedDocument.getId());

            // ==============================
            // 6. Attach document to quiz
            // ==============================

            quizClient.addDocumentToQuiz(
                    quizId,
                    savedDocument.getId());

            // ==============================
            // 7. Process document
            // ==============================

            documentProcessingService.processDocument(
                    savedDocument.getId());

            log.info(
                    "Document uploaded successfully. Document Id: {}",
                    savedDocument.getId());

            // ==============================
            // 8. Return response
            // ==============================

            return DocumentUploadResponse.builder()
                    .documentId(savedDocument.getId())
                    .quizId(quizId)
                    .fileName(savedDocument.getFileName())
                    .fileType(savedDocument.getFileType())
                    .status(savedDocument.getStatus())
                    .build();

        } catch (IOException e) {

            log.error(
                    "Failed to upload document.",
                    e);

            throw new RuntimeException(
                    "Unable to upload document.",
                    e);
        }
    }

    @Override
    public List<DocumentDto> getAllDocument(String quizId) {

        log.info("Fetching all documents for quizId: {}", quizId);

        List<String> documentIds =
                quizClient.getDocumentIdsByQuizId(quizId);

        List<Document> documents =
                documentRepository.findAllById(documentIds);

        log.info("Found {} document(s) for quizId: {}",
                documents.size(), quizId);

        return documents.stream()
                .map(document -> DocumentDto.builder()
                        .documentId(document.getId())
                        .quizId(quizId)
                        .fileName(document.getFileName())
                        .fileType(document.getFileType())
                        .status(document.getStatus())
                        .build())
                .toList();
    }

    @Override
    public void deleteByQuizId(DeleteDocumentRequest request) {

        log.info(
                "Removing document {} from quiz {}",
                request.getDocumentId(),
                request.getQuizId());

        quizClient.removeDocumentFromQuiz(
                request.getQuizId(),
                request.getDocumentId());

        log.info(
                "Document {} successfully removed from quiz {}",
                request.getDocumentId(),
                request.getQuizId());
    }
}
