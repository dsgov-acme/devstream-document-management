package io.nuvalence.ds4g.documentmanagement.service.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class GlobalErrorHandlerTest {

    @Test
    void testErrorResponseInitialization() {
        // Arrange
        String message = "Error occurred.";
        List<String> expectedMessages = Collections.singletonList(message);

        // Act
        GlobalErrorHandler.ErrorResponse errorResponse =
                new GlobalErrorHandler.ErrorResponse(message);

        // Assert
        Assertions.assertEquals(expectedMessages, errorResponse.getMessages());
    }
}
