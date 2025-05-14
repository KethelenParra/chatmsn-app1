# ChatMSN - Chat TCP em Java

## Descrição do Projeto
O **ChatMSN** é uma aplicação de chat em tempo real construída em Java, utilizando **sockets TCP** para comunicação entre múltiplos clientes e um servidor central. Inspirado em aplicações de mensagens como WhatsApp, permite:

- Troca de mensagens em grupo entre diversos clientes.
- Notificações de entrada, saída e expulsões de clientes.
- Exibição de horário de entrada, saída e duração da sessão de cada cliente.
- Exibição de regras ao cliente entrar no chat.
- Comando administrativo `list`/`lista` para visualizar clientes online e seus IPs.
- Comando administrativo `kick (nome do cliente)` para expulsar do servidor.
- Comando administrativo com `/ (mensagem)` para dar avisos aos clientes.
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
├── chat-app-1.0.6-server.jar
├── chat-app-1.0.6-client.jar
└── chat-app-1.0.6.jar
```

### ChatServer.java
Responsável por:
- Abrir `ServerSocket` na porta configurada.  
- Solicita nome ao cliente, registrar horário de conexão e iniciar `ClientHandler`.  
- Gerenciar lista de clientes com `CopyOnWriteArrayList<ClientHandler>`.  
- Fornecer comando `list`/`lista` no console do servidor para listar clientes online e IPs.
- Fornecer comando `kick (nome do cliente)` para expulsar o cliente.
- Fornecer comando `/ (mensagem)` para dar avisos ao clientes online.
- Realizar broadcast de mensagens de chat, notificações de entrada, saída e duração da sessão.

### ChatClient.java
Responsável por:
- Conectar-se ao servidor via `Socket(host, port)`.  
- Receber notificações de conexão de entrada de usuário.  
- Receber prompt `DIGITE SEU NOME:`, enviar apelido e entrar no chat.  
- Manter uma thread separada para leitura contínua de mensagens do servidor.  
- Enviar mensagens de texto digitadas pelo cliente.  
- Encerrar conexão ao digitar `/exit`.

## Funcionalidades Principais
- **Broadcast**: mensagens enviadas por um cliente são replicadas a todos os demais.  
- **Logs no Servidor**: datas e horários de entrada, saída, duração e conteúdo de todas as mensagens são exibidos no terminal do servidor.  
- **Comando Administrativo**: no console do servidor, o administrador pode digitar `list` ou `lista` para ver quem está online, junto ao IP de cada cliente, o administrador pode digitar `kick (nome do cliente)` para expulsar um cliente do chat, o administrador pode digitar `/ (mensagem)` para enviar avisos aos clientes online.

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
   * `chat-app-1.0.x-server.jar`  (servidor)
   * `chat-app-1.0.x-client.jar`  (cliente)
   * `chat-app-1.0.x.jar`         (sem deps)
## Como Executar

### Servidor

```
# Usando o fat-jar do servidor
java -jar target/chat-app-1.0.x-server.jar
```

No terminal do servidor, serão exibidas mensagens de log e o prompt:

```
Servidor iniciado na porta definida
```

### Cliente

Em outra máquina ou aba de terminal, execute:

```
java -jar chat-app-1.0.x-client.jar <IP_SERVIDOR> <porta do servidor> SeuApelido
```

Fluxo no cliente:

```
DIGITE SEU NOME:
```

Após digitar o nome, basta enviar mensagens e ver as mensagens dos outros clientes online.

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

Digite:
```
/ (mensagem)
ex: / Proibido palavras de baixo calão.
```

Será exibido para os clientes:

```
[hh:mm:ss] [Servidor] Proibido palavras de baixo calão.
```


Digite:
```
kick (nome do cliente)
ex: kick Bob
```
Para o cliente que foi expulso, retorna a mensagem da seguinte forma:
```
Você foi expulso pelo servidor.
Conexão encerrada pelo servidor.
```

Após o cliente ser expulso do chat, retorna a mensagem aos clientes online:
```
[hh:mm:ss] Bob foi expulso pelo servidor.
```

Para encerrar o servidor basta apertar `Ctrl + C`.

## Desenvolvido por:
[Kethelen Parra](https://github.com/KethelenParra) e [Gabriel Mussatto](https://github.com/GabrielMussatto).
