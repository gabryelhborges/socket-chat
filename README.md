📬 Aplicativo de Chat Multi-Cliente em Java
Bem-vindo ao Aplicativo de Chat Multi-Cliente, uma aplicação robusta desenvolvida em Java que permite comunicação em tempo real entre múltiplos usuários, com suporte a mensagens privadas, chats em grupo, autenticação de usuários e mensagens offline. Utilizando sockets TCP para comunicação confiável e MySQL para persistência de dados, esta aplicação é ideal para aprendizado e experimentação com sistemas distribuídos e concorrência.

✨ Funcionalidades
Este aplicativo oferece um conjunto completo de funcionalidades para uma experiência de chat interativa e dinâmica:
👤 Gerenciamento de Usuários

Registro: Crie uma conta com nome completo, login único, email e senha.
Login/Logout: Autentique-se para entrar ou sair da aplicação.
Atualização de Status: Defina seu status como "online", "offline", "ocupado" ou "ausente".
Recuperação de Senha: Recupere sua senha fornecendo o email registrado (em produção, recomenda-se redefinição de senha).
Lista de Usuários: Veja todos os usuários registrados e seus status atuais.

💬 Mensagens

Mensagens Privadas (Um-para-Um):
Envie mensagens privadas para outro usuário.
Solicitação inicial de chat com aceitação ou recusa pelo destinatário.
Confirmação de entrega (entregue, enfileirada para offline ou recusada).


Mensagens em Grupo:
Envie mensagens para todos os membros de um grupo.
Mensagens são identificadas com remetente e timestamp (e.g., João (2025-05-25 10:40:00): Olá!).


Mensagens Direcionadas em Grupo:
Envie mensagens para usuários específicos em um grupo (e.g., MSG_GRUPO_ALVO meugrupo maria,joao Oi!).


Mensagens Privadas no Contexto de Grupo:
Envie mensagens privadas a um membro de um grupo sem sair do contexto (e.g., MSG_GRUPO_PRIVADA meugrupo@maria Oi Maria!).


Mensagens Offline:
Mensagens para usuários offline são armazenadas e entregues quando o usuário faz login.



👥 Gerenciamento de Grupos

Criar Grupo: Qualquer usuário pode criar um grupo e se tornar seu administrador.
Adicionar Membros: O criador do grupo pode convidar outros usuários, que devem aceitar o convite.
Solicitar Entrada em Grupo: Usuários podem solicitar entrada em grupos existentes, com aprovação necessária de todos os membros atuais.
Sair de Grupo: Saia de um grupo, notificando todos os membros.
Lista de Grupos: Veja todos os grupos disponíveis na aplicação.

⚙️ Concorrência e Escalabilidade

Servidor Multi-Thread: Cada cliente conectado é gerenciado por uma thread dedicada, permitindo múltiplas conexões simultâneas.
Cliente Não-Bloqueante: Threads separadas para envio e recebimento de mensagens garantem uma interface responsiva.

🗄️ Persistência de Dados

Todas as informações de usuários, grupos, membros e mensagens offline são armazenadas em um banco de dados MySQL, garantindo persistência e recuperação de dados.


🛠️ Configuração e Instalação
Pré-requisitos

Java 8 ou superior: Certifique-se de ter o JDK instalado.
MySQL: Banco de dados MySQL rodando localmente em localhost:3306.
Driver JDBC do MySQL: Baixe o mysql-connector-java.jar para conectar ao banco.
Ambiente de Desenvolvimento: Qualquer IDE (como IntelliJ ou Eclipse) ou terminal com javac e java.

Passos para Configuração

Configurar o Banco de Dados:

Crie o banco de dados app_chat no MySQL.

Execute o script SQL abaixo para criar as tabelas necessárias:
CREATE DATABASE app_chat;
USE app_chat;

CREATE TABLE usuarios (
    login VARCHAR(50) PRIMARY KEY,
    nome_completo VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'offline'
);

CREATE TABLE grupos (
    nome_grupo VARCHAR(50) PRIMARY KEY,
    login_criador VARCHAR(50),
    FOREIGN KEY (login_criador) REFERENCES usuarios(login)
);

CREATE TABLE membros_grupo (
    nome_grupo VARCHAR(50),
    login_usuario VARCHAR(50),
    PRIMARY KEY (nome_grupo, login_usuario),
    FOREIGN KEY (nome_grupo) REFERENCES grupos(nome_grupo),
    FOREIGN KEY (login_usuario) REFERENCES usuarios(login)
);

CREATE TABLE mensagens_offline (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login_remetente VARCHAR(50),
    login_destinatario VARCHAR(50),
    nome_grupo VARCHAR(50),
    conteudo_mensagem TEXT,
    data_hora DATETIME,
    FOREIGN KEY (login_remetente) REFERENCES usuarios(login),
    FOREIGN KEY (login_destinatario) REFERENCES usuarios(login),
    FOREIGN KEY (nome_grupo) REFERENCES grupos(nome_grupo)
);

