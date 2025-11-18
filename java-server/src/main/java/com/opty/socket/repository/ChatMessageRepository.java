package com.opty.socket.repository;

import com.opty.socket.model.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data para ChatMessageDocument.
 * Fornece métodos CRUD para a coleção 'chat_messages'.
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    // Spring Data cria a query automaticamente pelo nome do método
    List<ChatMessageDocument> findBySessionId(String sessionId);
}