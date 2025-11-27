package com.opty.socket.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes INTRACLASSE para a classe Message
 * Abordagem: CATEGORIAS DE MÉTODOS
 *
 * Categorias testadas:
 * 1. Métodos de Validação (hasValidSessionId, hasValidFrom)
 * 2. Métodos Factory (error, connectResponse)
 * 3. Métodos de Construção (construtores)
 */
@DisplayName("Testes INTRACLASSE - Message (Abordagem: CATEGORIAS DE MÉTODOS)")
class MessageTest {

    @Nested
    @DisplayName("Categoria 1: Métodos de Validação")
    class ValidationMethods {

        @Test
        @DisplayName("hasValidSessionId() deve retornar true quando sessionId não é null e não é vazio")
        void testHasValidSessionId_WithValidId_ShouldReturnTrue() {
            // Arrange
            Message message = new Message(
                "session-123",
                "CLIENT",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidSessionId();

            // Assert
            assertTrue(isValid, "SessionId válido ('session-123') deve retornar true");
        }

        @Test
        @DisplayName("hasValidSessionId() deve retornar false quando sessionId é null")
        void testHasValidSessionId_WithNullId_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                null,
                "CLIENT",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidSessionId();

            // Assert
            assertFalse(isValid, "SessionId null deve retornar false");
        }

        @Test
        @DisplayName("hasValidSessionId() deve retornar false quando sessionId é vazio")
        void testHasValidSessionId_WithEmptyId_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                "",
                "CLIENT",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidSessionId();

            // Assert
            assertFalse(isValid, "SessionId vazio deve retornar false");
        }

        @Test
        @DisplayName("hasValidSessionId() deve retornar false quando sessionId contém apenas espaços")
        void testHasValidSessionId_WithBlankId_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                "   ",
                "CLIENT",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidSessionId();

