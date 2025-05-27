package org.example;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorChat {
    private static final int PORTA = 5000;
    private static final String URL_BANCO = "jdbc:mysql://localhost:3306/app_chat";
    private static final String USUARIO_BANCO = "root";
    private static final String SENHA_BANCO = "";
    private static final Map<String, ManipuladorCliente> clientes = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> grupos = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> solicitacoes_entrada = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket servidorSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor iniciado na porta " + PORTA);
            while (true) {
                Socket socketCliente = servidorSocket.accept();
                ManipuladorCliente manipuladorCliente = new ManipuladorCliente(socketCliente);
                new Thread(manipuladorCliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ManipuladorCliente implements Runnable {
        private Socket socket;
        private PrintWriter saida;
        private BufferedReader entrada;
        private String login;
        private Connection conexaoBanco;

        public ManipuladorCliente(Socket socket) {
            this.socket = socket;
            try {
                this.saida = new PrintWriter(socket.getOutputStream(), true);
                this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.conexaoBanco = DriverManager.getConnection(URL_BANCO, USUARIO_BANCO, SENHA_BANCO);
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String comando = entrada.readLine();
                    if (comando == null) break;
                    processarComando(comando);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (login != null) {
                    atualizarStatusUsuario(login, "offline");
                    clientes.remove(login);
                    transmitirStatusUsuario(login, "offline");
                }
                try {
                    socket.close();
                    conexaoBanco.close();
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processarComando(String comando) throws IOException {
            String[] partes = comando.split(" ", 2);
            String acao = partes[0];
            String argumentos = partes.length > 1 ? partes[1] : "";

            switch (acao) {
                case "REGISTRAR":
                    manipularRegistro(argumentos);
                    break;
                case "ENTRAR":
                    manipularEntrada(argumentos);
                    break;
                case "SAIR":
                    manipularSaida();
                    break;
                case "STATUS":
                    manipularStatus(argumentos);
                    break;
                case "LISTAR_USUARIOS":
                    manipularListarUsuarios();
                    break;
                case "LISTAR_GRUPOS":
                    manipularListarGrupos();
                    break;
                case "MSG_USUARIO":
                    manipularMensagemUsuario(argumentos);
                    break;
                case "ACEITAR_CHAT":
                    if (argumentos.isEmpty()) {
                        saida.println("ERRO Formato inválido. Use ACEITAR_CHAT nomeUsuario");
                    } else {
                        manipularAceitarChat(argumentos);
                    }
                    break;
                case "RECUSAR_CHAT":
                    if (argumentos.isEmpty()) {
                        saida.println("ERRO Formato inválido. Use RECUSAR_CHAT nomeUsuario");
                    } else {
                        manipularRecusarChat(argumentos);
                    }
                    break;
                case "PMSG":
                    manipularMensagemPrivada(argumentos);
                    break;
                case "CRIAR_GRUPO":
                    manipularCriarGrupo(argumentos);
                    break;
                case "ADICIONAR_A_GRUPO":
                    manipularAdicionarAGrupo(argumentos);
                    break;
                case "SOLICITAR_ENTRADA_GRUPO":
                    manipularSolicitarEntradaGrupo(argumentos);
                    break;
                case "RESPOSTA_CONVITE_GRUPO":
                    manipularRespostaConviteGrupo(argumentos);
                    break;
                case "VOTAR_ENTRADA_GRUPO":
                    manipularVotarEntradaGrupo(argumentos);
                    break;
                case "MSG_GRUPO":
                    manipularMensagemGrupo(argumentos);
                    break;
                case "MSG_GRUPO_ALVO":
                    manipularMensagemGrupoAlvo(argumentos);
                    break;
                case "MSG_GRUPO_PRIVADA":
                    manipularMensagemGrupoPrivada(argumentos);
                    break;
                case "SAIR_GRUPO":
                    manipularSairGrupo(argumentos);
                    break;
                case "LISTAR_AMIZADES":
                    listarAmizades();
                    break;
                default:
                    saida.println("ERRO Comando desconhecido");
            }
        }

        private void manipularRegistro(String argumentos) {
            String[] args = argumentos.split(" ", 4);
            if (args.length != 4) {
                saida.println("ERRO Formato de registro inválido");
                return;
            }
            String nomeCompleto = args[0];
            String login = args[1];
            String email = args[2];
            String senha = args[3];

            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "INSERT INTO usuarios (nome_completo, login, email, senha) VALUES (?, ?, ?, ?)");
                stmt.setString(1, nomeCompleto);
                stmt.setString(2, login);
                stmt.setString(3, email);
                stmt.setString(4, senha);
                stmt.executeUpdate();
                saida.println("OK Registrado com sucesso");
            } catch (SQLException e) {
                saida.println("ERRO Falha no registro: " + e.getMessage());
            }
        }

        private void manipularEntrada(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de login inválido");
                return;
            }
            String login = args[0];
            String senha = args[1];

            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT * FROM usuarios WHERE login = ? AND senha = ?");
                stmt.setString(1, login);
                stmt.setString(2, senha);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    if (clientes.containsKey(login)) {
                        saida.println("ERRO Usuário já está logado");
                        return;
                    }
                    this.login = login;
                    clientes.put(login, this);
                    atualizarStatusUsuario(login, "online");
                    transmitirStatusUsuario(login, "online");
                    saida.println("OK Entrada bem-sucedida");
                    enviarMensagensOffline(login);
                } else {
                    saida.println("ERRO Credenciais inválidas");
                }
            } catch (SQLException e) {
                saida.println("ERRO Falha na entrada: " + e.getMessage());
            }
        }

        private void manipularSaida() {
            if (login != null) {
                atualizarStatusUsuario(login, "offline");
                clientes.remove(login);
                transmitirStatusUsuario(login, "offline");
                saida.println("OK Deslogado");
            } else {
                saida.println("ERRO Não está logado");
            }
        }

        private void manipularStatus(String status) {
            if (login == null) {
                saida.println("ERRO Não está logado");
                return;
            }
            atualizarStatusUsuario(login, status);
            transmitirStatusUsuario(login, status);
            saida.println("OK Status atualizado para " + status);
        }

        private void manipularListarUsuarios() {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement("SELECT login, status FROM usuarios");
                ResultSet rs = stmt.executeQuery();
                StringBuilder resposta = new StringBuilder("OK Usuários:\n");
                while (rs.next()) {
                    resposta.append(rs.getString("login")).append(" (").append(rs.getString("status")).append(")\n");
                }
                saida.println(resposta.toString());
            } catch (SQLException e) {
                saida.println("ERRO Falha ao listar usuários: " + e.getMessage());
            }
        }

        private void manipularListarGrupos() {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement("SELECT nome_grupo FROM grupos");
                ResultSet rs = stmt.executeQuery();
                StringBuilder resposta = new StringBuilder("OK Grupos:\n");
                while (rs.next()) {
                    resposta.append(rs.getString("nome_grupo")).append("\n");
                }
                saida.println(resposta.toString());
            } catch (SQLException e) {
                saida.println("ERRO Falha ao listar grupos: " + e.getMessage());
            }
        }

        private void manipularMensagemUsuario(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de mensagem inválido");
                return;
            }
            String destinatario = args[0];
            String mensagem = args[1];

            // Verificar se os usuários são amigos
            try {
                if (verificarAmizade(login, destinatario)) {
                    // Se são amigos, enviar mensagem normalmente
                    ManipuladorCliente manipuladorDestinatario = clientes.get(destinatario);
                    if (manipuladorDestinatario != null) {
                        manipuladorDestinatario.saida.println("NOVA_MSG " + login + " " + mensagem);
                        saida.println("OK Mensagem enviada para " + destinatario);
                    } else {
                        armazenarMensagemOffline(login, destinatario, null, mensagem);
                        saida.println("OK Mensagem enfileirada para usuário offline " + destinatario);
                    }
                } else {
                    // Se não são amigos, enviar solicitação de contato
                    ManipuladorCliente manipuladorDestinatario = clientes.get(destinatario);
                    if (manipuladorDestinatario != null) {
                        // Criar a solicitação de contato no banco de dados
                        criarSolicitacaoAmizade(login, destinatario);
                        manipuladorDestinatario.saida.println("SOLICITACAO_CONTATO " + login + " " + mensagem);
                        saida.println("OK Solicitação de contato enviada para " + destinatario);
                    } else {
                        // Se o destinatário estiver offline, criar a solicitação e armazenar
                        criarSolicitacaoAmizade(login, destinatario);
                        saida.println("OK Solicitação de contato enfileirada para usuário offline " + destinatario);
                    }
                }
            } catch (SQLException e) {
                saida.println("ERRO Falha ao verificar amizade: " + e.getMessage());
            }
        }

        private void manipularAceitarChat(String remetente) {
            try {
                // Verificar primeiro se o remetente existe no banco de dados
                PreparedStatement checkUserStmt = conexaoBanco.prepareStatement(
                        "SELECT login FROM usuarios WHERE login = ?");
                checkUserStmt.setString(1, remetente);
                ResultSet userRs = checkUserStmt.executeQuery();
                
                if (!userRs.next()) {
                    saida.println("ERRO Usuário " + remetente + " não existe no sistema");
                    return;
                }
                
                // Verificar se existe uma solicitação pendente
                PreparedStatement checkStmt = conexaoBanco.prepareStatement(
                        "SELECT * FROM amizades WHERE " +
                        "((usuario1 = ? AND usuario2 = ?) OR (usuario1 = ? AND usuario2 = ?)) " +
                        "AND status = 'pendente'");
                checkStmt.setString(1, remetente);
                checkStmt.setString(2, login);
                checkStmt.setString(3, login);
                checkStmt.setString(4, remetente);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    // Atualizar status da amizade para aceita
                    PreparedStatement updateStmt = conexaoBanco.prepareStatement(
                            "UPDATE amizades SET status = 'aceita' WHERE " +
                            "((usuario1 = ? AND usuario2 = ?) OR (usuario1 = ? AND usuario2 = ?)) " +
                            "AND status = 'pendente'");
                    updateStmt.setString(1, remetente);
                    updateStmt.setString(2, login);
                    updateStmt.setString(3, login);
                    updateStmt.setString(4, remetente);
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // Notificar o remetente se estiver online
                        ManipuladorCliente manipuladorRemetente = clientes.get(remetente);
                        if (manipuladorRemetente != null) {
                            manipuladorRemetente.saida.println("CONTATO_ACEITO " + login);
                        }
                        saida.println("OK Chat aceito com " + remetente);
                    } else {
                        saida.println("ERRO Falha ao atualizar status da amizade");
                    }
                } else {
                    // Se não existir uma solicitação pendente, retornar erro
                    saida.println("ERRO Não existe solicitação de contato pendente de " + remetente);
                }
            } catch (SQLException e) {
                saida.println("ERRO Falha ao aceitar contato: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void manipularRecusarChat(String remetente) {
            try {
                // Atualizar status da amizade para recusada
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "UPDATE amizades SET status = 'recusada' WHERE " +
                        "(usuario1 = ? AND usuario2 = ?) OR (usuario1 = ? AND usuario2 = ?)");
                stmt.setString(1, remetente);
                stmt.setString(2, login);
                stmt.setString(3, login);
                stmt.setString(4, remetente);
                stmt.executeUpdate();
                
                // Notificar o remetente se estiver online
                ManipuladorCliente manipuladorRemetente = clientes.get(remetente);
                if (manipuladorRemetente != null) {
                    manipuladorRemetente.saida.println("CONTATO_RECUSADO " + login);
                }
                
                saida.println("OK Chat recusado com " + remetente);
            } catch (SQLException e) {
                saida.println("ERRO Falha ao recusar contato: " + e.getMessage());
            }
        }

        // Método para verificar se dois usuários são amigos
        private boolean verificarAmizade(String usuario1, String usuario2) throws SQLException {
            PreparedStatement stmt = conexaoBanco.prepareStatement(
                    "SELECT status FROM amizades WHERE " +
                    "((usuario1 = ? AND usuario2 = ?) OR (usuario1 = ? AND usuario2 = ?)) " +
                    "AND status = 'aceita'");
            stmt.setString(1, usuario1);
            stmt.setString(2, usuario2);
            stmt.setString(3, usuario2);
            stmt.setString(4, usuario1);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Se retornar algum resultado, são amigos
            }
        }

        // Método para criar uma solicitação de amizade no banco
        private void criarSolicitacaoAmizade(String solicitante, String solicitado) throws SQLException {
            // Verificar se o solicitado existe
            PreparedStatement checkUserStmt = conexaoBanco.prepareStatement(
                    "SELECT login FROM usuarios WHERE login = ?");
            checkUserStmt.setString(1, solicitado);
            ResultSet userRs = checkUserStmt.executeQuery();
            
            if (!userRs.next()) {
                throw new SQLException("Usuário destinatário não existe no sistema");
            }
            
            // Verificar se já existe uma solicitação
            PreparedStatement checkStmt = conexaoBanco.prepareStatement(
                    "SELECT * FROM amizades WHERE " +
                    "(usuario1 = ? AND usuario2 = ?) OR (usuario1 = ? AND usuario2 = ?)");
            checkStmt.setString(1, solicitante);
            checkStmt.setString(2, solicitado);
            checkStmt.setString(3, solicitado);
            checkStmt.setString(4, solicitante);
            ResultSet rs = checkStmt.executeQuery();
            
            // Se não existir, criar uma nova
            if (!rs.next()) {
                PreparedStatement insertStmt = conexaoBanco.prepareStatement(
                        "INSERT INTO amizades (usuario1, usuario2, status) VALUES (?, ?, 'pendente')");
                insertStmt.setString(1, solicitante);
                insertStmt.setString(2, solicitado);
                insertStmt.executeUpdate();
            }
        }

        // Método para manipular mensagens privadas (reaproveitando a lógica)
        private void manipularMensagemPrivada(String argumentos) {
            // Reaproveitando a mesma lógica que manipularMensagemUsuario
            manipularMensagemUsuario(argumentos);
        }

        private void manipularCriarGrupo(String nomeGrupo) {
            if (login == null) {
                saida.println("ERRO Não está logado");
                return;
            }
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "INSERT INTO grupos (nome_grupo, login_criador) VALUES (?, ?)");
                stmt.setString(1, nomeGrupo);
                stmt.setString(2, login);
                stmt.executeUpdate();

                stmt = conexaoBanco.prepareStatement(
                        "INSERT INTO membros_grupo (nome_grupo, login_usuario) VALUES (?, ?)");
                stmt.setString(1, nomeGrupo);
                stmt.setString(2, login);
                stmt.executeUpdate();

                grupos.computeIfAbsent(nomeGrupo, k -> new HashSet<>()).add(login);
                saida.println("OK Grupo " + nomeGrupo + " criado");
            } catch (SQLException e) {
                saida.println("ERRO Falha ao criar grupo: " + e.getMessage());
            }
        }

        private void manipularAdicionarAGrupo(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de adição a grupo inválido");
                return;
            }
            String nomeGrupo = args[0];
            String loginUsuario = args[1];

            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT * FROM grupos WHERE nome_grupo = ? AND login_criador = ?");
                stmt.setString(1, nomeGrupo);
                stmt.setString(2, login);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    saida.println("ERRO Não autorizado a adicionar ao grupo ou grupo não existe");
                    return;
                }

                ManipuladorCliente manipuladorUsuario = clientes.get(loginUsuario);
                if (manipuladorUsuario != null) {
                    manipuladorUsuario.saida.println("CONVITE_GRUPO " + nomeGrupo + " " + login);
                    solicitacoes_entrada.computeIfAbsent(nomeGrupo, k -> new HashMap<>()).put(loginUsuario, "pendente");
                    saida.println("OK Convite enviado para " + loginUsuario);
                } else {
                    saida.println("ERRO Usuário " + loginUsuario + " está offline");
                }
            } catch (SQLException e) {
                saida.println("ERRO Falha ao adicionar ao grupo: " + e.getMessage());
            }
        }

        private void manipularSolicitarEntradaGrupo(String nomeGrupo) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT login_usuario FROM membros_grupo WHERE nome_grupo = ?");
                stmt.setString(1, nomeGrupo);
                ResultSet rs = stmt.executeQuery();
                Set<String> membros = new HashSet<>();
                while (rs.next()) {
                    membros.add(rs.getString("login_usuario"));
                }

                solicitacoes_entrada.computeIfAbsent(nomeGrupo, k -> new HashMap<>()).put(login, "pendente");
                for (String membro : membros) {
                    ManipuladorCliente manipuladorMembro = clientes.get(membro);
                    if (manipuladorMembro != null) {
                        manipuladorMembro.saida.println("SOLICITACAO_ENTRADA_GRUPO " + nomeGrupo + " " + login);
                    }
                }
                saida.println("OK Solicitação de entrada enviada para o grupo " + nomeGrupo);
            } catch (SQLException e) {
                saida.println("ERRO Falha ao solicitar entrada: " + e.getMessage());
            }
        }

        private void manipularRespostaConviteGrupo(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de resposta de convite inválido");
                return;
            }
            String nomeGrupo = args[0];
            String resposta = args[1];

            if (resposta.equalsIgnoreCase("sim")) {
                try {
                    PreparedStatement stmt = conexaoBanco.prepareStatement(
                            "INSERT INTO membros_grupo (nome_grupo, login_usuario) VALUES (?, ?)");
                    stmt.setString(1, nomeGrupo);
                    stmt.setString(2, login);
                    stmt.executeUpdate();
                    grupos.computeIfAbsent(nomeGrupo, k -> new HashSet<>()).add(login);
                    saida.println("OK Entrou no grupo " + nomeGrupo);
                    transmitirMensagemGrupo(nomeGrupo, login, login + " entrou no grupo");
                } catch (SQLException e) {
                    saida.println("ERRO Falha ao entrar no grupo: " + e.getMessage());
                }
            } else {
                saida.println("OK Convite para o grupo " + nomeGrupo + " recusado");
            }
        }

        private void manipularVotarEntradaGrupo(String argumentos) {
            String[] args = argumentos.split(" ", 3);
            if (args.length != 3) {
                saida.println("ERRO Formato de votação de entrada inválido");
                return;
            }
            String nomeGrupo = args[0];
            String usuarioSolicitante = args[1];
            String voto = args[2];

            Map<String, String> solicitacoes = solicitacoes_entrada.get(nomeGrupo);
            if (solicitacoes == null || !solicitacoes.containsKey(usuarioSolicitante)) {
                saida.println("ERRO Nenhuma solicitação pendente para " + usuarioSolicitante);
                return;
            }

            if (voto.equalsIgnoreCase("não")) {
                solicitacoes.remove(usuarioSolicitante);
                ManipuladorCliente solicitante = clientes.get(usuarioSolicitante);
                if (solicitante != null) {
                    solicitante.saida.println("ERRO Solicitação de entrada para " + nomeGrupo + " rejeitada");
                }
                saida.println("OK Voto registrado: rejeitado");
            } else {
                try {
                    PreparedStatement stmt = conexaoBanco.prepareStatement(
                            "SELECT login_usuario FROM membros_grupo WHERE nome_grupo = ?");
                    stmt.setString(1, nomeGrupo);
                    ResultSet rs = stmt.executeQuery();
                    Set<String> membros = new HashSet<>();
                    while (rs.next()) {
                        membros.add(rs.getString("login_usuario"));
                    }

                    boolean todosAprovaram = true;
                    // Simplificado: assume que todos os membros devem votar sim
                    if (todosAprovaram) {
                        stmt = conexaoBanco.prepareStatement(
                                "INSERT INTO membros_grupo (nome_grupo, login_usuario) VALUES (?, ?)");
                        stmt.setString(1, nomeGrupo);
                        stmt.setString(2, usuarioSolicitante);
                        stmt.executeUpdate();
                        grupos.computeIfAbsent(nomeGrupo, k -> new HashSet<>()).add(usuarioSolicitante);
                        ManipuladorCliente solicitante = clientes.get(usuarioSolicitante);
                        if (solicitante != null) {
                            solicitante.saida.println("OK Entrou no grupo " + nomeGrupo);
                        }
                        transmitirMensagemGrupo(nomeGrupo, usuarioSolicitante, usuarioSolicitante + " entrou no grupo");
                        solicitacoes.remove(usuarioSolicitante);
                    }
                    saida.println("OK Voto registrado");
                } catch (SQLException e) {
                    saida.println("ERRO Falha ao processar voto: " + e.getMessage());
                }
            }
        }

        private void manipularMensagemGrupo(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de mensagem de grupo inválido");
                return;
            }
            String nomeGrupo = args[0];
            String mensagem = args[1];

            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT login_usuario FROM membros_grupo WHERE nome_grupo = ?");
                stmt.setString(1, nomeGrupo);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String membro = rs.getString("login_usuario");
                    ManipuladorCliente manipuladorMembro = clientes.get(membro);
                    if (manipuladorMembro != null) {
                        manipuladorMembro.saida.println("NOVA_MSG_GRUPO " + nomeGrupo + " " + login + " " + obterDataHoraAtual() + " " + mensagem);
                    } else {
                        armazenarMensagemOffline(login, membro, nomeGrupo, mensagem);
                    }
                }
                saida.println("OK Mensagem de grupo enviada para " + nomeGrupo);
            } catch (SQLException e) {
                saida.println("ERRO Falha ao enviar mensagem de grupo: " + e.getMessage());
            }
        }

        private void manipularMensagemGrupoAlvo(String argumentos) {
            String[] args = argumentos.split(" ", 3);
            if (args.length != 3) {
                saida.println("ERRO Formato de mensagem de grupo alvo inválido");
                return;
            }
            String nomeGrupo = args[0];
            String destinatarios = args[1];
            String mensagem = args[2];

            String[] listaDestinatarios = destinatarios.split(",");
            for (String destinatario : listaDestinatarios) {
                destinatario = destinatario.trim();
                ManipuladorCliente manipuladorDestinatario = clientes.get(destinatario);
                if (manipuladorDestinatario != null) {
                    manipuladorDestinatario.saida.println("NOVA_MSG_GRUPO " + nomeGrupo + " " + login + " " + obterDataHoraAtual() + " " + mensagem);
                } else {
                    armazenarMensagemOffline(login, destinatario, nomeGrupo, mensagem);
                }
            }
            saida.println("OK Mensagem de grupo alvo enviada");
        }

        private void manipularMensagemGrupoPrivada(String argumentos) {
            String[] args = argumentos.split(" ", 2);
            if (args.length != 2) {
                saida.println("ERRO Formato de mensagem privada de grupo inválido");
                return;
            }
            String destinatario = args[0];
            String mensagem = args[1];
            String[] partesDestinatario = destinatario.split("@");
            if (partesDestinatario.length != 2) {
                saida.println("ERRO Formato de destinatário inválido");
                return;
            }
            String nomeGrupo = partesDestinatario[0];
            String loginUsuario = partesDestinatario[1];

            ManipuladorCliente manipuladorDestinatario = clientes.get(loginUsuario);
            if (manipuladorDestinatario != null) {
                manipuladorDestinatario.saida.println("NOVA_MSG " + login + " " + mensagem);
                saida.println("OK Mensagem privada enviada para " + loginUsuario + " no grupo " + nomeGrupo);
            } else {
                armazenarMensagemOffline(login, loginUsuario, nomeGrupo, mensagem);
                saida.println("OK Mensagem privada enfileirada para usuário offline " + loginUsuario);
            }
        }

        private void manipularSairGrupo(String nomeGrupo) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "DELETE FROM membros_grupo WHERE nome_grupo = ? AND login_usuario = ?");
                stmt.setString(1, nomeGrupo);
                stmt.setString(2, login);
                stmt.executeUpdate();

                grupos.getOrDefault(nomeGrupo, new HashSet<>()).remove(login);
                transmitirMensagemGrupo(nomeGrupo, login, login + " saiu do grupo");
                saida.println("OK Saiu do grupo " + nomeGrupo);
            } catch (SQLException e) {
                saida.println("ERRO Falha ao sair do grupo: " + e.getMessage());
            }
        }

        private void atualizarStatusUsuario(String login, String status) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "UPDATE usuarios SET status = ? WHERE login = ?");
                stmt.setString(1, status);
                stmt.setString(2, login);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void transmitirStatusUsuario(String login, String status) {
            for (ManipuladorCliente cliente : clientes.values()) {
                cliente.saida.println("ATUALIZACAO_STATUS_USUARIO " + login + " " + status);
            }
        }

        private void transmitirMensagemGrupo(String nomeGrupo, String remetente, String mensagem) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT login_usuario FROM membros_grupo WHERE nome_grupo = ?");
                stmt.setString(1, nomeGrupo);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String membro = rs.getString("login_usuario");
                    ManipuladorCliente manipuladorMembro = clientes.get(membro);
                    if (manipuladorMembro != null) {
                        manipuladorMembro.saida.println("NOVA_MSG_GRUPO " + nomeGrupo + " " + remetente + " " + obterDataHoraAtual() + " " + mensagem);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void armazenarMensagemOffline(String remetente, String destinatario, String nomeGrupo, String mensagem) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "INSERT INTO mensagens_offline (login_remetente, login_destinatario, nome_grupo, conteudo_mensagem, data_hora) VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, remetente);
                stmt.setString(2, destinatario);
                stmt.setString(3, nomeGrupo);
                stmt.setString(4, mensagem);
                stmt.setTimestamp(5, Timestamp.valueOf(obterDataHoraAtual()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void enviarMensagensOffline(String login) {
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT * FROM mensagens_offline WHERE login_destinatario = ?");
                stmt.setString(1, login);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String remetente = rs.getString("login_remetente");
                    String nomeGrupo = rs.getString("nome_grupo");
                    String mensagem = rs.getString("conteudo_mensagem");
                    String dataHora = rs.getString("data_hora");
                    if (nomeGrupo != null) {
                        saida.println("NOVA_MSG_GRUPO " + nomeGrupo + " " + remetente + " " + dataHora + " " + mensagem);
                    } else {
                        saida.println("NOVA_MSG " + remetente + " " + mensagem);
                    }
                }
                stmt = conexaoBanco.prepareStatement(
                        "DELETE FROM mensagens_offline WHERE login_destinatario = ?");
                stmt.setString(1, login);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private String obterDataHoraAtual() {
            return new Timestamp(System.currentTimeMillis()).toString();
        }

        private void listarAmizades() {
            if (login == null) {
                saida.println("ERRO Não está logado");
                return;
            }
            
            try {
                PreparedStatement stmt = conexaoBanco.prepareStatement(
                        "SELECT CASE WHEN usuario1 = ? THEN usuario2 ELSE usuario1 END AS amigo, " +
                        "status FROM amizades WHERE usuario1 = ? OR usuario2 = ?");
                stmt.setString(1, login);
                stmt.setString(2, login);
                stmt.setString(3, login);
                ResultSet rs = stmt.executeQuery();
                
                StringBuilder resposta = new StringBuilder("OK Suas amizades:\n");
                boolean hasAmizades = false;
                
                while (rs.next()) {
                    hasAmizades = true;
                    String amigo = rs.getString("amigo");
                    String status = rs.getString("status");
                    resposta.append(amigo).append(" (").append(status).append(")\n");
                }
                
                if (!hasAmizades) {
                    resposta.append("Você não tem amizades registradas.\n");
                }
                
                saida.println(resposta.toString());
            } catch (SQLException e) {
                saida.println("ERRO Falha ao listar amizades: " + e.getMessage());
            }
        }
    }
}