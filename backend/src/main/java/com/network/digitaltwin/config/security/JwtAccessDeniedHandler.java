package com.network.digitaltwin.config.security;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.digitaltwin.dto.response.ApiResponse;

/**
 * JWT access denied handler for handling forbidden requests.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    @Override
    public void handle(javax.servlet.http.HttpServletRequest request, 
                      javax.servlet.http.HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {

        logger.error("Access denied error: {}", accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN);

        ApiResponse apiResponse = new ApiResponse(
            false,
            "Access denied: " + accessDeniedException.getMessage(),
            null
        );

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
