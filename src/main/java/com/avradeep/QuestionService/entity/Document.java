package com.avradeep.QuestionService.entity;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    private String id;

    // Quiz for which this document is uploaded
    private String quizId;

    // Original uploaded file name
    private String fileName;

    // PDF / DOCX / JPG / PNG
    private FileType fileType;

    // Location where the file is stored
    private String filePath;

    // Extracted text from the document
    private String extractedText;

    // Current processing status
    private GenerationStatus status;

    // User(Admin) who uploaded the document
    private String uploadedBy;

    private Date uploadedAt;
}
