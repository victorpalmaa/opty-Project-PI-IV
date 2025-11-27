package com.opty.socket.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes INTRACLASSE para a classe Session
 * Abordagem: ESTADOS
 *
 * Estados testados:
 * - UNPAIRED: Sessão criada sem supervisor
 * - PAIRED: Sessão com supervisor pareado
 * - EXPIRED: Sessão que expirou por timeout
 */
@DisplayName("Testes INTRACLASSE - Session (Abordagem: ESTADOS)")
class SessionTest {

    @Test
    @DisplayName("Estado UNPAIRED: Sessão criada sem supervisor deve estar não pareada")
    void testUnpairedState_SessionWithoutSupervisor_ShouldBeUnpaired() {
        // Arrange
        String sessionId = "session-123";
        String clientConnectionId = "client-456";
        Instant now = Instant.now();

        // Act
        Session session = new Session(sessionId, clientConnectionId, null, now, now);

        // Assert
        assertFalse(session.isPaired(), "Sessão sem supervisor deve estar não pareada");
        assertNull(session.supervisorConnectionId(), "Supervisor deve ser null");
        assertEquals(sessionId, session.sessionId());
        assertEquals(clientConnectionId, session.clientConnectionId());
        assertEquals(now, session.createdAt());
        assertEquals(now, session.lastActivityAt());
    }

    @Test
    @DisplayName("Estado PAIRED: Sessão com supervisor deve estar pareada")
    void testPairedState_SessionWithSupervisor_ShouldBePaired() {
        // Arrange
        String sessionId = "session-789";
        String clientConnectionId = "client-001";
        String supervisorConnectionId = "supervisor-002";
        Instant now = Instant.now();

        // Act
        Session session = new Session(sessionId, clientConnectionId, supervisorConnectionId, now, now);

        // Assert
        assertTrue(session.isPaired(), "Sessão com supervisor deve estar pareada");
        assertNotNull(session.supervisorConnectionId(), "Supervisor não deve ser null");
        assertEquals(supervisorConnectionId, session.supervisorConnectionId());
    }

    @Test
    @DisplayName("Transição UNPAIRED -> PAIRED: Adicionar supervisor deve parear sessão")
    void testStateTransition_UnpairedToPaired_ShouldPairSession() {
        // Arrange - Estado inicial UNPAIRED
        String sessionId = "session-transition-1";
        String clientConnectionId = "client-100";
        Instant now = Instant.now();
        Session unpairedSession = new Session(sessionId, clientConnectionId, null, now, now);

        assertFalse(unpairedSession.isPaired(), "Estado inicial deve ser UNPAIRED");

        // Act - Transição para PAIRED
        String supervisorConnectionId = "supervisor-200";
        Session pairedSession = unpairedSession.withSupervisor(supervisorConnectionId);

        // Assert - Estado final PAIRED
        assertTrue(pairedSession.isPaired(), "Estado final deve ser PAIRED");
        assertEquals(supervisorConnectionId, pairedSession.supervisorConnectionId());
        assertEquals(sessionId, pairedSession.sessionId(), "SessionId deve permanecer o mesmo");
        assertEquals(clientConnectionId, pairedSession.clientConnectionId(), "ClientId deve permanecer o mesmo");
    }

    @Test
    @DisplayName("Estado EXPIRED: Sessão antiga deve estar expirada")
    void testExpiredState_OldSession_ShouldBeExpired() {
        // Arrange
        String sessionId = "session-old";
        String clientConnectionId = "client-old";
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        int timeoutMinutes = 30; // 30 minutos de timeout

        // Act
        Session expiredSession = new Session(sessionId, clientConnectionId, null, oneHourAgo, oneHourAgo);

        // Assert
        assertTrue(expiredSession.isExpired(timeoutMinutes),
            "Sessão com última atividade há 1 hora deve estar expirada (timeout: 30 min)");
    }

    @Test
    @DisplayName("Estado NÃO EXPIRED: Sessão recente não deve estar expirada")
    void testNotExpiredState_RecentSession_ShouldNotBeExpired() {
        // Arrange
        String sessionId = "session-recent";
        String clientConnectionId = "client-recent";
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        int timeoutMinutes = 30; // 30 minutos de timeout

        // Act
        Session recentSession = new Session(sessionId, clientConnectionId, null, fiveMinutesAgo, fiveMinutesAgo);

        // Assert
        assertFalse(recentSession.isExpired(timeoutMinutes),
            "Sessão com última atividade há 5 minutos não deve estar expirada (timeout: 30 min)");
    }

