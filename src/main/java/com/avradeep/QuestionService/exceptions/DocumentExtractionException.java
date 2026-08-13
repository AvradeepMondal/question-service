package com.avradeep.QuestionService.exceptions;

public class DocumentExtractionException extends RuntimeException {

    public DocumentExtractionException(String message) {
        super(message);
    }

    public DocumentExtractionException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}