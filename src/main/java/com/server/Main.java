package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final int DEFAULT_PORT = 1234;
    private static final Set<String> usedNames = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();

    static {
        // Base de données d'utilisateurs (en production, utiliser BCrypt pour hasher)
        credentials.put("alice", "password123");
        credentials.put("bob", "bobpass");
    }

    public static void main(String[] args) {
        try {
            // Vérification stricte du port 1234
            verifyPortAvailability();
            
            // Démarrage du serveur
            try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
                logger.info("Serveur démarré avec succès sur le port {}", DEFAULT_PORT);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Nouvelle connexion: {}", clientSocket.getRemoteSocketAddress());
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            }
        } catch (IOException e) {
            logger.error("Erreur serveur: {}", e.getMessage());
        }
    }

    private static void verifyPortAvailability() throws IOException {
        try (ServerSocket testSocket = new ServerSocket(DEFAULT_PORT)) {
            testSocket.setReuseAddress(true);
        } catch (BindException e) {
            logger.error("ERREUR: Le port {} est déjà utilisé.", DEFAULT_PORT);
            logger.error("Solutions possibles:");
            logger.error("1. Fermez toute autre instance de ce serveur");
            logger.error("2. Exécutez 'netstat -ano | findstr :{}' pour identifier le processus", DEFAULT_PORT);
            logger.error("3. Utilisez 'taskkill /PID [PID] /F' pour terminer le processus");
            throw e;
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Phase d'authentification
                out.println("AUTH_REQUIRED");
                String[] authData = in.readLine().split(":");
                if (authData.length != 2 || !credentials.getOrDefault(authData[0], "").equals(authData[1])) {
                    out.println("AUTH_FAILED");
                    return;
                }
                out.println("AUTH_SUCCESS");

                // Phase de nom d'utilisateur
                out.println("SUBMITNAME");
                while (true) {
                    username = in.readLine();
                    synchronized (usedNames) {
                        if (!usedNames.contains(username)) {
                            usedNames.add(username);
                            out.println("NAMEACCEPTED");
                            break;
                        }
                        out.println("NAMEREJECTED");
                    }
                }

                logger.info("{} a rejoint le chat", username);
                
                // Gestion des messages
                String input;
                while ((input = in.readLine()) != null) {
                    if ("/quit".equalsIgnoreCase(input)) break;
                    logger.info("{}: {}", username, input);
                }

            } catch (IOException e) {
                logger.error("Erreur avec le client: {}", e.getMessage());
            } finally {
                if (username != null) {
                    usedNames.remove(username);
                    logger.info("{} a quitté le chat", username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("Erreur lors de la fermeture du socket: {}", e.getMessage());
                }
            }
        }
    }
}