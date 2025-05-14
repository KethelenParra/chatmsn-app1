// ChatServer.java
package com.chatmsn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Classe principal do servidor de chat.
 * - Aceita conexões de múltiplos clientes.
 * - Faz broadcast de mensagens de um cliente para todos os outros.
 * - Permite comandos administrativos via console:
 *     * list / lista  → exibe clientes conectados
 *     * /mensagem     → envia mensagem “do servidor” a todos
 *     * kick <nome>    → expulsa (desconecta) um cliente pelo nome
 */
public class ChatServer {
    /** Porta em que o servidor ficará escutando novas conexões */
    private static final int PORT = 22230;

    /** Lista thread-safe que guarda todos os ClientHandlers ativos */
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Thread para processar comandos administrativos via console
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            while (true) {
                String cmd = scanner.nextLine().trim();

                // “list” ou “lista”: mostra clientes conectados
                if ("list".equalsIgnoreCase(cmd) || "lista".equalsIgnoreCase(cmd)) {
                    listActiveClients();
                }
                // “/texto”: broadcast do servidor
                else if (cmd.startsWith("/") && cmd.length() > 1) {
                    String text = cmd.substring(1).trim();
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [Servidor] " + text;
                    System.out.println("Enviando para clientes: " + msg);
                    broadcast(msg, null);
                }
                // “kick nome”: expulsa cliente daquele nome
                else if (cmd.toLowerCase().startsWith("kick ")) {
                    String target = cmd.substring(5).trim();
                    kickClient(target);
                }
                // outros comandos são ignorados
            }
        }).start();

        // Loop principal: aceita novas conexões indefinidamente
        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    /** Exibe no console a lista de clientes atualmente conectados */
    private static void listActiveClients() {
        System.out.println("=== Clientes online ===");
        for (ClientHandler c : clients) {
            System.out.printf("%s - %s%n", c.getName(), c.getAddress());
        }
        System.out.println("=======================");
    }

    /**
     * Envia uma mensagem a todos os clientes, exceto o remetente (se informado).
     * @param msg     texto a ser enviado
     * @param sender  quem enviou originalmente (null = servidor)
     */
    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(msg);
            }
        }
    }

    /**
     * Expulsa (desconecta) um cliente específico pelo nome.
     * @param targetName nome do cliente a ser removido
     */
    private static void kickClient(String targetName) {
        for (ClientHandler c : clients) {
            if (c.getName().equalsIgnoreCase(targetName)) {
                c.send("Você foi expulso pelo servidor.");
                c.disconnect();      // força fechamento do socket
                clients.remove(c);   // remove da lista
                System.out.println("Cliente '" + targetName + "' foi expulso.");
                return;
            }
        }
        System.out.println("Cliente '" + targetName + "' não encontrado.");
    }

    /**
     * Classe interna que trata a comunicação com um único cliente.
     * Cada cliente roda em sua própria thread.
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;     // socket deste cliente
        private PrintWriter out;         // para enviar mensagens ao cliente
        private String name;             // nome escolhido pelo usuário
        private Instant connectTime;     // hora em que entrou

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /** Retorna o nome do usuário (usado em list e kick) */
        public String getName() {
            return name;
        }

        /** Retorna o endereço IP do cliente */
        public String getAddress() {
            return socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

                // Prepara canal de saída com auto-flush
                out = new PrintWriter(socket.getOutputStream(), true);

                // 1) Solicita e lê o nome do cliente
                out.println("DIGITE SEU NOME:");
                name = in.readLine().trim();
                connectTime = Instant.now();
                String enterTime = fmt.format(connectTime);

                // 2) Notifica no console e em broadcast a entrada
                System.out.println(name + " entrou no chat às " + enterTime);
                broadcast("[" + enterTime + "] " + name + " entrou no chat.", this);

                // 3) Loop principal de leitura de mensagens
                String line;
                while ((line = in.readLine()) != null) {
                    if ("/exit".equalsIgnoreCase(line)) {
                        break;  // sai se o cliente digitar /exit
                    }
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [" + name + "] " + line;
                    System.out.println(msg);    // log no servidor
                    broadcast(msg, this);       // envia a todos (exceto quem enviou)
                }
            }
            // Se o socket fechar por kick(), cai aqui com SocketException:
            catch (SocketException e) {
                // desconexão forçada: ignora para não poluir o log
            }
            // Qualquer outra falha de I/O deve ser registrada
            catch (IOException e) {
                e.printStackTrace();
            }
            // No fim, seja por /exit ou por erro, tratamos a saída:
            finally {
                // Remove da lista e notifica saída
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

                // Fecha o socket definitivamente
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /**
         * Envia mensagem para este cliente.
         * @param message texto a ser enviado
         */
        void send(String message) {
            out.println(message);
        }

        /**
         * Fecha o socket deste cliente, forçando desconexão imediata.
         */
        void disconnect() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
