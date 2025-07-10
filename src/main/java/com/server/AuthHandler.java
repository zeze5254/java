package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthHandler implements Runnable {
    private Socket socket;
    private Set<String> usedNames;
    private ConcurrentHashMap<String, String> credentials;
    private BufferedReader in;
    private PrintWriter out;

    public AuthHandler(Socket socket, Set<String> usedNames, 
                     ConcurrentHashMap<String, String> credentials) {
        this.socket = socket;
        this.usedNames = usedNames;
        this.credentials = credentials;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erreur AuthHandler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Phase 1: Authentification
            out.println("AUTH_REQUIRED");
            String authData = in.readLine();
            
            if (authData == null) return;
            
            String[] parts = authData.split(":");
            if (parts.length != 2 || !authenticate(parts[0], parts[1])) {
                out.println("AUTH_FAILED");
                socket.close();
                return;
            }

            // Phase 2: Nom d'utilisateur unique
            out.println("SUBMITNAME");
            String name;
            
            while (true) {
                name = in.readLine();
                if (name == null) return;
                
                synchronized (usedNames) {
                    if (!usedNames.contains(name)) {
                        usedNames.add(name);
                        out.println("NAMEACCEPTED:" + name);
                        break;
                    }
                    out.println("NAMEREJECTED");
                }
            }

            // Passer au chat
            new ClientHandler(socket, name, usedNames).run();
            
        } catch (IOException e) {
            System.err.println("Erreur authentification: " + e.getMessage());
        }
    }

    private boolean authenticate(String username, String password) {
        String storedPass = credentials.get(username);
        return storedPass != null && storedPass.equals(password);
    }
}