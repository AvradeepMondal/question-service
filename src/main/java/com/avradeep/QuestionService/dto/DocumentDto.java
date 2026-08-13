package com.avradeep.QuestionService.dto;

import com.avradeep.QuestionService.entity.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private String documentId;

    private String quizId;

    private String fileName;

    private FileType fileType;
}
