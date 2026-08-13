package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.entity.FileType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FileValidator {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Arrays.stream(FileType.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty.");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.contains(".")) {
            throw new RuntimeException("Invalid file name.");
        }

        String extension = getExtension(fileName);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new RuntimeException(
                    "Unsupported file type. Allowed types are: PDF, DOCX, TXT."
            );
        }
    }

    private String getExtension(String fileName) {

        return fileName.substring(
                fileName.lastIndexOf('.') + 1
        ).toLowerCase();
    }
}
