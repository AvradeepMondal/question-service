package com.avradeep.QuestionService.controller;

import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.service.DocumentProcessingService;
import com.avradeep.QuestionService.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;

    /**
     * Upload a source document (PDF/DOCX/JPG/PNG)
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> uploadDocument(

            @RequestParam("quizId") String quizId,

            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                documentService.uploadDocument(
                        quizId,
                        file
                )
        );
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @GetMapping("/allDocument")
    public ResponseEntity<List<DocumentDto>> getAllDocument(
            @RequestBody String quizID
    ){
        return ResponseEntity.ok(
                documentService.getAllDocument(quizID));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/deleteDocument")
    public ResponseEntity<?> deleteDocument(DeleteDocumentRequest request){

        documentService.deleteByQuizId(request);

        return ResponseEntity.noContent().build();
    }

    /**
        testing
     */
    @PostMapping("/{documentId}/process")
    public ResponseEntity<String> processDocument(
            @PathVariable String documentId) {

        documentProcessingService.processDocument(documentId);

        return ResponseEntity.ok(
                "Document processing started for: " + documentId
        );
    }
}