CREATE TABLE solicitacoes_entrada_grupo (
    nome_grupo VARCHAR(50),
    login_usuario VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pendente',
    PRIMARY KEY (nome_grupo, login_usuario),
    FOREIGN KEY (nome_grupo) REFERENCES grupos(nome_grupo),
    FOREIGN KEY (login_usuario) REFERENCES usuarios(login)
);


Certifique-se de que o MySQL está configurado com usuário root e sem senha, ou atualize as credenciais em ServidorChat.java (USUARIO_BANCO e SENHA_BANCO).



Baixar o Driver JDBC:

Faça o download do mysql-connector-java.jar de um repositório confiável (como o site oficial da Oracle ou Maven Central).
Coloque o arquivo no diretório do projeto.


Compilar e Executar o Servidor:

Compile o arquivo ServidorChat.java:javac -cp .:mysql-connector-java.jar ServidorChat.java


Execute o servidor:java -cp .:mysql-connector-java.jar ServidorChat




Compilar e Executar o Cliente:

Compile o arquivo ClienteChat.java:javac ClienteChat.java


Execute o cliente (você pode abrir várias instâncias para simular múltiplos usuários):java ClienteChat




Observações:

O servidor deve estar rodando antes de iniciar os clientes.
Para fechar um cliente, digite sair no terminal.
O servidor roda em localhost na porta 5000 por padrão. Modifique PORTA em ServidorChat.java e ClienteChat.java se necessário.




🚀 Como Usar
A aplicação utiliza uma interface de linha de comando com comandos baseados em texto. Abaixo estão os comandos disponíveis e exemplos de uso.
Comandos Disponíveis

REGISTRAR <nome_completo> <login> <email> <senha>: Registra um novo usuário.
ENTRAR <login> <senha>: Faz login na aplicação.
SAIR: Faz logout.
STATUS <novo_status>: Atualiza o status do usuário (e.g., online, ocupado, ausente).
LISTAR_USUARIOS: Lista todos os usuários e seus status.
LISTAR_GRUPOS: Lista todos os grupos disponíveis.
MSG_USUARIO <login_destinatario> <mensagem>: Envia uma solicitação de chat privado.
ACEITAR_CHAT <login_remetente>: Aceita uma solicitação de chat privado.
RECUSAR_CHAT <login_remetente>: Recusa uma solicitação de chat privado.
PMSG <login_destinatario> <mensagem>: Envia uma mensagem privada após aceitação.
CRIAR_GRUPO <nome_grupo>: Cria um novo grupo.
ADICIONAR_A_GRUPO <nome_grupo> <login_usuario>: Convida um usuário para um grupo.
SOLICITAR_ENTRADA_GRUPO <nome_grupo>: Solicita entrada em um grupo existente.
RESPOSTA_CONVITE_GRUPO <nome_grupo> <sim/não>: Aceita ou recusa um convite de grupo.
VOTAR_ENTRADA_GRUPO <nome_grupo> <login_solicitante> <sim/não>: Vota na entrada de um usuário em um grupo.
MSG_GRUPO <nome_grupo> <mensagem>: Envia uma mensagem para todos os membros do grupo.
MSG_GRUPO_ALVO <nome_grupo> <usuario1,usuario2> <mensagem>: Envia uma mensagem para usuários específicos no grupo.
MSG_GRUPO_PRIVADA <nome_grupo>@<login_usuario> <mensagem>: Envia uma mensagem privada a um membro do grupo.
SAIR_GRUPO <nome_grupo>: Sai de um grupo.

Exemplo de Interação

Registrar e Entrar:
> REGISTRAR João Silva joaosilva joao@exemplo.com senha123
OK Registrado com sucesso
> ENTRAR joaosilva senha123
OK Entrada bem-sucedida


Criar e Gerenciar Grupos:
> CRIAR_GRUPO meugrupo
OK Grupo meugrupo criado
> ADICIONAR_A_GRUPO meugrupo mariasilva
OK Convite enviado para mariasilva

(No cliente da Maria):
CONVITE_GRUPO meugrupo joaosilva
> RESPOSTA_CONVITE_GRUPO meugrupo sim
OK Entrou no grupo meugrupo


Enviar Mensagens:
> MSG_GRUPO meugrupo Olá a todos!
OK Mensagem de grupo enviada para meugrupo

(Membros do grupo recebem):
NOVA_MSG_GRUPO meugrupo joaosilva 2025-05-25 10:40:00.0 Olá a todos!


Mensagem Privada:
> PMSG mariasilva Oi Maria!
OK Mensagem privada enviada para mariasilva

(Maria recebe):
NOVA_MSG joaosilva Oi Maria!




🔍 Notas e Limitações

Segurança: As senhas são armazenadas em texto puro para simplicidade. Em um ambiente de produção, use hash (e.g., BCrypt) e implemente criptografia para comunicações.
Escalabilidade: O modelo de thread por cliente é adequado para pequenos grupos, mas para milhares de usuários, considere usar Java NIO ou um pool de threads.
Tratamento de Erros: Inclui validação básica; adicione logging e validação mais robusta para cenários reais.
Votação em Grupos: Simplificado para exigir aprovação unânime. Um sistema completo rastrearia votos individuais.
Interface: A interface de linha de comando.