    @Test
    @DisplayName("Estado EXPIRED (limite exato): Sessão exatamente no timeout deve estar expirada")
    void testExpiredState_ExactTimeout_ShouldBeExpired() {
        // Arrange
        String sessionId = "session-exact";
        String clientConnectionId = "client-exact";
        int timeoutMinutes = 30;
        Instant exactlyTimeout = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);

        // Act
        Session session = new Session(sessionId, clientConnectionId, null, exactlyTimeout, exactlyTimeout);

        // Assert
        assertTrue(session.isExpired(timeoutMinutes),
            "Sessão exatamente no limite do timeout deve estar expirada");
    }

    @Test
    @DisplayName("Estado PAIRED: getOtherPartyConnectionId deve retornar supervisor quando chamado pelo cliente")
    void testPairedState_GetOtherParty_ClientPerspective_ShouldReturnSupervisor() {
        // Arrange - Estado PAIRED
        String sessionId = "session-paired-1";
        String clientConnectionId = "client-300";
        String supervisorConnectionId = "supervisor-400";
        Instant now = Instant.now();
        Session pairedSession = new Session(sessionId, clientConnectionId, supervisorConnectionId, now, now);

        // Act
        String otherParty = pairedSession.getOtherPartyConnectionId(clientConnectionId);

        // Assert
        assertEquals(supervisorConnectionId, otherParty,
            "Do ponto de vista do cliente, a outra parte deve ser o supervisor");
    }

    @Test
    @DisplayName("Estado PAIRED: getOtherPartyConnectionId deve retornar cliente quando chamado pelo supervisor")
    void testPairedState_GetOtherParty_SupervisorPerspective_ShouldReturnClient() {
        // Arrange - Estado PAIRED
        String sessionId = "session-paired-2";
        String clientConnectionId = "client-500";
        String supervisorConnectionId = "supervisor-600";
        Instant now = Instant.now();
        Session pairedSession = new Session(sessionId, clientConnectionId, supervisorConnectionId, now, now);

        // Act
        String otherParty = pairedSession.getOtherPartyConnectionId(supervisorConnectionId);

        // Assert
        assertEquals(clientConnectionId, otherParty,
            "Do ponto de vista do supervisor, a outra parte deve ser o cliente");
    }

    @Test
    @DisplayName("Estado UNPAIRED: getOtherPartyConnectionId deve retornar null quando não pareado")
    void testUnpairedState_GetOtherParty_ShouldReturnNull() {
        // Arrange - Estado UNPAIRED
        String sessionId = "session-unpaired";
        String clientConnectionId = "client-700";
        Instant now = Instant.now();
        Session unpairedSession = new Session(sessionId, clientConnectionId, null, now, now);

        // Act
        String otherParty = unpairedSession.getOtherPartyConnectionId(clientConnectionId);

        // Assert
        assertNull(otherParty, "Sessão não pareada não deve ter outra parte");
    }

    @Test
    @DisplayName("Atualizar lastActivityAt deve manter estado PAIRED e evitar expiração")
    void testStateTransition_UpdateActivity_ShouldPreventExpiration() {
        // Arrange - Sessão PAIRED antiga
        String sessionId = "session-activity";
        String clientConnectionId = "client-800";
        String supervisorConnectionId = "supervisor-900";
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        int timeoutMinutes = 30;

        Session oldSession = new Session(sessionId, clientConnectionId, supervisorConnectionId, oneHourAgo, oneHourAgo);
        assertTrue(oldSession.isExpired(timeoutMinutes), "Sessão antiga deve estar expirada inicialmente");

        // Act - Atualizar atividade
        Session updatedSession = oldSession.withLastActivity();

        // Assert - Sessão não deve mais estar expirada
        assertFalse(updatedSession.isExpired(timeoutMinutes),
            "Sessão com atividade recente não deve estar expirada");
        assertTrue(updatedSession.isPaired(), "Estado PAIRED deve ser mantido após atualização");
        assertEquals(supervisorConnectionId, updatedSession.supervisorConnectionId(),
            "Supervisor deve permanecer o mesmo");
    }
}
