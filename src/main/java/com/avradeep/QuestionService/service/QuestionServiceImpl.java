package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.client.AIClient;
import com.avradeep.QuestionService.client.QuizClient;
import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.DocumentChunk;
import com.avradeep.QuestionService.entity.GenerationStatus;
import com.avradeep.QuestionService.entity.Question;
import com.avradeep.QuestionService.exceptions.DocumentExtractionException;
import com.avradeep.QuestionService.rag.chunk.TextChunker;
import com.avradeep.QuestionService.rag.retrieval.RagRetrievalService;
import com.avradeep.QuestionService.rag.vectorstore.VectorStoreService;
import com.avradeep.QuestionService.repository.DocumentRepository;
import com.avradeep.QuestionService.repository.QuestionRepository;
import com.avradeep.QuestionService.util.extractor.DocumentExtractor;
import com.avradeep.QuestionService.util.extractor.DocumentExtractorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentExtractorFactory extractorFactory;
    private final AIClient aiClient;
    private final QuizClient quizClient;
    private final RagRetrievalService ragRetrievalService;
    private final TextChunker textChunker;
    private final VectorStoreService vectorStoreService;

    // ================= USER METHODS =================

    @Override
    public List<AdminQuestionDto> getQuestionsByQuizId(String quizId) {

        log.info("Fetching questions for quizId: {}", quizId);

        List<Question> questions = questionRepository.findByQuizId(quizId);

        log.info("Total questions fetched for quizId {}: {}", quizId, questions.size());

        return questions.stream()
                .map(question -> AdminQuestionDto.builder()
                        .id(question.getId())
                        .quizId(question.getQuizId())
                        .questionText(question.getQuestionText())
                        .options(question.getOptions())
                        .correctAnswer(question.getCorrectAnswer())
                        .build())
                .toList();
    }

    // ================= ADMIN METHODS =================

    @Override
    public void addQuestion(CreateQuestionRequest request) {

        log.info("Adding question to quizId: {}", request.getQuizId());

        // 🔥 Basic validation
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            log.error("Options list is empty for quizId: {}", request.getQuizId());
            throw new RuntimeException("Options cannot be empty");
        }

        if (request.getCorrectAnswer() < 0 ||
                request.getCorrectAnswer() >= request.getOptions().size()) {
            log.error("Invalid correctAnswer index for quizId: {}", request.getQuizId());
            throw new RuntimeException("Invalid correct answer index");
        }

        Question q = new Question();

        q.setQuizId(request.getQuizId());
        q.setQuestionText(request.getQuestionText());
        q.setOptions(request.getOptions());
        q.setCorrectAnswer(request.getCorrectAnswer());

        questionRepository.save(q);

        log.info("Question added successfully with id: {}", q.getId());
    }

    @Override
    public void updateQuestion(String questionId, CreateQuestionRequest request) {

        log.info("Updating question with id: {}", questionId);

        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    log.error("Question not found with id: {}", questionId);
                    return new RuntimeException("Question not found");
                });

        // 🔥 Validation again (IMPORTANT)
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            log.error("Options list is empty while updating questionId: {}", questionId);
            throw new RuntimeException("Options cannot be empty");
        }

        if (request.getCorrectAnswer() < 0 ||
                request.getCorrectAnswer() >= request.getOptions().size()) {
            log.error("Invalid correctAnswer index for questionId: {}", questionId);
            throw new RuntimeException("Invalid correct answer index");
        }

        q.setQuestionText(request.getQuestionText());
        q.setOptions(request.getOptions());
        q.setCorrectAnswer(request.getCorrectAnswer());

        questionRepository.save(q);

        log.info("Question updated successfully with id: {}", questionId);
    }

    @Override
    public void deleteQuestion(String questionId) {

        log.warn("Deleting question with id: {}", questionId);

        if (!questionRepository.existsById(questionId)) {
            log.error("Question not found for deletion: {}", questionId);
            throw new RuntimeException("Question not found");
        }

        questionRepository.deleteById(questionId);

        log.info("Question deleted successfully with id: {}", questionId);
    }

    /**
        DOCUMENT -> QUESTION creation
    */
    @Override
    public List<GeneratedQuestionResponse> generateQuestions(
            GenerateQuestionRequest request) {

        log.info(
                "Generating {} {} questions from document {}",
                request.getNumberOfQuestions(),
                request.getDifficulty(),
                request.getDocumentId());

        // =========================================
        // 1. Validate request
        // =========================================

        if (request.getDocumentId() == null ||
                request.getDocumentId().isBlank()) {
            throw new IllegalArgumentException(
                    "Document ID cannot be null or empty.");
        }

        if (request.getNumberOfQuestions() <= 0) {
            throw new IllegalArgumentException(
                    "Number of questions must be greater than zero.");
        }

        if (request.getDifficulty() == null) {
            throw new IllegalArgumentException(
                    "Difficulty cannot be null.");
        }

        // =========================================
        // 2. Find document
        // =========================================

        Document document =
                documentRepository
                        .findById(request.getDocumentId())
                        .orElseThrow(() -> {
                            log.error(
                                    "Document not found: {}",
                                    request.getDocumentId()
                            );
                            return new RuntimeException(
                                    "Document not found: "
                                            + request.getDocumentId());
                        });

        // =========================================
        // 3. Get appropriate extractor
        // =========================================

        DocumentExtractor extractor = extractorFactory.getExtractor(document.getFileType());

        // =========================================
        // 4. Extract document content
        // =========================================

        ExtractedDocument extractedDocument;

        try {

            extractedDocument = extractor.extract(document.getFilePath());

            Files.writeString(Path.of("debug-extracted-document.txt"),
                    extractedDocument.getText());

        } catch (IOException e) {

            log.error("Failed to extract content from document: {}",
                    request.getDocumentId(),
                    e);

            throw new DocumentExtractionException("Failed to extract content from document.",
                    e);
        }

        // =========================================
        // 5. Get extracted text
        // =========================================

        String extractedText = extractedDocument.getText();

        // =========================================
        // 6. Validate extracted content
        // =========================================

        if (extractedText == null ||
                extractedText.isBlank()) {

            log.error("No usable content extracted from document: {}",
                    request.getDocumentId());

            throw new IllegalStateException("No usable content could be " +
                    "extracted from the document.");
        }

        // =========================================
        // 7. Log extraction statistics
        // =========================================

        log.info("Document extraction completed. " +
                        "Pages: {}, Native chars: {}, " +
                        "OCR chars: {}, Total chars: {}",
                extractedDocument.getPageCount(),
                extractedDocument.getTextCharacterCount(),
                extractedDocument.getOcrCharacterCount(),
                extractedText.length());

        // =========================================
        // 8. Chunk extracted document
        // =========================================

        List<DocumentChunk> chunks =
                textChunker.chunk(
                        document.getId(),
                        extractedText
                );

        log.info(
                "Document chunking completed. " +
                        "documentId: {}, chunks: {}",
                document.getId(),
                chunks.size()
        );

        // =========================================
        // 9. Store chunks in Vector Store
        // =========================================

        vectorStoreService.storeChunks(chunks);

        log.info(
                "Document chunks embedded and stored. " +
                        "documentId: {}, chunks: {}",
                document.getId(),
                chunks.size()
        );

        // =========================================
        // 10. Get quiz information
        // =========================================

        String quizId = document.getQuizId();

        if (quizId == null || quizId.isBlank()) {

            throw new IllegalStateException(
                    "Quiz ID is missing for document: "
                            + document.getId()
            );
        }

        /**
          Get the quiz from QUIZ-SERVICE.
         */

        QuizResponse quiz =
                quizClient.getQuizById(quizId);

        if (quiz == null ||
                quiz.getTitle() == null ||
                quiz.getTitle().isBlank()) {

            throw new IllegalStateException(
                    "Quiz title could not be found for quizId: "
                            + quizId
            );
        }

        String quizTitle = quiz.getTitle();

        log.info(
                "RAG retrieval query: '{}', documentId: {}",
                quizTitle,
                document.getId()
        );

        // =========================================
        // 11. Retrieve relevant chunks using RAG
        // =========================================

        List<org.springframework.ai.document.Document>
                retrievedDocuments =
                ragRetrievalService.retrieve(
                        document.getId(),
                        quizTitle,
                        5
                );

        log.info(
                "RAG retrieval completed. " +
                        "documentId: {}, query: {}, chunks retrieved: {}",
                document.getId(),
                quizTitle,
                retrievedDocuments.size()
        );

        // =========================================
        // 12. Build context from retrieved chunks
        // =========================================

        String retrievedContext =
                retrievedDocuments.stream()
                        .map(org.springframework.ai.document.Document
                                        ::getText)
                        .filter(text -> text != null && !text.isBlank())
                        .collect(java.util.stream.Collectors.joining("\n\n"));

        // =========================================
        // 13. Validate retrieved context
        // =========================================

        if (retrievedContext.isBlank()) {
            log.error("No relevant context found through RAG. " +
                            "documentId: {}, query: {}",
                    document.getId(),
                    quizTitle
            );
            throw new IllegalStateException("No relevant content found in the document "
                    + "for quiz: " + quizTitle);
        }

        log.info("RAG context created successfully. " +
                        "Context characters: {}",
                retrievedContext.length());

        // =========================================
        // 14. Build AI request
        // =========================================

        AiGenerateQuestionRequest aiRequest =
                AiGenerateQuestionRequest.builder()
                        .extractedText(retrievedContext)
                        .difficulty(request.getDifficulty())
                        .numberOfQuestions(request.getNumberOfQuestions())
                        .build();

        log.info("Sending RAG-retrieved context to AI-SERVICE. " +
                        "Quiz: {}, Questions: {}, Difficulty: {}",
                quizTitle,
                request.getNumberOfQuestions(),
                request.getDifficulty());

        // =========================================
        // 15. Call AI-SERVICE
        // =========================================

        return aiClient.generateQuestions(aiRequest);
    }

    @Override
    public List<AdminQuestionDto> approveGeneratedQuestions(
            ApproveGeneratedQuestionsRequest request) {

        log.info(
                "Received request to approve generated questions for documentId: {}",
                request.getDocumentId()
        );

        // 1. Validate document ID
        if (request.getDocumentId() == null ||
                request.getDocumentId().isBlank()) {

            throw new IllegalArgumentException(
                    "Document ID cannot be null or empty."
            );
        }

        // 2. Validate questions
        if (request.getQuestions() == null ||
                request.getQuestions().isEmpty()) {

            throw new IllegalArgumentException(
                    "No generated questions found for approval."
            );
        }

        // 3. Fetch document
        Document document = documentRepository
                .findById(request.getDocumentId())
                .orElseThrow(() -> {

                    log.error(
                            "Document not found: {}",
                            request.getDocumentId()
                    );

                    return new RuntimeException(
                            "Document not found: "
                                    + request.getDocumentId()
                    );
                });

        String quizId = document.getQuizId();

        log.info(
                "Approving {} generated questions for quizId: {}",
                request.getQuestions().size(),
                quizId
        );

        // 4. Convert and validate generated questions
        List<Question> questionsToSave =
                request.getQuestions()
                        .stream()
                        .map(q -> {

                            // Validate question text
                            if (q.getQuestionText() == null ||
                                    q.getQuestionText().isBlank()) {

                                throw new IllegalArgumentException(
                                        "Question text cannot be empty."
                                );
                            }

                            // Validate options
                            if (q.getOptions() == null ||
                                    q.getOptions().size() != 4) {

                                throw new IllegalArgumentException(
                                        "Each question must contain exactly 4 options."
                                );
                            }

                            // Validate correct answer
                            if (q.getCorrectAnswer() < 0 ||
                                    q.getCorrectAnswer() >=
                                            q.getOptions().size()) {

                                throw new IllegalArgumentException(
                                        "Invalid correct answer index for question: "
                                                + q.getQuestionText()
                                );
                            }

                            return Question.builder()
                                    .quizId(quizId)
                                    .questionText(q.getQuestionText())
                                    .options(q.getOptions())
                                    .correctAnswer(q.getCorrectAnswer())
                                    .build();
                        })
                        .toList();

        log.info(
                "Validation successful. Saving {} approved questions for quizId: {}",
                questionsToSave.size(),
                quizId
        );

        // 5. Save questions
        List<Question> savedQuestions =
                questionRepository.saveAll(questionsToSave);

        int numberOfNewQuestions = savedQuestions.size();

        log.info(
                "Updating total question count in QUIZ-SERVICE. " +
                        "quizId: {}, newQuestions: {}",
                quizId,
                numberOfNewQuestions
        );

        UpdateQuestionCountRequest updateRequest =
                UpdateQuestionCountRequest.builder()
                        .numberOfQuestions(numberOfNewQuestions)
                        .build();

        /**
            QUESTION-SERVICE calling QUIZ-SERVICE
         */

        QuizQuestionCountResponse response =
                quizClient.updateQuestionCount(
                        quizId,
                        updateRequest
                );

        log.info(
                "QUIZ-SERVICE updated successfully. quizId: {}, totalQuestions: {}",
                response.getQuizId(),
                response.getTotalQuestions()
        );

        log.info(
                "{} questions saved successfully for quizId: {}",
                savedQuestions.size(),
                quizId
        );

        // 6. Update document status
        document.setStatus(GenerationStatus.APPROVED);

        documentRepository.save(document);

        log.info(
                "Document {} marked as APPROVED.",
                document.getId()
        );

        // 7. Return final approved questions
        return savedQuestions.stream()
                .map(question ->
                        AdminQuestionDto.builder()
                                .id(question.getId())
                                .quizId(question.getQuizId())
                                .questionText(question.getQuestionText())
                                .options(question.getOptions())
                                .correctAnswer(question.getCorrectAnswer())
                                .build()
                )
                .toList();
    }
}