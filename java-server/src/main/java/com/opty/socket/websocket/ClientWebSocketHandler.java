/**
 * Client WebSocket handler.
 */

package com.opty.socket.websocket;


/**
 * IMPORTS
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opty.socket.model.ConnectionInfo;
import com.opty.socket.model.Message;
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


/**
 * CODE
 */

/**
 * Handles WebSocket connections from clients.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler extends TextWebSocketHandler {

    // --- ATTRIBUTES ---
    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final SupervisorQueueService supervisorQueueService;
    private final ObjectMapper objectMapper;


    /**
     * Called when a new client WebSocket connection is established.
     *
     * Creates a new session and sends the sessionId back to the client.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectionId = session.getId();
        log.info("Client connecting: connectionId={}, remoteAddress={}",
                connectionId, session.getRemoteAddress());

        try {
            // Check max connections limit
            if (sessionManager.isMaxConnectionsReached()) {
                log.warn("Max connections reached, rejecting client: connectionId={}", connectionId);
                Message error = Message.error(null, "Server at maximum capacity");
                messageRouter.sendMessage(session, error);
                session.close(CloseStatus.SERVICE_OVERLOAD);
                return;
            }

            // Create session for client
            Session newSession = sessionManager.createSession(connectionId);

            // Register connection
            ConnectionInfo connectionInfo = new ConnectionInfo(
                    connectionId,
                    session,
                    null,  // WebSocket connection (no Parceiro)
                    "CLIENT",
                    newSession.sessionId()
            );
            sessionManager.registerConnection(connectionInfo);

            // Send CONNECT response with sessionId
            Message connectResponse = Message.connectResponse(newSession.sessionId());
            messageRouter.sendMessage(session, connectResponse);

            // Broadcast queue update to all supervisors (new session available)
            supervisorQueueService.broadcastQueueUpdate();

            log.info("Client connected successfully: connectionId={}, sessionId={}",
                    connectionId, newSession.sessionId());

        } catch (Exception e) {
            log.error("Error establishing client connection: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }


    /**
     * Called when a text message is received from the client.
     *
     * Parses the JSON message and routes it to the paired supervisor.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String connectionId = session.getId();
        String payload = textMessage.getPayload();

        log.debug("Message received from client: connectionId={}, length={}",
                connectionId, payload.length());

        try {
            // Parse message
            Message message = objectMapper.readValue(payload, Message.class);

            // Validate message type
            if (message.type() == null) {
                log.warn("Invalid message type from client: connectionId={}", connectionId);
                messageRouter.sendErrorToConnection(connectionId, "Invalid message type");
                return;
            }

            // Handle different message types
            switch (message.type()) {
                case MESSAGE -> {
                    // Route message to paired supervisor
                    boolean routed = messageRouter.routeMessage(connectionId, message);
                    if (!routed) {
                        log.debug("Failed to route message from client: connectionId={}", connectionId);
                    }
                }
                case DISCONNECT -> {
                    // Graceful disconnect requested
                    log.info("Client requested disconnect: connectionId={}", connectionId);
                    session.close(CloseStatus.NORMAL);
                }
                default -> {
                    log.warn("Unexpected message type from client: connectionId={}, type={}",
                            connectionId, message.type());
                    messageRouter.sendErrorToConnection(connectionId,
                            "Unexpected message type: " + message.type());
                }
            }

        } catch (Exception e) {
            log.error("Error processing client message: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
            messageRouter.sendErrorToConnection(connectionId,
                    "Failed to process message: " + e.getMessage());
        }
    }


    /**
     * Called when the client WebSocket connection is closed.
     *
     * Notifies the paired supervisor and cleans up resources.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String connectionId = session.getId();
        log.info("Client disconnecting: connectionId={}, status={}", connectionId, status);

        try {
            // Notify paired supervisor
            messageRouter.notifyDisconnect(connectionId);

            // Remove session
            sessionManager.removeSessionByConnectionId(connectionId);

            // Remove connection
            sessionManager.removeConnection(connectionId);

            // Broadcast queue update to supervisors (session removed)
            supervisorQueueService.broadcastQueueUpdate();

            log.info("Client disconnected and cleaned up: connectionId={}", connectionId);

        } catch (Exception e) {
            log.error("Error during client disconnect cleanup: connectionId={}, error={}",
                    connectionId, e.getMessage(), e);
        }
    }

    
    /**
     * Called when a transport error occurs.
     *
     * Logs the error and attempts to notify the client.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connectionId = session.getId();
        log.error("Transport error for client: connectionId={}, error={}",
                connectionId, exception.getMessage(), exception);

        try {
            if (session.isOpen()) {
                messageRouter.sendErrorToConnection(connectionId,
                        "Transport error: " + exception.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send error message to client: connectionId={}", connectionId);
        }
    }
}
