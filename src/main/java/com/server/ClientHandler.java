package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String clientName;
    private Set<String> usedNames;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, String name, Set<String> usedNames) {
        this.clientSocket = socket;
        this.clientName = name;
        this.usedNames = usedNames;
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Erreur création des flux: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            System.out.println(clientName + " a rejoint le chat.");

            String input;
            while ((input = in.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(input)) {
                    break;
                }
                System.out.println(clientName + ": " + input);
            }
        } catch (IOException e) {
            System.err.println("Erreur client: " + e.getMessage());
        } finally {
            // Nettoyage
            if (clientName != null) {
                usedNames.remove(clientName);
                System.out.println(clientName + " a quitté le chat.");
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erreur fermeture socket: " + e.getMessage());
            }
        }
    }
}