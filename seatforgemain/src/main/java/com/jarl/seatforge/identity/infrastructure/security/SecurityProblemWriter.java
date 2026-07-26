package com.jarl.seatforge.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class SecurityProblemWriter {

    private SecurityProblemWriter() {
    }

    static void unauthorized(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, 401, "Unauthorized", "Bearer token is missing or invalid.", "UNAUTHORIZED");
    }

    static void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, "Forbidden", "The actor is not allowed to perform this operation.", "FORBIDDEN");
    }

    private static void write(
            HttpServletResponse response,
            int status,
            String title,
            String detail,
            String code
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        response.getWriter().printf(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"code\":\"%s\"}",
                title,
                status,
                detail,
                code
        );
    }
}
