package com.chatmsn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente de chat simples que se conecta a um servidor ChatServer.
 * Envia mensagens digitadas pelo usuário e exibe mensagens recebidas do servidor.
 *
 * Uso: java -jar chat-client.jar <host> <porta> <nome>
 */
public class ChatClient {
    public static void main(String[] args) throws Exception {
        // Verifica se os parâmetros necessários foram informados
        if (args.length < 3) {
            System.out.println("Uso: java -jar chat-client.jar <host> <porta> <nome>");
            return;
        }

        // Lê argumentos de conexão e nome de usuário
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];

        // Abre socket de conexão com servidor usando try-with-resources para garantir fechamento
        try (Socket socket = new Socket(host, port);
             // Canal de entrada para ler mensagens enviadas pelo servidor
             BufferedReader serverIn = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             // Canal de saída para enviar mensagens ao servidor (auto-flush ativado)
             PrintWriter serverOut = new PrintWriter(
                 socket.getOutputStream(), true);
             // Canal de leitura padrão (teclado) para ler mensagens digitadas pelo usuário
             BufferedReader userIn = new BufferedReader(
                 new InputStreamReader(System.in))
        ) {
            // Primeiro, o servidor solicita o nome do usuário
            System.out.println(serverIn.readLine());
            // Envia o nome digitado ao servidor
            serverOut.println(user);

            // Thread separada para escutar e exibir mensagens que chegam do servidor
            new Thread(() -> {
                try {
                    String msg;
                    // Enquanto houver mensagens do servidor, imprime no console
                    while ((msg = serverIn.readLine()) != null) {
                        System.out.println(msg);
                    }

                    // Se o loop terminar, significa que o servidor encerrou a conexão
                    System.out.println("Conexão encerrada pelo servidor.");
                } catch (IOException e) {
                    // E, caso de erro, imprime mensagem de erro
                    System.out.println("Conexão encerrada.");
                } finally {
                    // Encerra o cliente completamente
                    System.exit(0);
                }
            }).start();

            // Loop principal: lê linhas digitadas pelo usuário e envia ao servidor
            String line;
            while ((line = userIn.readLine()) != null) {
                serverOut.println(line);
                // Comando '/exit' encerra a sessão do cliente
                if ("/exit".equalsIgnoreCase(line)) break;
            }
        }
        // Ao sair do try, todos os recursos (socket e readers) são fechados automaticamente
    }
}
