package com.agentic.pm.api.controller;

import com.agentic.pm.api.security.UserIdResolver;
import com.agentic.pm.dto.question.AnswerRequestDto;
import com.agentic.pm.dto.question.QuestionDto;
import com.agentic.pm.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QuestionsController {

    private static final Logger log = LoggerFactory.getLogger(QuestionsController.class);

    private final QuestionService questionService;
    private final UserIdResolver userIdResolver;

    public QuestionsController(QuestionService questionService, UserIdResolver userIdResolver) {
        this.questionService = questionService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/projects/{projectId}/generate-questions")
    public ResponseEntity<List<QuestionDto>> generateQuestions(
            @PathVariable String projectId
    ) {
        log.info("generate-questions request received. projectId={}", projectId);
        String userId = userIdResolver.resolve();
        log.info("generate-questions request received. projectId={} userId={}", projectId, userId);
        try {
            List<QuestionDto> out = questionService.generateQuestions(projectId, userId).stream()
                    .map(QuestionDto::from)
                    .toList();
            log.info("generate-questions succeeded. projectId={} questionCount={}", projectId, out != null ? out.size() : 0);
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("generate-questions failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @GetMapping("/projects/{projectId}/questions")
    public ResponseEntity<List<QuestionDto>> listQuestions(@PathVariable String projectId) {
        String userId = userIdResolver.resolve();
        log.info("list-questions request received. projectId={} userId={}", projectId, userId);
        try {
            List<QuestionDto> out = questionService.listQuestions(projectId, userId).stream()
                    .map(QuestionDto::from)
                    .toList();
            log.info("list-questions succeeded. projectId={} questionCount={}", projectId, out.size());
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("list-questions failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @PutMapping("/projects/{projectId}/questions/{questionId}/answer")
    public ResponseEntity<Map<String, String>> putAnswer(
            @PathVariable String projectId,
            @PathVariable String questionId,
            @RequestBody AnswerRequestDto body
    ) {
        String userId = userIdResolver.resolve();
        log.info("put-answer request received. projectId={} questionId={} userId={}", projectId, questionId, userId);
        try {
            questionService.saveAnswer(projectId, questionId, userId, body != null ? body.getAnswer() : null);
            log.info("put-answer succeeded. projectId={} questionId={}", projectId, questionId);
            return ResponseEntity.ok(Map.of("status", "saved"));
        } catch (RuntimeException e) {
            log.error("put-answer failed. projectId={} questionId={} userId={}", projectId, questionId, userId, e);
            throw e;
        }
    }
}

