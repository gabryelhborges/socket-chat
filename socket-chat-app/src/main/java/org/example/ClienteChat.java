package org.example;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteChat {
    private static final String ENDERECO_SERVIDOR = "localhost";
    private static final int PORTA_SERVIDOR = 5000;
    private Socket socket;
    private PrintWriter saida;
    private BufferedReader entrada;
    private String login;

    public ClienteChat() {
        try {
            socket = new Socket(ENDERECO_SERVIDOR, PORTA_SERVIDOR);
            saida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void iniciar() {
        // Inicia thread receptora
        new Thread(new Receptor()).start();

        // Thread principal gerencia entrada do usuário
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao Aplicativo de Chat!");
        System.out.println("Comandos disponíveis: REGISTRAR, ENTRAR, SAIR, STATUS, LISTAR_USUARIOS, LISTAR_GRUPOS, LISTAR_AMIZADES, " +
                           "MSG_USUARIO, PMSG, CRIAR_GRUPO, ADICIONAR_A_GRUPO, SOLICITAR_ENTRADA_GRUPO, RESPOSTA_CONVITE_GRUPO, " +
                           "VOTAR_ENTRADA_GRUPO, MSG_GRUPO, MSG_GRUPO_ALVO, MSG_GRUPO_PRIVADA, SAIR_GRUPO");

        while (true) {
            System.out.print("> ");
            String entradaUsuario = scanner.nextLine();
            if (entradaUsuario.equalsIgnoreCase("sair")) break;
            saida.println(entradaUsuario);
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Receptor implements Runnable {
        @Override
        public void run() {
            try {
                String mensagem;
                while ((mensagem = entrada.readLine()) != null) {
                    if (mensagem.startsWith("SOLICITACAO_CONTATO")) {
                        // Formato: SOLICITACAO_CONTATO remetente mensagem
                        String[] partes = mensagem.split(" ", 3);
                        String remetente = partes[1];
                        String conteudo = partes.length > 2 ? partes[2] : "";
                        System.out.println("\nSolicitação de contato de " + remetente + ": " + conteudo);
                        System.out.println("Digite exatamente 'ACEITAR_CHAT " + remetente + "' para aceitar");
                        System.out.println("ou 'RECUSAR_CHAT " + remetente + "' para recusar");
                    } else if (mensagem.startsWith("CONTATO_ACEITO")) {
                        String remetente = mensagem.split(" ")[1];
                        System.out.println("\n" + remetente + " aceitou seu pedido de contato. Vocês agora podem conversar!");
                    } else if (mensagem.startsWith("CONTATO_RECUSADO")) {
                        String remetente = mensagem.split(" ")[1];
                        System.out.println("\n" + remetente + " recusou seu pedido de contato.");
                    } else {
                        System.out.println(mensagem);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ClienteChat cliente = new ClienteChat();
        cliente.iniciar();
    }
}