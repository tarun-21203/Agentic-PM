package com.agentic.pm.api.security;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserIdResolver {

    private static final Logger log = LoggerFactory.getLogger(UserIdResolver.class);
    
    public String resolve() {
        log.info("resolving user id");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.error("Missing authentication");
            throw new IllegalStateException("Missing authentication");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            log.error("Unsupported authentication principal");
            throw new IllegalStateException("Unsupported authentication principal");
        }

        String subject = jwt.getSubject(); // Cognito JWT 'sub'
        if (subject == null || subject.isBlank()) {
            log.error("JWT subject (sub) is missing");
            throw new IllegalStateException("JWT subject (sub) is missing");
        }

        log.info("resolved user id: {}", subject);
        return subject;
    }
}

