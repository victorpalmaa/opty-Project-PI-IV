/**
 * Supervisor WebSocket handler.
 */

package com.opty.socket.websocket;


/**
 * IMPORTS
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opty.socket.model.ConnectionInfo;
import com.opty.socket.model.Message;
import com.opty.socket.model.MessageType;
import com.opty.socket.model.Session;
import com.opty.socket.service.MessageRouter;
import com.opty.socket.service.SessionManager;
import com.opty.socket.service.SupervisorQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;


/**
 * CODE
 */

/**
 * Handles WebSocket connections from supervisors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisorWebSocketHandler extends TextWebSocketHandler {

    // --- ATTRIBUTES ---
    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final SupervisorQueueService supervisorQueueService;
    private final ObjectMapper objectMapper;

    /**
     * Called when a new supervisor WebSocket connection is established.
     *
     * Registers the connection and waits for the supervisor to send a CONNECT message
     * with the sessionId to join an existing client session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectionId = session.getId();
        log.info("Supervisor connecting: connectionId={}, remoteAddress={}",
                connectionId, session.getRemoteAddress());

        try {
            // Check max connections limit
            if (sessionManager.isMaxConnectionsReached()) {
                log.warn("Max connections reached, rejecting supervisor: connectionId={}", connectionId);
                Message error = Message.error(null, "Server at maximum capacity");
                messageRouter.sendMessage(session, error);
                session.close(CloseStatus.SERVICE_OVERLOAD);
                return;
            }

            // Register connection (without sessionId yet)
            ConnectionInfo connectionInfo = new ConnectionInfo(
                    connectionId,
                    session,
                    null,  // WebSocket connection (no Parceiro)
                    "SUPERVISOR",
                    null
            );
            sessionManager.registerConnection(connectionInfo);

            // Register as queue listener (will receive session updates)
            supervisorQueueService.registerQueueListener(connectionId);

            // Send acknowledgment and initial queue state
            Message ackMessage = new Message(
                    null,
                    "SERVER",
                    MessageType.CONNECT,
                    Map.of("message", "Connected, listening to session queue")
            );
            messageRouter.sendMessage(session, ackMessage);

            // Send initial queue state
            supervisorQueueService.sendInitialQueue(session);

            log.info("Supervisor connection registered as queue listener: connectionId={}", connectionId);

        } catch (Exception e) {
            log.error("Error establishing supervisor connection: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }


    /**
     * Called when a text message is received from the supervisor.
     *
     * Handles CONNECT message to join session, then routes subsequent messages to paired client.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String connectionId = session.getId();
        String payload = textMessage.getPayload();

        log.debug("Message received from supervisor: connectionId={}, length={}",
                connectionId, payload.length());

        try {
            // Parse message
            Message message = objectMapper.readValue(payload, Message.class);

            // Validate message type
            if (message.type() == null) {
                log.warn("Invalid message type from supervisor: connectionId={}", connectionId);
                messageRouter.sendErrorToConnection(connectionId, "Invalid message type");
                return;
            }

            // Handle different message types
            switch (message.type()) {
                case CONNECT -> {
                    // Supervisor is trying to join a session
                    handleSupervisorJoinSession(connectionId, message, session);
                }
                case MESSAGE -> {
                    // Route message to paired client
                    boolean routed = messageRouter.routeMessage(connectionId, message);
                    if (!routed) {
                        log.debug("Failed to route message from supervisor: connectionId={}", connectionId);
                    }
                }
                case DISCONNECT -> {
                    // Graceful disconnect requested
                    log.info("Supervisor requested disconnect: connectionId={}", connectionId);
                    session.close(CloseStatus.NORMAL);
                }
                default -> {
                    log.warn("Unexpected message type from supervisor: connectionId={}, type={}",
                            connectionId, message.type());
                    messageRouter.sendErrorToConnection(connectionId,
                            "Unexpected message type: " + message.type());
                }
            }

        } catch (Exception e) {
            log.error("Error processing supervisor message: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
            messageRouter.sendErrorToConnection(connectionId,
                    "Failed to process message: " + e.getMessage());
        }
    }


    /**
     * Handles supervisor joining an existing client session.
     */
    private void handleSupervisorJoinSession(String connectionId, Message message, WebSocketSession session) {
        String sessionId = message.sessionId();

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Supervisor CONNECT without sessionId: connectionId={}", connectionId);
            messageRouter.sendErrorToConnection(connectionId, "sessionId is required to join");
            return;
        }

        // Check if session exists
        Session existingSession = sessionManager.getSession(sessionId).orElse(null);
        if (existingSession == null) {
            log.warn("Supervisor trying to join non-existent session: connectionId={}, sessionId={}",
                    connectionId, sessionId);
            Message error = Message.error(sessionId, "Session not found");
            messageRouter.sendMessage(session, error);
            return;
        }

        // Pair supervisor with session
        Session pairedSession = sessionManager.pairSupervisor(sessionId, connectionId).orElse(null);
        if (pairedSession == null || !pairedSession.isPaired()) {
            log.warn("Failed to pair supervisor: connectionId={}, sessionId={}", connectionId, sessionId);
            Message error = Message.error(sessionId, "Failed to join session (may already be paired)");
            messageRouter.sendMessage(session, error);
            return;
        }

        // Update connection with sessionId
        sessionManager.updateSessionId(connectionId, sessionId);

        // Remove from queue listeners (supervisor is now paired with a session)
        supervisorQueueService.removeQueueListener(connectionId);

        // Send success response to supervisor
        Message successResponse = new Message(
                sessionId,
                "SERVER",
                MessageType.CONNECT,
                Map.of("message", "Successfully joined session", "paired", true)
        );
        messageRouter.sendMessage(session, successResponse);

        // Notify client that supervisor has joined
        sessionManager.getConnection(existingSession.clientConnectionId()).ifPresent(clientConn -> {
            Message notifyClient = new Message(
                    sessionId,
                    "SERVER",
                    MessageType.CONNECT,
                    Map.of("message", "Supervisor has joined the session")
            );
            messageRouter.sendMessage(clientConn.webSocketSession(), notifyClient);
        });

        // Broadcast queue update to other supervisors (session is no longer available)
        supervisorQueueService.broadcastQueueUpdate();

        log.info("Supervisor joined session successfully: connectionId={}, sessionId={}", connectionId, sessionId);
    }


    /**
     * Called when the supervisor WebSocket connection is closed.
     *
     * Notifies the paired client and cleans up resources.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connectionId = session.getId();
        log.info("Supervisor disconnecting: connectionId={}, status={}", connectionId, status);

        try {
            // Notify paired client
            messageRouter.notifyDisconnect(connectionId);

            // Get session before removing connection
            Session existingSession = sessionManager.getSessionByConnectionId(connectionId).orElse(null);

            // Remove connection from SessionManager
            sessionManager.removeConnection(connectionId);

            // Unpair supervisor from session (but keep session active for client)
            if (existingSession != null && existingSession.isPaired()) {
                sessionManager.unpairSupervisor(existingSession.sessionId());
                log.info("Supervisor unpaired from session: sessionId={}, session remains active for client",
                        existingSession.sessionId());

                // Broadcast queue update (session is now available again)
                supervisorQueueService.broadcastQueueUpdate();
            }

            // Remove from queue listeners (if was listening)
            supervisorQueueService.removeQueueListener(connectionId);

            log.info("Supervisor disconnected: connectionId={}", connectionId);

        } catch (Exception e) {
            log.error("Error during supervisor disconnect cleanup: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
        }
    }

    
    /**
     * Called when a transport error occurs.
     *
     * Logs the error and attempts to notify the supervisor.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connectionId = session.getId();
        log.error("Transport error for supervisor: connectionId={}, error={}",
                connectionId, exception.getMessage(), exception);

        try {
            if (session.isOpen()) {
                messageRouter.sendErrorToConnection(connectionId,
                        "Transport error: " + exception.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send error message to supervisor: connectionId={}", connectionId);
        }
    }
}
