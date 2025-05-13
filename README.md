
# ChatMSN - Chat TCP em Java

## Descrição do Projeto
O **ChatMSN** é uma aplicação de chat em tempo real construída em Java, utilizando **sockets TCP** para comunicação entre múltiplos clientes e um servidor central. Inspirado em aplicações de mensagens como WhatsApp, permite:

- Troca de mensagens em grupo entre diversos clientes.
- Notificações de entrada e saída de usuários.
- Exibição de horário de entrada, saída e duração da sessão de cada usuário.  
- Comando administrativo `list`/`lista` para visualizar clientes online e seus IPs.  
- Estrutura thread-safe com `CopyOnWriteArrayList` para gerenciar handlers de clientes.
  
### Estrutura do Código
```
chatmsn-app/
├── pom.xml
├── README.md
├── src/
│   └── main/java/com/chatmsn/
│       ├── ChatServer.java
│       └── ChatClient.java
└── target/
├── chat-app-1.0.0-server.jar
├── chat-app-1.0.0-client.jar
└── chat-app-1.0.0.jar
```

### ChatServer.java
Responsável por:
- Abrir `ServerSocket` na porta configurada.  
- Solicita nome ao cliente, registrar horário de conexão e iniciar `ClientHandler`.  
- Gerenciar lista de clientes com `CopyOnWriteArrayList<ClientHandler>`.  
- Fornecer comando `list`/`lista` no console do servidor para listar clientes online e IPs.  
- Realizar broadcast de mensagens de chat, notificações de entrada, saída e duração da sessão.

### ChatClient.java
Responsável por:
- Conectar-se ao servidor via `Socket(host, port)`.  
- Receber notificações de conexão de entrada de usuário.  
- Receber prompt `DIGITE SEU NOME:`, enviar apelido e entrar no chat.  
- Manter uma thread separada para leitura contínua de mensagens do servidor.  
- Enviar mensagens de texto digitadas pelo usuário.  
- Encerrar conexão ao digitar `/exit`.

## Funcionalidades Principais
- **Broadcast**: mensagens enviadas por um cliente são replicadas a todos os demais.  
- **Logs no Servidor**: datas e horários de entrada, saída, duração e conteúdo de todas as mensagens são exibidos no terminal do servidor.  
- **Comando Administrativo**: no console do servidor, o administrador pode digitar `list` ou `lista` para ver quem está online, junto ao IP de cada cliente.

## Pré-requisitos
- Java 11+ (JRE ou JDK) instalado.  
- Maven para compilação e empacotamento.

## Como Compilar e Empacotar

1. Clone o repositório para ter acesso ao servidor e cliente:
   ```
   git clone https://github.com/seu-usuario/chatmsn-app.git
   cd chatmsn-app
   ```
   
2. Compile e gere os fat-jars:
   
   ```
   mvn clean package
  
   * Será gerado em `target/`:
   * `chat-app-1.0.0-server.jar`  (servidor)
   * `chat-app-1.0.0-client.jar`  (cliente)
   * `chat-app-1.0.0.jar`         (sem deps)
## Como Executar

### Servidor

```
# Usando o fat-jar do servidor
java -jar target/chat-app-1.0.0-server.jar
```

No terminal do servidor, serão exibidas mensagens de log e o prompt:

```
Servidor iniciado na porta 12345
```

### Cliente

Em outra máquina ou aba de terminal, execute:

```
java -jar target/chat-app-1.0.0-client.jar <IP_SERVIDOR> 12345 SeuApelido
```

Fluxo no cliente:

```
DIGITE SEU NOME:
```

Após digitar o nome, basta enviar mensagens e ver as dos outros.

Para sair, digite:

```
/exit
```

## Comando Administrativo

No terminal do servidor, digite:

```
list ou lista
```

Será exibido os usuários como no exemplo:

```
=== Clientes online ===
Alice - 192.168.100.115
Bob   - 192.168.100.120
=======================
```
Para encerrar o servidor basta apertar `Ctrl + C`.

## Desenvolvido por:
[Kethelen Parra](https://github.com/KethelenParra) e [Gabriel Mussatto](https://github.com/GabrielMussatto).
