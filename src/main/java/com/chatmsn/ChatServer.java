package com.chatmsn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Classe principal do servidor de chat.
 * Aceita conexões de clientes e gerencia o broadcast de mensagens.
 */
public class ChatServer {
    // Porta em que o servidor irá escutar conexões
    private static final int PORT = 12345;

    // Lista thread-safe de clientes conectados
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        // Cria um ServerSocket para escutar a porta definida
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Inicia uma thread separada para comandos administrativos via console
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String cmd = scanner.nextLine();
                // Comando 'list' ou 'lista' exibe clientes conectados
                if ("list".equalsIgnoreCase(cmd) || "lista".equalsIgnoreCase(cmd)) {
                    listActiveClients();
                }
            }
        }).start();

        // Loop principal: aceita novas conexões indefinidamente
        while (true) {
            Socket socket = serverSocket.accept();  // bloqueia até um cliente conectar
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);                  // adiciona cliente à lista
            new Thread(handler).start();           // inicia thread para tratamento desse cliente
        }
    }

    /**
     * Exibe no console a lista de clientes atualmente conectados.
     */
    private static void listActiveClients() {
        System.out.println("=== Clientes online ===");
        for (ClientHandler c : clients) {
            // Mostra nome e endereço IP de cada cliente
            System.out.printf("%s - %s%n", c.getName(), c.getAddress());
        }
        System.out.println("=======================");
    }

    /**
     * Envia uma mensagem para todos os clientes, exceto o remetente.
     *
     * @param msg     mensagem a ser enviada
     * @param sender  cliente que enviou a mensagem originalmente
     */
    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(msg);  // envia mensagem para cada cliente
            }
        }
    }

    /**
     * Classe interna que trata a conexão com um único cliente.
     * Implementa Runnable para ser executada em thread separada.
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;         // socket de comunicação com o cliente
        private PrintWriter out;             // canal de saída para o cliente
        private String name;                 // nome do usuário
        private Instant connectTime;         // momento da conexão

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            // retorna o endereço IP do cliente
            return socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            // Formato de data/hora para exibir timestamps
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
                // Inicializa canal de saída com auto-flush
                out = new PrintWriter(socket.getOutputStream(), true);

                // Solicita nome do usuário e registra horário de conexão
                out.println("DIGITE SEU NOME:");
                name = in.readLine().trim();
                connectTime = Instant.now();
                String enterTime = fmt.format(connectTime);

                // Loga no console e avisa demais clientes sobre a nova entrada
                System.out.println(name + " entrou no chat às " + enterTime);
                broadcast("[" + enterTime + "] " + name + " entrou no chat.", this);

                // Loop de leitura das linhas enviadas pelo cliente
                String line;
                while ((line = in.readLine()) != null) {
                    // Se receber '/exit', sai do loop e desconecta
                    if ("/exit".equalsIgnoreCase(line)) break;
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [" + name + "] " + line;
                    System.out.println(msg);           // log no servidor
                    broadcast(msg, this);             // envia aos demais
                }
            } catch (IOException e) {
                e.printStackTrace();  // em caso de erro de I/O, imprime stack trace
            } finally {
                // Remove o cliente da lista ao sair
                clients.remove(this);

                // Calcula horário de saída e duração da sessão
                Instant exitTime = Instant.now();
                String exitTimeStr = DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(exitTime);
                Duration online = Duration.between(connectTime, exitTime);
                long min = online.toMinutes();
                long sec = online.minusMinutes(min).getSeconds();
                String duration = String.format("%d min %d s", min, sec);

                // Mensagem de saída
                String msg = "[" + exitTimeStr + "] " + name
                           + " saiu do chat (online por " + duration + ")";

                System.out.println(msg);           // log de saída
                broadcast(msg, this);              // avisa demais clientes

                try {
                    socket.close();                // fecha socket
                } catch (IOException ignored) {}
            }
        }

        /**
         * Envia uma mensagem diretamente a este cliente.
         *
         * @param message texto a ser enviado
         */
        void send(String message) {
            out.println(message);
        }
    }
}
