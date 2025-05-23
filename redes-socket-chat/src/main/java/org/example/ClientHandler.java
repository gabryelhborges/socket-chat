package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private final Socket socket;
    public PrintWriter out;
    private BufferedReader in;
    private String username;
    private final DataManager dataManager;
    private final ConcurrentHashMap<String, ClientHandler> clients;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dataManager = ChatServer.getDataManager();
        this.clients = ChatServer.getClients();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                String input = in.readLine();
                if (input == null) break;
                processCommand(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "REGISTER":
                handleRegister(args);
                break;
            case "LOGIN":
                handleLogin(args);
                break;
            case "LOGOUT":
                handleLogout();
                break;
            case "STATUS":
                handleStatus(args);
                break;
            case "LIST_USERS":
                handleListUsers();
                break;
            case "LIST_GROUPS":
                handleListGroups();
                break;
            case "MSG_USER":
                handleMsgUser(args);
                break;
            case "ACCEPT_CHAT":
                handleAcceptChat(args);
                break;
            case "DECLINE_CHAT":
                handleDeclineChat(args);
                break;
            case "PMSG":
                handlePrivateMsg(args);
                break;
            case "CREATE_GROUP":
                handleCreateGroup(args);
                break;
            case "ADD_TO_GROUP":
                handleAddToGroup(args);
                break;
            case "JOIN_GROUP_REQUEST":
                handleJoinGroupRequest(args);
                break;
            case "GROUP_INVITE_RESPONSE":
                handleGroupInviteResponse(args);
                break;
            case "GROUP_JOIN_VOTE":
                handleGroupJoinVote(args);
                break;
            case "MSG_GROUP":
                handleMsgGroup(args);
                break;
            case "MSG_GROUP_TARGETED":
                handleMsgGroupTargeted(args);
                break;
            case "MSG_GROUP_PRIVATE":
                handleMsgGroupPrivate(args);
                break;
            case "LEAVE_GROUP":
                handleLeaveGroup(args);
                break;
            default:
                out.println("ERROR Unknown command");
        }
    }

    private void handleRegister(String args) {
        String[] parts = args.split(" ", 4);
        if (parts.length != 4) {
            out.println("ERROR Invalid register format");
            return;
        }
        String fullName = parts[0];
        String login = parts[1];
        String email = parts[2];
        String password = parts[3];

        if (dataManager.registerUser(fullName, login, email, password)) {
            out.println("OK Registered successfully");
        } else {
            out.println("ERROR Login already exists");
        }
    }

    private void handleLogin(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid login format");
            return;
        }
        String login = parts[0];
        String password = parts[1];

        if (dataManager.authenticateUser(login, password)) {
            this.username = login;
            clients.put(login, this);
            dataManager.setUserStatus(login, "online");
            out.println("OK Logged in successfully");
            sendOfflineMessages();
        } else {
            out.println("ERROR Invalid credentials");
        }
    }

    private void handleLogout() {
        if (username != null) {
            dataManager.setUserStatus(username, "offline");
            clients.remove(username);
            out.println("OK Logged out");
            cleanup();
        }
    }

    private void handleStatus(String status) {
        if (username != null) {
            dataManager.setUserStatus(username, status);
            broadcastStatusUpdate(username, status);
            out.println("OK Status updated to " + status);
        } else {
            out.println("ERROR Not logged in");
        }
    }

    private void handleListUsers() {
        String users = String.join(",", dataManager.getOnlineUsers());
        out.println("OK " + users);
    }

    private void handleListGroups() {
        String groups = String.join(",", dataManager.getGroups());
        out.println("OK " + groups);
    }

    private void handleMsgUser(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid message format");
            return;
        }
        String recipient = parts[0];
        String message = parts[1];

        ClientHandler recipientHandler = clients.get(recipient);
        if (recipientHandler != null) {
            recipientHandler.out.println("NEW_MSG " + username + " " + message);
            out.println("OK Message sent");
        } else {
            dataManager.storeOfflineMessage(recipient, username, message);
            out.println("OK Message queued for offline user");
        }
    }

    private void handleAcceptChat(String sender) {
        // Implementar lógica de aceitação de chat
        out.println("OK Chat accepted with " + sender);
    }

    private void handleDeclineChat(String sender) {
        // Implementar lógica de recusa de chat
        out.println("OK Chat declined with " + sender);
    }

    private void handlePrivateMsg(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid private message format");
            return;
        }
        String recipient = parts[0];
        String message = parts[1];

        ClientHandler recipientHandler = clients.get(recipient);
        if (recipientHandler != null) {
            recipientHandler.out.println("NEW_MSG " + username + " " + message);
            out.println("OK Private message sent");
        } else {
            dataManager.storeOfflineMessage(recipient, username, message);
            out.println("OK Private message queued for offline user");
        }
    }

    private void handleCreateGroup(String groupName) {
        if (dataManager.createGroup(groupName, username)) {
            out.println("OK Group " + groupName + " created");
        } else {
            out.println("ERROR Group already exists");
        }
    }

    private void handleAddToGroup(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid add to group format");
            return;
        }
        String groupName = parts[0];
        String userLogin = parts[1];

        if (dataManager.addUserToGroup(groupName, userLogin, username)) {
            ClientHandler userHandler = clients.get(userLogin);
            if (userHandler != null) {
                userHandler.out.println("GROUP_INVITE " + groupName + " " + username);
            }
            out.println("OK User invited to group");
        } else {
            out.println("ERROR Failed to add user to group");
        }
    }

    private void handleJoinGroupRequest(String groupName) {
        if (dataManager.requestJoinGroup(groupName, username)) {
            out.println("OK Join request sent");
        } else {
            out.println("ERROR Failed to send join request");
        }
    }

    private void handleGroupInviteResponse(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid group invite response format");
            return;
        }
        String groupName = parts[0];
        String response = parts[1];

        if (dataManager.processGroupInviteResponse(groupName, username, response.equals("yes"))) {
            out.println("OK Group invite response processed");
        } else {
            out.println("ERROR Failed to process group invite response");
        }
    }

    private void handleGroupJoinVote(String args) {
        String[] parts = args.split(" ", 3);
        if (parts.length != 3) {
            out.println("ERROR Invalid group join vote format");
            return;
        }
        String groupName = parts[0];
        String requestingUser = parts[1];
        String vote = parts[2];

        if (dataManager.processGroupJoinVote(groupName, requestingUser, username, vote.equals("yes"))) {
            out.println("OK Vote processed");
        } else {
            out.println("ERROR Failed to process vote");
        }
    }

    private void handleMsgGroup(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid group message format");
            return;
        }
        String groupName = parts[0];
        String message = parts[1];

        dataManager.broadcastGroupMessage(groupName, username, message);
        out.println("OK Group message sent");
    }

    private void handleMsgGroupTargeted(String args) {
        String[] parts = args.split(" ", 3);
        if (parts.length != 3) {
            out.println("ERROR Invalid targeted group message format");
            return;
        }
        String groupName = parts[0];
        String recipients = parts[1];
        String message = parts[2];

        dataManager.sendTargetedGroupMessage(groupName, username, recipients.split(","), message);
        out.println("OK Targeted group message sent");
    }

    private void handleMsgGroupPrivate(String args) {
        String[] parts = args.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR Invalid private group message format");
            return;
        }
        String[] recipientParts = parts[0].split("@");
        if (recipientParts.length != 2) {
            out.println("ERROR Invalid private group message format");
            return;
        }
        String groupName = recipientParts[0];
        String recipient = recipientParts[1];
        String message = parts[1];

        dataManager.sendPrivateGroupMessage(groupName, username, recipient, message);
        out.println("OK Private group message sent");
    }

    private void handleLeaveGroup(String groupName) {
        if (dataManager.leaveGroup(groupName, username)) {
            out.println("OK Left group " + groupName);
            dataManager.notifyGroupMembers(groupName, "USER_LEFT_GROUP " + groupName + " " + username);
        } else {
            out.println("ERROR Failed to leave group");
        }
    }

    private void sendOfflineMessages() {
        List<String> offlineMessages = dataManager.getOfflineMessages(username);
        for (String msg : offlineMessages) {
            out.println("NEW_MSG " + msg);
        }
    }

    private void broadcastStatusUpdate(String user, String status) {
        for (ClientHandler client : clients.values()) {
            client.out.println("USER_STATUS_UPDATE " + user + " " + status);
        }
    }

    private void cleanup() {
        if (username != null) {
            clients.remove(username);
            dataManager.setUserStatus(username, "offline");
            broadcastStatusUpdate(username, "offline");
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}