            // Assert
            assertFalse(isValid, "SessionId com apenas espaços deve retornar false");
        }

        @Test
        @DisplayName("hasValidFrom() deve retornar true quando from é 'CLIENT'")
        void testHasValidFrom_WithClient_ShouldReturnTrue() {
            // Arrange
            Message message = new Message(
                "session-456",
                "CLIENT",
                MessageType.MESSAGE,
                Map.of("text", "Hello from client"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidFrom();

            // Assert
            assertTrue(isValid, "From com valor 'CLIENT' deve retornar true");
        }

        @Test
        @DisplayName("hasValidFrom() deve retornar true quando from é 'SUPERVISOR'")
        void testHasValidFrom_WithSupervisor_ShouldReturnTrue() {
            // Arrange
            Message message = new Message(
                "session-789",
                "SUPERVISOR",
                MessageType.MESSAGE,
                Map.of("text", "Hello from supervisor"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidFrom();

            // Assert
            assertTrue(isValid, "From com valor 'SUPERVISOR' deve retornar true");
        }

        @Test
        @DisplayName("hasValidFrom() deve retornar false quando from é null")
        void testHasValidFrom_WithNull_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                "session-001",
                null,
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidFrom();

            // Assert
            assertFalse(isValid, "From null deve retornar false");
        }

        @Test
        @DisplayName("hasValidFrom() deve retornar false quando from é um valor inválido")
        void testHasValidFrom_WithInvalidValue_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                "session-002",
                "INVALID_SENDER",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidFrom();

            // Assert
            assertFalse(isValid, "From com valor inválido deve retornar false");
        }

        @Test
        @DisplayName("hasValidFrom() deve retornar false quando from é vazio")
        void testHasValidFrom_WithEmpty_ShouldReturnFalse() {
            // Arrange
            Message message = new Message(
                "session-003",
                "",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act
            boolean isValid = message.hasValidFrom();

            // Assert
            assertFalse(isValid, "From vazio deve retornar false");
        }

        @Test
        @DisplayName("hasValidFrom() deve ser case-sensitive")
        void testHasValidFrom_IsCaseSensitive() {
            // Arrange - lowercase
            Message messageWithLowercase = new Message(
                "session-004",
                "client",
                MessageType.MESSAGE,
                Map.of("text", "Hello"),
                Instant.now()
            );

            // Act & Assert
            assertFalse(messageWithLowercase.hasValidFrom(),
                "From com 'client' (lowercase) deve retornar false");
        }
    }

    @Nested
    @DisplayName("Categoria 2: Métodos Factory")
    class FactoryMethods {

        @Test
        @DisplayName("error() deve criar mensagem de erro com todos os campos corretos")
        void testError_ShouldCreateErrorMessage() {
            // Arrange
            String sessionId = "session-error-1";
            String errorMessage = "Conexão recusada";

            // Act
            Message errorMsg = Message.error(sessionId, errorMessage);

            // Assert
            assertNotNull(errorMsg, "Mensagem de erro não deve ser null");
            assertEquals(sessionId, errorMsg.sessionId(), "SessionId deve corresponder");
            assertEquals("SERVER", errorMsg.from(), "From deve ser 'SERVER'");
            assertEquals(MessageType.ERROR, errorMsg.type(), "Type deve ser ERROR");
            assertNotNull(errorMsg.payload(), "Payload não deve ser null");
            assertEquals(errorMessage, errorMsg.payload().get("error"),
                "Payload deve conter a mensagem de erro no campo 'error'");
            assertNotNull(errorMsg.timestamp(), "Timestamp não deve ser null");
        }

        @Test
        @DisplayName("error() deve criar mensagem com timestamp atual")
        void testError_ShouldHaveCurrentTimestamp() {
            // Arrange
            Instant before = Instant.now().minus(1, ChronoUnit.SECONDS);
            String sessionId = "session-error-2";
            String errorMessage = "Erro de validação";

            // Act
            Message errorMsg = Message.error(sessionId, errorMessage);
            Instant after = Instant.now().plus(1, ChronoUnit.SECONDS);

            // Assert
            assertTrue(errorMsg.timestamp().isAfter(before) || errorMsg.timestamp().equals(before),
                "Timestamp deve ser depois ou igual ao momento antes da criação");
            assertTrue(errorMsg.timestamp().isBefore(after) || errorMsg.timestamp().equals(after),
                "Timestamp deve ser antes ou igual ao momento depois da criação");
        }

        @Test
        @DisplayName("error() deve aceitar sessionId null")
        void testError_WithNullSessionId_ShouldCreateMessage() {
            // Arrange
            String sessionId = null;
            String errorMessage = "Sessão não encontrada";

            // Act
            Message errorMsg = Message.error(sessionId, errorMessage);

            // Assert
            assertNotNull(errorMsg, "Mensagem de erro não deve ser null");
            assertNull(errorMsg.sessionId(), "SessionId deve ser null");
            assertEquals("SERVER", errorMsg.from(), "From deve ser 'SERVER'");
            assertEquals(MessageType.ERROR, errorMsg.type(), "Type deve ser ERROR");
            assertEquals(errorMessage, errorMsg.payload().get("error"),
                "Payload deve conter a mensagem de erro");
        }

        @Test
        @DisplayName("connectResponse() deve criar mensagem de conexão bem-sucedida")
        void testConnectResponse_ShouldCreateConnectMessage() {
            // Arrange
            String sessionId = "session-connect-1";

            // Act
            Message connectMsg = Message.connectResponse(sessionId);

            // Assert
            assertNotNull(connectMsg, "Mensagem de conexão não deve ser null");
            assertEquals(sessionId, connectMsg.sessionId(), "SessionId deve corresponder");
            assertEquals("SERVER", connectMsg.from(), "From deve ser 'SERVER'");
            assertEquals(MessageType.CONNECT, connectMsg.type(), "Type deve ser CONNECT");
            assertNotNull(connectMsg.payload(), "Payload não deve ser null");
            assertEquals("Connected successfully", connectMsg.payload().get("message"),
                "Payload deve conter mensagem de conexão estabelecida");
            assertEquals(sessionId, connectMsg.payload().get("sessionId"),
                "Payload deve conter o sessionId");
            assertNotNull(connectMsg.timestamp(), "Timestamp não deve ser null");
        }

        @Test
        @DisplayName("connectResponse() deve criar mensagem com timestamp atual")
        void testConnectResponse_ShouldHaveCurrentTimestamp() {
            // Arrange
            Instant before = Instant.now().minus(1, ChronoUnit.SECONDS);
            String sessionId = "session-connect-2";

            // Act
            Message connectMsg = Message.connectResponse(sessionId);
            Instant after = Instant.now().plus(1, ChronoUnit.SECONDS);

            // Assert
            assertTrue(connectMsg.timestamp().isAfter(before) || connectMsg.timestamp().equals(before),
                "Timestamp deve ser depois ou igual ao momento antes da criação");
            assertTrue(connectMsg.timestamp().isBefore(after) || connectMsg.timestamp().equals(after),
                "Timestamp deve ser antes ou igual ao momento depois da criação");
        }

        @Test
        @DisplayName("Mensagens factory devem ter hasValidFrom() == false (pois from é SERVER)")
        void testFactoryMessages_ShouldHaveInvalidFrom() {
            // Arrange & Act
            Message errorMsg = Message.error("session-1", "Erro");
            Message connectMsg = Message.connectResponse("session-2");

            // Assert
            assertFalse(errorMsg.hasValidFrom(),
                "Mensagem de erro com from='SERVER' deve ter hasValidFrom() == false");
            assertFalse(connectMsg.hasValidFrom(),
                "Mensagem de conexão com from='SERVER' deve ter hasValidFrom() == false");
        }
    }

    @Nested
    @DisplayName("Categoria 3: Métodos de Construção")
    class ConstructorMethods {

        @Test
        @DisplayName("Construtor completo deve criar mensagem com todos os campos fornecidos")
        void testFullConstructor_WithAllFields_ShouldCreateMessage() {
            // Arrange
            String sessionId = "session-constructor-1";
            String from = "CLIENT";
            MessageType type = MessageType.MESSAGE;
            Map<String, Object> payload = Map.of("text", "Mensagem de teste");
            Instant timestamp = Instant.now();

            // Act
            Message message = new Message(sessionId, from, type, payload, timestamp);

            // Assert
            assertEquals(sessionId, message.sessionId());
            assertEquals(from, message.from());
            assertEquals(type, message.type());
            assertEquals(payload, message.payload());
            assertEquals(timestamp, message.timestamp());
        }

        @Test
        @DisplayName("Construtor sem timestamp deve criar mensagem com timestamp atual")
        void testConstructorWithoutTimestamp_ShouldHaveCurrentTimestamp() {
            // Arrange
            Instant before = Instant.now().minus(1, ChronoUnit.SECONDS);
            String sessionId = "session-auto-timestamp";
            String from = "CLIENT";
            MessageType type = MessageType.MESSAGE;
            Map<String, Object> payload = Map.of("text", "Test");

            // Act
            Message message = new Message(sessionId, from, type, payload);
            Instant after = Instant.now().plus(1, ChronoUnit.SECONDS);

            // Assert
            assertNotNull(message.timestamp(), "Timestamp não deve ser null");
            assertTrue(message.timestamp().isAfter(before) || message.timestamp().equals(before),
                "Timestamp deve ser depois ou igual ao momento antes da criação");
            assertTrue(message.timestamp().isBefore(after) || message.timestamp().equals(after),
                "Timestamp deve ser antes ou igual ao momento depois da criação");
        }

        @Test
        @DisplayName("Construtor sem payload deve criar mensagem sem payload")
        void testConstructorWithoutPayload_ShouldCreateMessageWithNullPayload() {
            // Arrange
            String sessionId = "session-no-payload";
            String from = "CLIENT";
            MessageType type = MessageType.DISCONNECT;

            // Act
            Message message = new Message(sessionId, from, type);

            // Assert
            assertNotNull(message, "Message não deve ser null");
            assertEquals(sessionId, message.sessionId());
            assertEquals(from, message.from());
            assertEquals(type, message.type());
            assertNull(message.payload(), "Payload deve ser null");
            assertNotNull(message.timestamp(), "Timestamp não deve ser null");
        }

        @Test
        @DisplayName("Construtor deve aceitar todos os tipos de mensagem")
        void testConstructor_WithDifferentMessageTypes_ShouldWork() {
            // Arrange & Act & Assert
            Instant now = Instant.now();
            Map<String, Object> payload = Map.of("text", "test");

            Message connectMsg = new Message("s1", "CLIENT", MessageType.CONNECT, payload, now);
            assertEquals(MessageType.CONNECT, connectMsg.type());

            Message regularMsg = new Message("s2", "CLIENT", MessageType.MESSAGE, payload, now);
            assertEquals(MessageType.MESSAGE, regularMsg.type());

            Message disconnectMsg = new Message("s3", "CLIENT", MessageType.DISCONNECT);
            assertEquals(MessageType.DISCONNECT, disconnectMsg.type());

            Message errorMsg = new Message("s4", "SERVER", MessageType.ERROR, payload, now);
            assertEquals(MessageType.ERROR, errorMsg.type());

            Message queueMsg = new Message("s5", "SERVER", MessageType.SESSION_QUEUE_UPDATE, payload, now);
            assertEquals(MessageType.SESSION_QUEUE_UPDATE, queueMsg.type());
        }

        @Test
        @DisplayName("Record deve ser imutável - campos devem ser acessíveis mas não modificáveis")
        void testConstructor_RecordShouldBeImmutable() {
            // Arrange
            String sessionId = "session-immutable";
            String from = "CLIENT";
            MessageType type = MessageType.MESSAGE;
            Map<String, Object> payload = Map.of("text", "Original payload");
            Instant timestamp = Instant.now();

            // Act
            Message message = new Message(sessionId, from, type, payload, timestamp);

            // Assert - Verificar que os getters retornam os valores corretos
            assertEquals(sessionId, message.sessionId());
            assertEquals(from, message.from());
            assertEquals(type, message.type());
            assertEquals(payload, message.payload());
            assertEquals(timestamp, message.timestamp());

            // Note: Como é um record, não há setters para testar
            // A imutabilidade é garantida pelo compilador Java
        }

        @Test
        @DisplayName("Duas mensagens com mesmos valores devem ser iguais (equals)")
        void testConstructor_EqualMessages_ShouldBeEqual() {
            // Arrange
            Instant timestamp = Instant.now();
            Map<String, Object> payload = Map.of("text", "Hello");
            Message message1 = new Message("session-1", "CLIENT", MessageType.MESSAGE, payload, timestamp);
            Message message2 = new Message("session-1", "CLIENT", MessageType.MESSAGE, payload, timestamp);

            // Act & Assert
            assertEquals(message1, message2, "Mensagens com mesmos valores devem ser iguais");
            assertEquals(message1.hashCode(), message2.hashCode(), "HashCodes devem ser iguais");
        }

        @Test
        @DisplayName("Duas mensagens com valores diferentes não devem ser iguais")
        void testConstructor_DifferentMessages_ShouldNotBeEqual() {
            // Arrange
            Instant timestamp = Instant.now();
            Map<String, Object> payload = Map.of("text", "Hello");
            Message message1 = new Message("session-1", "CLIENT", MessageType.MESSAGE, payload, timestamp);
            Message message2 = new Message("session-2", "CLIENT", MessageType.MESSAGE, payload, timestamp);

            // Act & Assert
            assertNotEquals(message1, message2, "Mensagens com sessionIds diferentes não devem ser iguais");
        }

        @Test
        @DisplayName("Payload pode conter diferentes tipos de objetos")
        void testConstructor_PayloadWithDifferentTypes_ShouldWork() {
            // Arrange
            Map<String, Object> complexPayload = Map.of(
                "text", "Hello",
                "count", 42,
                "enabled", true,
                "timestamp", Instant.now().toEpochMilli()
            );

            // Act
            Message message = new Message("session-complex", "CLIENT", MessageType.MESSAGE, complexPayload);

            // Assert
            assertNotNull(message.payload());
            assertEquals("Hello", message.payload().get("text"));
            assertEquals(42, message.payload().get("count"));
            assertEquals(true, message.payload().get("enabled"));
            assertNotNull(message.payload().get("timestamp"));
        }
    }
}
