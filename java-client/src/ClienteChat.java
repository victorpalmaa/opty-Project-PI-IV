import com.opty.socket.tradicional.comunicado.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Iniciar cliente.
 */
public class ClienteChat {
    public static final String HOST_PADRAO  = "localhost";
    public static final int PORTA_PADRAO = 3000;

    public static void main(String[] args) {

        // Validar argumentos
        if (args.length > 2) {
            System.err.println("Uso esperado: java ClienteChat [HOST [PORTA]]\n");
            return;
        }

        // --- CONECTAR AO SERVIDOR ---
        Socket conexao = null;
        try {
            String host = HOST_PADRAO;
            int porta= PORTA_PADRAO;

            if (args.length > 0)
                host = args[0];

            if (args.length == 2)
                porta = Integer.parseInt(args[1]);

            System.out.println("Conectando ao servidor " + host + ":" + porta + "...");
            conexao = new Socket(host, porta);
            System.out.println("Conectado com sucesso!\n");
        
        // Capturar erro de conexão
        } catch (Exception erro) {
            System.err.println("Erro ao conectar ao servidor!");
            System.err.println("Verifique se o servidor está rodando em " + (args.length > 0 ? args[0] : HOST_PADRAO) + ":" + (args.length == 2 ? args[1] : PORTA_PADRAO));
            return;
        }

        // --- CONFIGURAR FLUXOS DE ENTRADA/SAÍDA ---
        // Criar transmissor
        ObjectOutputStream transmissor = null;
        try {
            transmissor = new ObjectOutputStream(conexao.getOutputStream());

            // Evitar deadlock
            transmissor.flush();
        
        // Capturar erro de criação do transmissor
        } catch (Exception erro) {
            System.err.println("Erro ao criar transmissor!");
            return;
        }

        // Criar receptor
        ObjectInputStream receptor = null;
        try {
            receptor = new ObjectInputStream(conexao.getInputStream());
        
        // Capturar erro de criação do receptor
        } catch (Exception erro) {
            System.err.println("Erro ao criar receptor!");
            return;
        }

        // --- CRIAR PARCEIRO ---
        Parceiro servidor = null;
        try {
            servidor = new Parceiro(conexao, receptor, transmissor);

        // Capturar erro de criação do parceiro
        } catch (Exception erro) {
            System.err.println("Erro ao criar Parceiro!");
            return;
        }

        // --- PEDIR CONEXÃO ---
        String sessionId = null;  // Armazenar sessionId
        try {
            System.out.println("Enviando pedido de conexão...");
            PedidoDeConexao pedido = new PedidoDeConexao(null, "Cliente Java");
            servidor.receba(pedido);

            // Esperar resposta
            Comunicado resposta = servidor.envie();

            // Tratar resposta
            if (resposta instanceof RespostaDeConexao) {
                RespostaDeConexao respostaConexao = (RespostaDeConexao)resposta;

                // Sucesso na conexão
                if (respostaConexao.isSucesso()) {
                    sessionId = respostaConexao.getSessionId();  // GUARDAR sessionId
                    System.out.println("✅ " + respostaConexao.getMensagem());
                    System.out.println("   Session ID: " + sessionId);
                    System.out.println("Aguardando supervisor...\n");

                // Falha na conexão
                } else {
                    System.err.println("❌ Erro: " + respostaConexao.getMensagem());
                    return;
                }
            }

        // Capturar erro no pedido de conexão
        } catch (Exception erro) {
            System.err.println("Erro na conexão: " + erro.getMessage());
            return;
        }

        // --- INICIAR TRATADORA DE MENSAGENS ---
        TratadoraDeMensagens tratadoraDeMensagens = null;
        try {
            tratadoraDeMensagens = new TratadoraDeMensagens(servidor);
        } catch (Exception erro) {
            // sei que servidor foi instanciado
        }
        tratadoraDeMensagens.start();

        // --- LOOP PRINCIPAL DO CHAT ---
        Scanner scanner = new Scanner(System.in);
        String mensagem = "";

        System.out.println("═════════════════════════════════════════════");
        System.out.println("             CHAT SUPORTE OPTY");
        System.out.println("═════════════════════════════════════════════");
        System.out.println("Digite suas mensagens (ou 'sair' para encerrar):");
        System.out.println();

        while (!mensagem.equalsIgnoreCase("sair")) {
            try {
                mensagem = scanner.nextLine();

                if (mensagem.equalsIgnoreCase("sair")) {
                    break;
                }

                if (!mensagem.trim().isEmpty()) {
                    MensagemTexto msg = new MensagemTexto(sessionId, "CLIENT", mensagem);
                    servidor.receba(msg);
                }
            } catch (Exception erro) {
                System.err.println("Erro ao enviar mensagem: " + erro.getMessage());
            }
        }

        // --- ENCERRAR CONEXÃO ---
        try {
            System.out.println("\nEncerrando conexão...");
            PedidoParaSair pedidoParaSair = new PedidoParaSair();
            servidor.receba(pedidoParaSair);
            servidor.adeus();
        } catch (Exception erro) {
            System.err.println("Erro ao encerrar: " + erro.getMessage());
        }

        System.out.println("Obrigado por usar o Chat Opty!");
        System.exit(0);
    }
}
