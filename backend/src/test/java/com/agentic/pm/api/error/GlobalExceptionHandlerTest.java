package com.agentic.pm.api.error;

import com.agentic.pm.exception.ProjectNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(new IllegalArgumentException("bad"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("bad", response.getBody().get("message"));
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(new ProjectNotFoundException("p1"));
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void handleServerError_returns500() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getRequestURI()).thenReturn("/api/projects/p1/generate-questions");

        ResponseEntity<Map<String, String>> response = handler.handleServerError(new RuntimeException("boom"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().get("message"));
    }
}

