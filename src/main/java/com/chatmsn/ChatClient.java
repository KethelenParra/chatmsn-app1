package main.java.com.chatmsn;
import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java -jar chat-client.jar <host> <porta> <nome>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];

        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(
                 socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(
                 new InputStreamReader(System.in))
        ) {
            System.out.println(serverIn.readLine());
            serverOut.println(user);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverIn.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException ignored) {}
            }).start();

            String line;
            while ((line = userIn.readLine()) != null) {
                serverOut.println(line);
                if ("/exit".equalsIgnoreCase(line)) break;
            }
        }
    }
}