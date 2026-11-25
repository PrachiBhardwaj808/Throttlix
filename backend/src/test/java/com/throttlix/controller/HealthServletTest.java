package com.throttlix.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private HealthServlet servlet;
    private StringWriter responseBodyWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        servlet = new HealthServlet();
        responseBodyWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBodyWriter));
    }

    @Test
    void shouldReturn200AndHealthyStatusOnGet() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/health");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        String json = responseBodyWriter.toString();
        assertTrue(json.contains("\"status\": \"UP\""));
        assertTrue(json.contains("\"application\": \"Throttlix\""));
        assertTrue(json.contains("Backend is running successfully"));
    }
}
