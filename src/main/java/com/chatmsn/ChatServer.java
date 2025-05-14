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
 * - Ao entrar, cada cliente recebe primeiro as regras, depois solicita o nome.
 * - Se um cliente for expulso, todos os outros recebem aviso de expulsão.
 * - Se sair voluntariamente (/exit), aparece apenas "saiu do chat".
 */
public class ChatServer {
    /** Porta em que o servidor ficará escutando novas conexões */
    private static final int PORT = 22230;
    /** Guarda todos os ClientHandlers ativos de forma thread-safe */
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Thread para ler comandos administrativos via console
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            while (true) {
                String cmd = scanner.nextLine().trim();

                // list / lista → mostra quem está online
                if ("list".equalsIgnoreCase(cmd) || "lista".equalsIgnoreCase(cmd)) {
                    listActiveClients();
                }
                // /texto → broadcast de mensagem do servidor
                else if (cmd.startsWith("/") && cmd.length() > 1) {
                    String text = cmd.substring(1).trim();
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [Servidor] " + text;
                    System.out.println("Enviando para clientes: " + msg);
                    broadcast(msg, /* sender = */ null);
                }
                // kick nome → expulsa cliente com aquele nome
                else if (cmd.toLowerCase().startsWith("kick ")) {
                    String target = cmd.substring(5).trim();
                    kickClient(target);
                }
                // outros comandos são ignorados
            }
        }).start();

        // Loop principal: aceita clientes indefinidamente
        while (true) {
            Socket socket = serverSocket.accept();       // bloqueia até ter conexão
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);                        // registra handler
            new Thread(handler).start();                 // inicia thread de atendimento
        }
    }

    /** Imprime no console todos os clientes atualmente conectados */
    private static void listActiveClients() {
        System.out.println("=== Clientes online ===");
        for (ClientHandler c : clients) {
            System.out.printf("%s - %s%n", c.getName(), c.getAddress());
        }
        System.out.println("=======================");
    }

    /**
     * Envia uma mensagem a todos os clientes, exceto quem enviou (se houver).
     * @param msg     texto a ser enviado
     * @param sender  remetente original (null = servidor)
     */
    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(msg);
            }
        }
    }

    /**
     * Expulsa um cliente pelo nome:
     * - Avisa individualmente
     * - Marca como "kicked"
     * - Fecha o socket
     * - Remove da lista
     * - Envia broadcast de expulsão para todos
     */
    private static void kickClient(String targetName) {
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
        for (ClientHandler c : clients) {
            if (c.getName().equalsIgnoreCase(targetName)) {

                // 1) avisa diretamente
                c.send("Você foi expulso pelo servidor.");
                // 2) marca como expulso para diferenciar na saída
                c.markKicked();
                // 3) fecha imediatamente o socket
                c.disconnect();
                // 4) remove da lista de clientes
                clients.remove(c);

                // 5) notifica todos os outros sobre a expulsão
                String time = fmt.format(Instant.now());
                String notice = "[" + time + "] " + c.getName() + " foi expulso pelo servidor.";
                broadcast(notice, /*sender*/ null);

                System.out.println("Cliente '" + targetName + "' foi expulso.");
                return;
            }
        }
        System.out.println("Cliente '" + targetName + "' não encontrado.");
    }

    /** 
     * Trata cada cliente em sua própria thread, gerenciando entrada,
     * leitura de mensagens e saída (voluntária ou expulsão).
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;        // socket deste cliente
        private PrintWriter out;            // canal de saída para o cliente
        private String name;                // nome escolhido
        private Instant connectTime;        // hora da entrada
        private volatile boolean kicked;    // marca se foi expulso

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.kicked = false;
        }

        /** Retorna o nome do usuário (usado em list e kick) */
        public String getName() {
            return name;
        }
        /** Retorna o IP do cliente */
        public String getAddress() {
            return socket.getInetAddress().getHostAddress();
        }
        /** Marca este handler como “expulso” */
        void markKicked() {
            this.kicked = true;
        }

        @Override
        public void run() {
            DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

                // Prepara canal de saída (auto-flush)
                out = new PrintWriter(socket.getOutputStream(), true);

                // → Envia as regras ANTES de pedir o nome
                out.println("==== Regras do Chat ====");
                out.println("1. Sem xingamentos.");
                out.println("2. Não compartilhe informações pessoais.");
                out.println("3. Seja respeitoso com todos.");
                out.println("4. Mantenha o chat organizado.");
                out.println("Os servidores estão de olho!!");
                out.println();  // espaço em branco

                // → Agora solicita o nome
                out.println("DIGITE SEU NOME:");
                name = in.readLine().trim();
                connectTime = Instant.now();

                // → Notifica entrada ao grupo (exceto ao próprio)
                String enterTime = fmt.format(connectTime);
                System.out.println(name + " entrou no chat às " + enterTime);
                broadcast("[" + enterTime + "] " + name + " entrou no chat.", this);

                // → Loop de recebimento de mensagens do cliente
                String line;
                while ((line = in.readLine()) != null) {
                    if ("/exit".equalsIgnoreCase(line)) {
                        break;  // saída voluntária
                    }
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [" + name + "] " + line;
                    System.out.println(msg);      // log no servidor
                    broadcast(msg, this);         // envia ao grupo
                }
            }
            // Se for desconexão forçada (kick), chega aqui sem log
            catch (SocketException e) {
                // ignora para não poluir o console
            }
            // Outras I/O exceptions devem aparecer
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                // Remove de qualquer forma da lista
                clients.remove(this);

                // Se **não** foi expulso, faz broadcast de saída voluntária
                if (!kicked) {
                    Instant exitTime = Instant.now();
                    String exitStr = DateTimeFormatter
                        .ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(exitTime);
                    Duration online = Duration.between(connectTime, exitTime);
                    long min = online.toMinutes();
                    long sec = online.minusMinutes(min).getSeconds();
                    String duration = String.format("%d min %d s", min, sec);

                    String msg = "[" + exitStr + "] " + name
                               + " saiu do chat (online por " + duration + ")";
                    System.out.println(msg);
                    broadcast(msg, this);
                }

                // Fecha socket de limpeza
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        /** Envia mensagem para este cliente. */
        void send(String message) {
            out.println(message);
        }

        /** Fecha o socket deste cliente, forçando desconexão. */
        void disconnect() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
