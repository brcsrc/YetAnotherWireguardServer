PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS networks
(
    network_name VARCHAR(32) PRIMARY KEY,
    network_cidr VARCHAR(32),
    network_listen_port VARCHAR(32),
    network_private_key_name VARCHAR(32),
    network_tag VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS clients
(
    client_name VARCHAR(32) PRIMARY KEY,
    client_private_key_name VARCHAR(32),
    client_cidr VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS networks_clients
(
    network_client_id INTEGER PRIMARY KEY AUTOINCREMENT,
    network_name VARCHAR(32),
    client_name VARCHAR(32),
    FOREIGN KEY (client_name) REFERENCES clients(client_name),
    FOREIGN KEY (network_name) REFERENCES networks(network_name)
);

CREATE TABLE IF NOT EXISTS users
(
    user_name VARCHAR(32) PRIMARY KEY ,
    password VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS clients_users
(
    client_user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_name VARCHAR(32),
    client_name VARCHAR(32),
    FOREIGN KEY (user_name) REFERENCES users(user_name),
    FOREIGN KEY (client_name) REFERENCES clients(client_name)
);
