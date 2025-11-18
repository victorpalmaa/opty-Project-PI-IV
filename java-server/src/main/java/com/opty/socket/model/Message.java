/**
 * WebSocket message model.
 */

package com.opty.socket.model;


/**
 * IMPORTS
 */
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;


/**
 * CODE
 */

/**
 * Messages exchanged via WebSocket between clients, supervisors, and server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Message(
        @Size(max = 100, message = "Session ID must not exceed 100 characters")
        String sessionId,

        @Size(max = 20, message = "From field must not exceed 20 characters")
        String from,

        @NotNull(message = "Message type is required")
        MessageType type,

        Map<String, Object> payload,
        Instant timestamp
) {
    /**
     * Creates a new message with current timestamp.
     */
    public Message(String sessionId, String from, MessageType type, Map<String, Object> payload) {
        this(sessionId, from, type, payload, Instant.now());
    }

    /**
     * Creates a message without payload (for DISCONNECT).
     */
    public Message(String sessionId, String from, MessageType type) {
        this(sessionId, from, type, null, Instant.now());
    }

    /**
     * Validates if this message has a valid session ID.
     */
    public boolean hasValidSessionId() {
        return sessionId != null && !sessionId.isBlank();
    }

    /**
     * Validates if this message has a valid originator.
     */
    public boolean hasValidFrom() {
        return from != null && (from.equals("CLIENT") || from.equals("SUPERVISOR"));
    }

    /**
     * Creates an error message.
     */
    public static Message error(String sessionId, String errorMessage) {
        return new Message(
                sessionId,
                "SERVER",
                MessageType.ERROR,
                Map.of("error", errorMessage),
                Instant.now()
        );
    }

    /**
     * Creates a CONNECT response message for clients.
     */
    public static Message connectResponse(String sessionId) {
        return new Message(
                sessionId,
                "SERVER",
                MessageType.CONNECT,
                Map.of("message", "Connected successfully", "sessionId", sessionId),
                Instant.now()
        );
    }
}
