// ChatClient.java
package com.chatmsn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente de chat que conecta ao ChatServer.
 * Envia mensagens digitadas pelo usuário e exibe mensagens recebidas.
 *
 * Uso: java -jar chat-client.jar <host> <porta> <nome>
 */
public class ChatClient {
    public static void main(String[] args) throws Exception {
        // Verifica parâmetros (host, porta e nome)
        if (args.length < 3) {
            System.out.println("Uso: java -jar chat-client.jar <host> <porta> <nome>");
            return;
        }

        String host = args[0];                  // endereço do servidor
        int port = Integer.parseInt(args[1]);   // porta do servidor
        String user = args[2];                  // nome do usuário

        // Abre socket e canais de I/O (usa try-with-resources)
        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(
                 socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(
                 new InputStreamReader(System.in))
        ) {
            // Primeiro, recebe do servidor o prompt de "DIGITE SEU NOME:"
            System.out.println(serverIn.readLine());
            // Envia nome
            serverOut.println(user);

            // Thread para ler e exibir mensagens vindas do servidor
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverIn.readLine()) != null) {
                        System.out.println(msg);
                    }
                    // Quando o servidor fechar conexão
                    System.out.println("Conexão encerrada pelo servidor.");
                } catch (IOException e) {
                    System.out.println("Conexão encerrada.");
                } finally {
                    // Encerra aplicação
                    System.exit(0);
                }
            }).start();

            // Loop principal: lê tecladas do usuário e envia ao servidor
            String line;
            while ((line = userIn.readLine()) != null) {
                serverOut.println(line);
                // Se digitar /exit, sai do loop e fecha tudo
                if ("/exit".equalsIgnoreCase(line)) break;
            }
        }
        // Try-with-resources fecha socket e streams automaticamente aqui
    }
}
