// ChatServer.java
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
 * Aceita conexões de clientes, gerencia broadcast de mensagens
 * e permite ao administrador enviar mensagens via console.
 */
public class ChatServer {
    // Porta em que o servidor ficará escutando novas conexões
    private static final int PORT = 22230;
    // Lista thread-safe de todos os clientes conectados
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        // Cria o socket de servidor na porta definida
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Thread separada para processar comandos administrativos via console
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            // Formato de hora para timestamp das mensagens administrativas
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            while (true) {
                String cmd = scanner.nextLine().trim();
                // Se o comando for "list" ou "lista", exibe clientes conectados
                if ("list".equalsIgnoreCase(cmd) || "lista".equalsIgnoreCase(cmd)) {
                    listActiveClients();
                }
                // Se começar com "/", envia mensagem do servidor para todos
                else if (cmd.startsWith("/") && cmd.length() > 1) {
                    String text = cmd.substring(1).trim();             // remove a barra
                    String time = fmt.format(Instant.now());           // timestamp atual
                    String msg = "[" + time + "] [Servidor] " + text;  // monta mensagem
                    System.out.println("Enviando para clientes: " + msg);
                    broadcast(msg, null);  // sender=null faz enviar a todos
                }
                // Outros comandos são ignorados
            }
        }).start();

        // Loop principal: aceita clientes indefinidamente
        while (true) {
            Socket socket = serverSocket.accept();       // espera conexão
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);                        // registra cliente
            new Thread(handler).start();                 // inicia atendimento
        }
    }

    /**
     * Imprime no console todos os clientes atualmente conectados.
     */
    private static void listActiveClients() {
        System.out.println("=== Clientes online ===");
        for (ClientHandler c : clients) {
            System.out.printf("%s - %s%n", c.getName(), c.getAddress());
        }
        System.out.println("=======================");
    }

    /**
     * Envia uma mensagem a todos os clientes registrados,
     * exceto o remetente (se informado).
     *
     * @param msg     mensagem a ser enviada
     * @param sender  quem enviou originalmente (null para servidor)
     */
    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(msg);
            }
        }
    }

    /**
     * Classe interna para tratar cada cliente em sua própria thread.
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;      // canal de comunicação
        private PrintWriter out;          // para envio de dados ao cliente
        private String name;              // nome do usuário
        private Instant connectTime;      // horário de conexão

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getName() {
            return name;  // retorna nome para listagem
        }

        public String getAddress() {
            // retorna IP do cliente
            return socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            // Formatador de hora para mensagens de log e broadcast
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
                // Configura canal de escrita no socket com auto-flush
                out = new PrintWriter(socket.getOutputStream(), true);

                // Solicita nome ao cliente
                out.println("DIGITE SEU NOME:");
                name = in.readLine().trim();
                connectTime = Instant.now();
                String enterTime = fmt.format(connectTime);

                // Loga entrada no servidor e notifica outros clientes
                System.out.println(name + " entrou no chat às " + enterTime);
                broadcast("[" + enterTime + "] " + name + " entrou no chat.", this);

                // Loop de recebimento de mensagens do cliente
                String line;
                while ((line = in.readLine()) != null) {
                    // Se digitar /exit, encerra conexão
                    if ("/exit".equalsIgnoreCase(line)) break;
                    // Caso normal: acrescenta timestamp e envia ao grupo
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [" + name + "] " + line;
                    System.out.println(msg);      // log no console do servidor
                    broadcast(msg, this);         // envia aos demais clientes
                }
            } catch (IOException e) {
                e.printStackTrace();  // imprime stack trace em erro de I/O
            } finally {
                // Ao desconectar, remove cliente e notifica saída
                clients.remove(this);
                Instant exitTime = Instant.now();
                String exitTimeStr = DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(exitTime);
                Duration online = Duration.between(connectTime, exitTime);
                long min = online.toMinutes();
                long sec = online.minusMinutes(min).getSeconds();
                String duration = String.format("%d min %d s", min, sec);

                String msg = "[" + exitTimeStr + "] " + name
                           + " saiu do chat (online por " + duration + ")";
                System.out.println(msg);
                broadcast(msg, this);

                // Fecha socket
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /**
         * Envia mensagem ao cliente desta instância.
         *
         * @param message texto a ser enviado
         */
        void send(String message) {
            out.println(message);
        }
    }
}
