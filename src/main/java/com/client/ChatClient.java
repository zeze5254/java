package com.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            // Phase 1: Authentification
            String serverResponse = in.readLine();
            if ("AUTH_REQUIRED".equals(serverResponse)) {
                System.out.print("Nom d'utilisateur: ");
                String username = scanner.nextLine();
                System.out.print("Mot de passe: ");
                String password = scanner.nextLine();
                out.println(username + ":" + password);
                
                serverResponse = in.readLine();
                if (!"AUTH_SUCCESS".equals(serverResponse)) {
                    System.out.println("Échec de l'authentification");
                    return;
                }
            }

            // Phase 2: Nom unique
            serverResponse = in.readLine();
            if ("SUBMITNAME".equals(serverResponse)) {
                String name;
                while (true) {
                    System.out.print("Choisissez un pseudo: ");
                    name = scanner.nextLine();
                    out.println(name);
                    
                    serverResponse = in.readLine();
                    if (serverResponse.startsWith("NAMEACCEPTED")) {
                        System.out.println("Connecté en tant que " + name);
                        break;
                    } else if ("NAMEREJECTED".equals(serverResponse)) {
                        System.out.println("Nom déjà utilisé, veuillez en choisir un autre");
                    }
                }
            }

            // Phase 3: Chat
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("Déconnecté du serveur");
                }
            }).start();

            String userInput;
            while ((userInput = scanner.nextLine()) != null) {
                out.println(userInput);
                if ("/quit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur client: " + e.getMessage());
        }
    }
}