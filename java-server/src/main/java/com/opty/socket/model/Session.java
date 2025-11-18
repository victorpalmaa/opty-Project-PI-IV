/**
 * Session model.
 */

package com.opty.socket.model;


/**
 * IMPORTS
 */
import java.time.Instant;


/**
 * CODE
 */

/**
 * Represents a client-supervisor chat session.
 */
public record Session(
        String sessionId,
        String clientConnectionId,
        String supervisorConnectionId,
        Instant createdAt,
        Instant lastActivityAt
) {

    /**
     * Creates a new unpaired session (client only).
     */
    public Session(String sessionId, String clientConnectionId) {
        this(sessionId, clientConnectionId, null, Instant.now(), Instant.now());
    }

    /**
     * Creates a copy with paired supervisor.
     */
    public Session withSupervisor(String supervisorConnectionId) {
        return new Session(
                sessionId,
                clientConnectionId,
                supervisorConnectionId,
                createdAt,
                Instant.now()
        );
    }

    /**
     * Creates a copy with updated last activity time.
     */
    public Session withLastActivity() {
        return new Session(
                sessionId,
                clientConnectionId,
                supervisorConnectionId,
                createdAt,
                Instant.now()
        );
    }

    /**
     * Checks if this session is paired (has both client and supervisor).
     */
    public boolean isPaired() {
        return supervisorConnectionId != null && !supervisorConnectionId.isBlank();
    }

    /**
     * Checks if this session is expired based on timeout.
     *
     * @param timeoutMinutes session timeout in minutes
     * @return true if session has expired
     */
    public boolean isExpired(int timeoutMinutes) {
        Instant expirationTime = lastActivityAt.plusSeconds(timeoutMinutes * 60L);
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * Gets the connection ID of the other party in this session.
     *
     * @param connectionId the connection ID of one party
     * @return the connection ID of the other party, or null if not paired or invalid
     */
    public String getOtherPartyConnectionId(String connectionId) {

        // Not paired: return null
        if (connectionId == null) {
            return null;
        }

        // Passed connectionId is from client: return supervisor id
        if (connectionId.equals(clientConnectionId)) {
            return supervisorConnectionId;

        // Passed connectionId is from supervisor: return client id
        } else if (connectionId.equals(supervisorConnectionId)) {
            return clientConnectionId;
        }

        // Invalid connectionId: return null
        return null;
    }
}
