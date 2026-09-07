package com.avradeep.QuestionService.service;

import com.avradeep.QuestionService.client.AIClient;
import com.avradeep.QuestionService.client.QuizClient;
import com.avradeep.QuestionService.dto.*;
import com.avradeep.QuestionService.entity.Document;
import com.avradeep.QuestionService.entity.DocumentStatus;
import com.avradeep.QuestionService.entity.Question;
import com.avradeep.QuestionService.rag.retrieval.RagRetrievalService;
import com.avradeep.QuestionService.repository.DocumentRepository;
import com.avradeep.QuestionService.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final DocumentRepository documentRepository;
    private final AIClient aiClient;
    private final QuizClient quizClient;
    private final RagRetrievalService ragRetrievalService;

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
                request.getDocumentId()
        );

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
                                    request.getDocumentId());

                            return new RuntimeException(
                                    "Document not found: "
                                            + request.getDocumentId());
                        });

        // =========================================
        // 3. Check document processing status
        // =========================================

        if (document.getStatus() == DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Document is still being processed. Please try again later.");
        }

        if (document.getStatus() == DocumentStatus.FAILED) {
            throw new IllegalStateException(
                    "Document processing failed. Please process the document again.");
        }

        if (document.getStatus() != DocumentStatus.READY) {
            throw new IllegalStateException(
                    "Document is not ready for question generation. Current status: "
                            + document.getStatus());
        }

        log.info(
                "Document is ready for question generation. " +
                        "documentId: {}",
                document.getId());

        // =========================================
        // 4. Get quiz information
        // =========================================

        String quizId = request.getQuizId();

        if (quizId == null ||
                quizId.isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz ID cannot be null or empty.");
        }

        // Verify that the document belongs to this quiz

        List<String> documentIds =
                quizClient.getDocumentIdsByQuizId(quizId);

        if (!documentIds.contains(document.getId())) {

            throw new IllegalArgumentException(
                    "Document " + document.getId() +
                            " does not belong to quiz " + quizId);
        }

        QuizResponse quiz =
                quizClient.getQuizById(quizId);

        if (quiz == null ||
                quiz.getTitle() == null ||
                quiz.getTitle().isBlank()) {

            throw new IllegalStateException(
                    "Quiz title could not be found for quizId: "
                            + quizId);
        }

        String quizTitle =
                quiz.getTitle();

        // =========================================
        // 5. Retrieve relevant chunks using RAG
        // =========================================

        List<org.springframework.ai.document.Document>
                retrievedDocuments =
                ragRetrievalService.retrieve(
                        document.getId(),
                        quizTitle,
                        6);

        log.info(
                "RAG retrieval completed. " +
                        "documentId: {}, query: {}, chunks retrieved: {}",
                document.getId(),
                quizTitle,
                retrievedDocuments.size());

        // =========================================
        // 6. Build RAG context
        // =========================================

        String retrievedContext =
                retrievedDocuments.stream()
                        .map(
                                org.springframework.ai.document.Document
                                        ::getText)
                        .filter(
                                text -> text != null &&
                                        !text.isBlank())
                        .collect(
                                java.util.stream.Collectors.joining(
                                        "\n\n"));

        if (retrievedContext.isBlank()) {

            log.error(
                    "No relevant context found through RAG. " +
                            "documentId: {}, query: {}",
                    document.getId(),
                    quizTitle);

            throw new IllegalStateException(
                    "No relevant content found in the document " +
                            "for quiz: " +
                            quizTitle);
        }

        log.info(
                "RAG context created successfully. " +
                        "Context characters: {}",
                retrievedContext.length());

        // =========================================
        // 7. Build AI request
        // =========================================

        AiGenerateQuestionRequest aiRequest =
                AiGenerateQuestionRequest.builder()
                        .extractedText(retrievedContext)
                        .difficulty(
                                request.getDifficulty())
                        .numberOfQuestions(
                                request.getNumberOfQuestions())
                        .build();

        log.info(
                "Sending RAG-retrieved context to AI-SERVICE. " +
                        "Quiz: {}, Questions: {}, Difficulty: {}",
                quizTitle,
                request.getNumberOfQuestions(),
                request.getDifficulty());

        // =========================================
        // 8. Call AI-SERVICE
        // =========================================

        return aiClient.generateQuestions(aiRequest);
    }

    @Override
    public List<AdminQuestionDto> approveGeneratedQuestions(
            ApproveGeneratedQuestionsRequest request) {

        log.info(
                "Received request to approve generated questions. " +
                        "quizId: {}, documentId: {}",
                request.getQuizId(),
                request.getDocumentId());

        // =========================================
        // 1. Validate quiz ID
        // =========================================

        if (request.getQuizId() == null ||
                request.getQuizId().isBlank()) {

            throw new IllegalArgumentException(
                    "Quiz ID cannot be null or empty.");
        }

        // =========================================
        // 2. Validate document ID
        // =========================================

        if (request.getDocumentId() == null ||
                request.getDocumentId().isBlank()) {

            throw new IllegalArgumentException(
                    "Document ID cannot be null or empty.");
        }

        // =========================================
        // 3. Validate questions
        // =========================================

        if (request.getQuestions() == null ||
                request.getQuestions().isEmpty()) {

            throw new IllegalArgumentException(
                    "No generated questions found for approval.");
        }

        String quizId = request.getQuizId();
        String documentId = request.getDocumentId();

        // =========================================
        // 4. Fetch document
        // =========================================

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> {
                    log.error(
                            "Document not found: {}",
                            documentId);
                    return new RuntimeException(
                            "Document not found: " +
                                    documentId);
                });


        // =========================================
        // 5. Convert and validate generated questions
        // =========================================

        List<Question> questionsToSave =
                request.getQuestions()
                        .stream()
                        .map(q -> {

                            // Validate question text
                            if (q.getQuestionText() == null ||
                                    q.getQuestionText().isBlank()) {

                                throw new IllegalArgumentException(
                                        "Question text cannot be empty.");
                            }
                            // Validate options
                            if (q.getOptions() == null ||
                                    q.getOptions().size() != 4) {

                                throw new IllegalArgumentException(
                                        "Each question must contain exactly 4 options.");
                            }
                            // Validate correct answer
                            if (q.getCorrectAnswer() < 0 ||
                                    q.getCorrectAnswer() >=
                                            q.getOptions().size()) {

                                throw new IllegalArgumentException(
                                        "Invalid correct answer index for question: "
                                                + q.getQuestionText());
                            }

                            return Question.builder()
                                    .quizId(quizId)
                                    .questionText(
                                            q.getQuestionText())
                                    .options(
                                            q.getOptions())
                                    .correctAnswer(
                                            q.getCorrectAnswer())
                                    .build();
                        })
                        .toList();

        log.info(
                "Validation successful. Saving {} approved questions for quizId: {}",
                questionsToSave.size(),
                quizId);

        // =========================================
        // 6. Save questions
        // =========================================

        List<Question> savedQuestions =
                questionRepository.saveAll(
                        questionsToSave);

        int numberOfNewQuestions =
                savedQuestions.size();

        // =========================================
        // 7. Update quiz question count
        // =========================================

        log.info(
                "Updating total question count in QUIZ-SERVICE. " +
                        "quizId: {}, newQuestions: {}",
                quizId,
                numberOfNewQuestions);

        UpdateQuestionCountRequest updateRequest =
                UpdateQuestionCountRequest.builder()
                        .numberOfQuestions(
                                numberOfNewQuestions)
                        .build();

        QuizQuestionCountResponse response =
                quizClient.updateQuestionCount(
                        quizId,
                        updateRequest);

        log.info(
                "QUIZ-SERVICE updated successfully. " +
                        "quizId: {}, totalQuestions: {}",
                response.getQuizId(),
                response.getTotalQuestions());

        log.info(
                "{} questions saved successfully for quizId: {}",
                savedQuestions.size(),
                quizId);

        // =========================================
        // 8. Return approved questions
        // =========================================

        return savedQuestions.stream()
                .map(question ->
                        AdminQuestionDto.builder()
                                .id(question.getId())
                                .quizId(
                                        question.getQuizId())
                                .questionText(
                                        question.getQuestionText())
                                .options(
                                        question.getOptions())
                                .correctAnswer(
                                        question.getCorrectAnswer())
                                .build()
                )
                .toList();
    }
}