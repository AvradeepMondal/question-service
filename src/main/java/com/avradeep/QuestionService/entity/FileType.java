package com.avradeep.QuestionService.entity;

public enum FileType {
    PDF,
    DOCX,
    TXT;

    public static FileType fromFileName(String fileName) {

        String extension = fileName.substring(
                fileName.lastIndexOf('.') + 1
        ).toUpperCase();

        return FileType.valueOf(extension);
    }
}
