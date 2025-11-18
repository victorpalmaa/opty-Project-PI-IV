package com.opty.socket.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Representa uma mensagem de chat salva no MongoDB.
 */
@Data
@NoArgsConstructor
@Document(collection = "messages") // Nome da coleção no MongoDB
public class ChatMessageDocument {

    @Id
    private String id; // ID único gerado pelo MongoDB

    private String channelType;
    private String sessionId;
    private String from; // "CLIENT" ou "SUPERVISOR"
    private MessageType type;
    private Map<String, Object> payload;
    private Instant timestamp;

    /**
     * Construtor auxiliar para converter do nosso 'record' Message
     * para este Documento que será salvo.
     */
    public ChatMessageDocument(Message message, String channelType) {
        this.channelType = channelType;
        this.sessionId = message.sessionId();
        this.from = message.from();
        this.type = message.type();
        this.payload = message.payload();
        this.timestamp = message.timestamp();
    }
}