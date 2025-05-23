CREATE DATABASE IF NOT EXISTS chat_app;
USE chat_app;

CREATE TABLE IF NOT EXISTS users (
    login VARCHAR(50) PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'offline'
);

CREATE TABLE IF NOT EXISTS groups (
    name VARCHAR(50) PRIMARY KEY,
    creator VARCHAR(50) NOT NULL,
    FOREIGN KEY (creator) REFERENCES users(login)
);

CREATE TABLE IF NOT EXISTS group_members (
    group_name VARCHAR(50),
    user_login VARCHAR(50),
    PRIMARY KEY (group_name, user_login),
    FOREIGN KEY (group_name) REFERENCES groups(name),
    FOREIGN KEY (user_login) REFERENCES users(login)
);

CREATE TABLE IF NOT EXISTS group_invites (
    group_name VARCHAR(50),
    user_login VARCHAR(50),
    inviter VARCHAR(50),
    PRIMARY KEY (group_name, user_login),
    FOREIGN KEY (group_name) REFERENCES groups(name),
    FOREIGN KEY (user_login) REFERENCES users(login),
    FOREIGN KEY (inviter) REFERENCES users(login)
);

CREATE TABLE IF NOT EXISTS group_join_requests (
    group_name VARCHAR(50),
    user_login VARCHAR(50),
    PRIMARY KEY (group_name, user_login),
    FOREIGN KEY (group_name) REFERENCES groups(name),
    FOREIGN KEY (user_login) REFERENCES users(login)
);

CREATE TABLE IF NOT EXISTS offline_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(50) NOT NULL,
    sender VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    FOREIGN KEY (recipient) REFERENCES users(login),
    FOREIGN KEY (sender) REFERENCES users(login)
);