package com.opty.socket.tradicional;

import com.opty.socket.model.ConnectionInfo;
import com.opty.socket.model.Message;
import com.opty.socket.model.MessageType;
import com.opty.socket.model.Session;
import com.opty.socket.service.MessageRouter;
import com.opty.socket.service.SessionManager;
import com.opty.socket.service.SupervisorQueueService;
import com.opty.socket.tradicional.comunicado.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Thread que gerencia a comunicação com cada cliente conectado via Socket tradicional.
 */
@Slf4j
public class SupervisoraDeConexao extends Thread {
    private Parceiro            usuario;
    private Socket              conexao;
    private ArrayList<Parceiro> usuarios;

    // Integração com Spring Boot
    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final SupervisorQueueService supervisorQueueService;

    private String connectionId;
    private String sessionId;

    public SupervisoraDeConexao(Socket conexao,
                                ArrayList<Parceiro> usuarios,
                                SessionManager sessionManager,
                                MessageRouter messageRouter,
                                SupervisorQueueService supervisorQueueService)
            throws Exception {
        if (conexao == null)
            throw new Exception("Conexao ausente");

        if (usuarios == null)
            throw new Exception("Usuarios ausentes");

        this.conexao  = conexao;
        this.usuarios = usuarios;
        this.sessionManager = sessionManager;
        this.messageRouter = messageRouter;
        this.supervisorQueueService = supervisorQueueService;

        // Gera ID único para esta conexão
        this.connectionId = UUID.randomUUID().toString();
    }

    public void run() {
        ObjectOutputStream transmissor;
        try {
            transmissor = new ObjectOutputStream(this.conexao.getOutputStream());
            transmissor.flush();
        } catch (Exception erro) {
            log.error("Erro ao criar transmissor: {}", erro.getMessage());
            return;
        }

        ObjectInputStream receptor = null;
        try {
            // ObjectInputStream DEPOIS
            receptor = new ObjectInputStream(this.conexao.getInputStream());
        } catch (Exception err0) {
            try {
                transmissor.close();
            } catch (Exception falha) {
                // só tentando fechar antes de acabar a thread
            }
            log.error("Erro ao criar receptor: {}", err0.getMessage());
            return;
        }

        try {
            this.usuario = new Parceiro(this.conexao, receptor, transmissor);
        } catch (Exception erro) {
            // sei que passei os parametros corretos
        }

        try {
            synchronized (this.usuarios) {
                this.usuarios.add(this.usuario);
            }

            log.info("Cliente Socket conectado: connectionId={}", connectionId);

            // Loop infinito processando comunicados
            for(;;) {
                Comunicado comunicado = this.usuario.envie();

                if (comunicado == null)
                    return;
                else if (comunicado instanceof PedidoDeConexao) {
                    // Processar PedidoDeConexao
                    PedidoDeConexao pedido = (PedidoDeConexao)comunicado;
                    log.info("Recebido PedidoDeConexao: {}", pedido);

                    // Criar sessão no SessionManager
                    Session session = sessionManager.createSession(connectionId);
                    this.sessionId = session.sessionId();

                    // Registrar conexão Socket tradicional (sem WebSocketSession)
                    ConnectionInfo connInfo = new ConnectionInfo(
                            connectionId,
                            null, // Socket tradicional não tem WebSocketSession
                            this.usuario, // Parceiro para Socket tradicional
                            "CLIENT",
                            sessionId
                    );
                    sessionManager.registerConnection(connInfo);

                    // Enviar resposta de sucesso
                    RespostaDeConexao resposta = new RespostaDeConexao(
                            true,
                            sessionId,
                            "Conectado com sucesso! Session ID: " + sessionId
                    );
                    this.usuario.receba(resposta);

                    // Notificar supervisores que há nova sessão na fila
                    supervisorQueueService.broadcastQueueUpdate();

                    log.info("Sessão criada para cliente Socket: sessionId={}, connectionId={}",
                            sessionId, connectionId);
                }
                else if (comunicado instanceof MensagemTexto) {
                    // Processar mensagem de texto
                    MensagemTexto mensagem = (MensagemTexto)comunicado;
                    log.info("Recebida MensagemTexto: {}", mensagem);

                    // Converter para formato Message e rotear via MessageRouter
                    Message message = new Message(
                            mensagem.getSessionId(),
                            "CLIENT",
                            MessageType.MESSAGE,
                            Map.of("text", mensagem.getConteudo(),
                                   "timestamp", mensagem.getTimestamp().toString())
                    );

                    // MessageRouter vai enviar para o supervisor via WebSocket
                    boolean routed = messageRouter.routeMessage(connectionId, message);

                    if (!routed) {
                        log.warn("Falha ao rotear mensagem: sessionId={}", sessionId);
                        // Opcional: enviar erro de volta pro cliente
                    }

                    // Atualizar atividade da sessão
                    sessionManager.updateSessionActivity(sessionId);
                }
                else if (comunicado instanceof PedidoParaSair) {
                    log.info("Cliente solicitou desconexão: connectionId={}", connectionId);

                    // Notificar supervisor sobre desconexão
                    messageRouter.notifyDisconnect(connectionId);

                    // Remover do SessionManager
                    sessionManager.removeSession(sessionId);
                    sessionManager.removeConnection(connectionId);

                    // Atualizar fila
                    supervisorQueueService.broadcastQueueUpdate();

                    // Remover da lista e fechar
                    synchronized (this.usuarios) {
                        this.usuarios.remove(this.usuario);
                    }
                    this.usuario.adeus();

                    log.info("Cliente desconectado: connectionId={}", connectionId);
                    return; // Termina a thread
                }
            }
        } catch (Exception erro) {
            log.error("Erro no processamento do cliente: connectionId={}, erro={}",
                    connectionId, erro.getMessage(), erro);

            try {
                // Cleanup
                if (sessionId != null) {
                    messageRouter.notifyDisconnect(connectionId);
                    sessionManager.removeSession(sessionId);
                    sessionManager.removeConnection(connectionId);
                    supervisorQueueService.broadcastQueueUpdate();
                }

                synchronized (this.usuarios) {
                    this.usuarios.remove(this.usuario);
                }

                transmissor.close();
                receptor.close();
            } catch (Exception falha) {
                // só tentando fechar antes de acabar a thread
            }

            return;
        }
    }
}
