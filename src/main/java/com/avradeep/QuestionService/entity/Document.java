package com.avradeep.QuestionService.entity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    private String id;

    // Original uploaded file name
    private String fileName;

    // PDF / DOCX / JPG / PNG
    private FileType fileType;

    // Location where the file is stored
    private String filePath;

    @Indexed(unique = true)
    private String fileHash;

    // Extracted text from the document
    private String extractedText;

    // Current processing status
    private DocumentStatus status;

    // User(Admin) who uploaded the document
    private String uploadedBy;

    private Date uploadedAt;

}
