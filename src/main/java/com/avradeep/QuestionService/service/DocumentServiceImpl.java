package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.FileType;
import com.avradeep.QuestionService.entity.GenerationStatus;
import com.avradeep.QuestionService.repository.DocumentRepository;
import com.avradeep.QuestionService.util.extractor.FileValidator;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final String UPLOAD_DIR = "uploads";

    private final DocumentRepository documentRepository;
    private final FileValidator fileValidator;

    @Override
    public DocumentUploadResponse uploadDocument(
            String quizId,
            MultipartFile file) {

        log.info("Received document upload request for quizId: {}", quizId);

        fileValidator.validate(file);

        log.info("File validation successful. File Name: {}",
                file.getOriginalFilename());

        try {

            File directory = new File(UPLOAD_DIR);

            if (!directory.exists()) {
                directory.mkdirs();
                log.info("Upload directory created at: {}",
                        directory.getAbsolutePath());
            }

            String originalFileName = file.getOriginalFilename();

            String uniqueFileName =
                    UUID.randomUUID() + "_" + originalFileName;

            Path filePath =
                    Paths.get(UPLOAD_DIR, uniqueFileName);

            Files.copy(file.getInputStream(), filePath);

            log.info("File stored successfully at: {}",
                    filePath.toAbsolutePath());

            Document document = Document.builder()
                    .quizId(quizId)
                    .fileName(originalFileName)
                    .fileType(FileType.fromFileName(originalFileName))
                    .filePath(filePath.toString())
                    .status(GenerationStatus.UPLOADED)
                    .build();

            Document savedDocument =
                    documentRepository.save(document);

            log.info(
                    "Document metadata saved successfully. DocumentId: {}, QuizId: {}",
                    savedDocument.getId(),
                    savedDocument.getQuizId()
            );

            log.info("Document uploaded successfully. Document Id: {}",
                    savedDocument.getId());

            return DocumentUploadResponse.builder()
                    .documentId(savedDocument.getId())
                    .quizId(savedDocument.getQuizId())
                    .fileName(savedDocument.getFileName())
                    .fileType(savedDocument.getFileType())
                    .status(savedDocument.getStatus())
                    .build();

        } catch (IOException e) {

            log.error("Failed to upload document.", e);

            throw new RuntimeException(
                    "Unable to upload document.");
        }
    }

    @Override
    public List<DocumentDto> getAllDocument(String quizId) {

        log.info("Fetching all documents for quizId: {}", quizId);

        List<Document> documents = documentRepository.findByQuizId(quizId);

        log.info("Found {} document(s) for quizId: {}",
                documents.size(), quizId);

        /**
         *  String documentId;
         *  String quizId;
         *  String fileName;
         *  FileType fileType;
         */
        return documents.stream()
                .map(document -> DocumentDto.builder()
                        .documentId(document.getId())
                        .quizId(document.getQuizId())
                        .fileName(document.getFileName())
                        .fileType(document.getFileType())
                        .build())
                .toList();
    }

    @Override
    public void deleteByQuizId(DeleteDocumentRequest request) {

        log.info("Received request to delete document {} for quiz {}",
                request.getDocumentId(),
                request.getQuizId());

        Document document = documentRepository
                .findByIdAndQuizId(
                        request.getDocumentId(),
                        request.getQuizId()
                )
                .orElseThrow(() -> {

                    log.error("Document {} not found for quiz {}",
                            request.getDocumentId(),
                            request.getQuizId());

                    return new RuntimeException(
                            "Document " + request.getDocumentId()
                                    + " does not belong to quiz "
                                    + request.getQuizId()
                    );
                });

        // Delete the physical file
        try {

            Path filePath = Paths.get(document.getFilePath());

            if (Files.exists(filePath)) {

                Files.delete(filePath);

                log.info("Deleted file from storage: {}",
                        document.getFilePath());

            } else {

                log.warn("File not found on disk: {}",
                        document.getFilePath());
            }

        } catch (IOException e) {

            log.error("Failed to delete file: {}",
                    document.getFilePath(), e);

            throw new RuntimeException(
                    "Unable to delete document file.", e);
        }

        // Delete MongoDB record
        documentRepository.delete(document);

        log.info("Document {} deleted successfully.",
                document.getId());
    }
}
