package main.java.com.chatmsn;

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

public class ChatServer {
    private static final int PORT = 12345;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Thread para comandos administrativos no servidor (e.g. listar clientes)
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String cmd = scanner.nextLine();
                if ("list".equalsIgnoreCase(cmd) || "lista".equalsIgnoreCase(cmd)) {
                    listActiveClients();
                }
            }
        }).start();

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    private static void listActiveClients() {
        System.out.println("=== Clientes online ===");
        for (ClientHandler c : clients) {
            System.out.printf("%s - %s%n", c.getName(), c.getAddress());
        }
        System.out.println("=======================");
    }

    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(msg);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String name;
        private Instant connectTime;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getName() {
            return name;
        }

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
                out = new PrintWriter(socket.getOutputStream(), true);

                // Leitura do nome e registro de horário de conexão
                out.println("DIGITE SEU NOME:");
                name = in.readLine().trim();
                connectTime = Instant.now();
                String enterTime = fmt.format(connectTime);

                // Log e broadcast de entrada
                System.out.println(name + " entrou no chat às " + enterTime);
                broadcast("[" + enterTime + "] " + name + " entrou no chat.", this);

                // Loop de recebimento de mensagens
                String line;
                while ((line = in.readLine()) != null) {
                    if ("/exit".equalsIgnoreCase(line)) break;
                    String time = fmt.format(Instant.now());
                    String msg = "[" + time + "] [" + name + "] " + line;
                    System.out.println(msg);
                    broadcast(msg, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(this);

                // Horário e duração da sessão
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

                // Log de saída e broadcast
                System.out.println(msg);
                broadcast(msg, this);

                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        void send(String message) {
            out.println(message);
        }
    }
}