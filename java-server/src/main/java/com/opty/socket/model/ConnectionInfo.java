/**
 * Connection metadata.
 */

package com.opty.socket.model;


/**
 * IMPORTS
 */
import com.opty.socket.tradicional.Parceiro;
import org.springframework.web.socket.WebSocketSession;


/**
 * CODE
 */

/**
 * Tracks active connections.
 */
public record ConnectionInfo(

        String connectionId,
        WebSocketSession webSocketSession,
        Parceiro parceiro,
        String connectionType,
        String sessionId
) {

    /**
     * Creates a copy with updated session ID.
     */
    public ConnectionInfo withSessionId(String newSessionId) {
        return new ConnectionInfo(
                connectionId,
                webSocketSession,
                parceiro,
                connectionType,
                newSessionId
        );
    }

    /**
     * Checks if this is a WebSocket connection.
     */
    public boolean isWebSocket() {
        return webSocketSession != null;
    }

    /**
     * Checks if this is a traditional Socket connection.
     */
    public boolean isTraditionalSocket() {
        return parceiro != null;
    }

    /**
     * Checks if this connection is a client.
     */
    public boolean isClient() {
        return "CLIENT".equals(connectionType);
    }

    /**
     * Checks if this connection is a supervisor.
     */
    public boolean isSupervisor() {
        return "SUPERVISOR".equals(connectionType);
    }

    /**
     * Checks if this connection is paired (has a session ID).
     */
    public boolean isPaired() {
        return sessionId != null && !sessionId.isBlank();
    }
}
