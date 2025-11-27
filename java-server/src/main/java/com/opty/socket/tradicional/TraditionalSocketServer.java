package com.opty.socket.tradicional;

import com.opty.socket.service.MessageRouter;
import com.opty.socket.service.SessionManager;
import com.opty.socket.service.SupervisorQueueService;
import com.opty.socket.tradicional.comunicado.ComunicadoDeDesligamento;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * TraditionalSocketServer - Integração do Socket tradicional (padrão professor) com Spring Boot.
 * Este componente inicia o servidor Socket na porta configurada quando a aplicação Spring Boot sobe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraditionalSocketServer {

    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final SupervisorQueueService supervisorQueueService;

    @Value("${socket.traditional.port:3000}")
    private String porta;

    @Value("${socket.traditional.enabled:true}")
    private boolean enabled;

    private ArrayList<Parceiro> usuarios;
    private AceitadoraDeConexao aceitadoraDeConexao;

    /**
     * Inicia o servidor Socket tradicional quando Spring Boot sobe.
     */
    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("Servidor Socket tradicional DESABILITADO via configuração");
            return;
        }

        try {
            log.info("Iniciando servidor Socket tradicional na porta {}...", porta);

            // Criar lista de usuários
            usuarios = new ArrayList<>();

            // Criar e iniciar AceitadoraDeConexao
            aceitadoraDeConexao = new AceitadoraDeConexao(
                    porta,
                    usuarios,
                    sessionManager,
                    messageRouter,
                    supervisorQueueService
            );
            aceitadoraDeConexao.start();

            log.info("✅ Servidor Socket tradicional INICIADO na porta {}", porta);
            log.info("   Clientes Java podem conectar em: localhost:{}", porta);

        } catch (Exception erro) {
            log.error("❌ Erro ao iniciar servidor Socket tradicional: {}", erro.getMessage());
            log.error("   Escolha uma porta apropriada e liberada para uso!");
        }
    }

    /**
     * Desliga graciosamente o servidor Socket tradicional quando aplicação para.
     */
    @PreDestroy
    public void shutdown() {
        if (!enabled || usuarios == null) {
            return;
        }

        log.info("Desligando servidor Socket tradicional...");

        synchronized (usuarios) {
            // Enviar ComunicadoDeDesligamento para todos os clientes
            ComunicadoDeDesligamento comunicadoDeDesligamento = new ComunicadoDeDesligamento();

            for (Parceiro usuario : usuarios) {
                try {
                    usuario.receba(comunicadoDeDesligamento);
                    usuario.adeus();
                } catch (Exception erro) {
                    log.warn("Erro ao desconectar cliente: {}", erro.getMessage());
                }
            }

            usuarios.clear();
        }

        log.info("✅ Servidor Socket tradicional DESLIGADO");
    }

    /**
     * Retorna número de clientes conectados via Socket tradicional.
     */
    public int getClientesConectados() {
        if (usuarios == null) {
            return 0;
        }
        synchronized (usuarios) {
            return usuarios.size();
        }
    }

    /**
     * Verifica se servidor está rodando.
     */
    public boolean isRunning() {
        return enabled && aceitadoraDeConexao != null && aceitadoraDeConexao.isAlive();
    }
}
