package org.codeforall.concurrentchatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private int portNumber = 8085;
    private ServerSocket serverSocket;
    private ArrayList<ServerWorker> serverWorkers = new ArrayList<>();

    private void start(){
        try {
            // start server
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started: " + serverSocket.toString());
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private void connect() {
        while (true) {
            try {
                // waiting for client
                ServerWorker worker = new ServerWorker(serverSocket.accept());

                // client joined, add to list of clients, start thread
                serverWorkers.add(worker);
                Thread t = new Thread(worker);
                t.start();
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }

    private void forward(String str) {
        // forward message to all clients in list
        for (ServerWorker worker : serverWorkers) {
            worker.send(str);
            System.out.println("Sending \"" + str + "\" to " + worker.userName);
        }
    }

    private void clientQuit(ServerWorker worker){
        // remove client from list when they manually quit
        serverWorkers.remove(worker);
    }

    private void clientWhisper(ServerWorker sender, String recipient, String str) {
        // checks all clients for sender and recipient and sends the whisper
        for (ServerWorker worker : serverWorkers) {
            if (worker.userName.equals(recipient)){
                worker.send("whisper from " + sender.userName + ": " + str);
                sender.send("whisper sent to " + recipient + ": " + str);
                System.out.println("Sending whisper \"" + str + "\" to " + worker.userName);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
        server.connect();
    }

    private class ServerWorker implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String userName = "anonymous";

        public ServerWorker(Socket socket) {
            // initialize client socket and in/out streams
            clientSocket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.getMessage();
            }
            System.out.println("Client connected: " + clientSocket.toString());
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String str = in.readLine();

                    // check if message is a command, if not forward
                    if (str.startsWith("/")) {
                        checkCommands(str);
                    }
                    else {
                        forward(userName + ": " + str);
                    }
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }

        private void checkCommands(String str){
            // check for command to change username
            if (str.split(" ")[0].equals("/name")) {
                if (str.split(" ").length > 1) {
                    System.out.println(userName + " changed username to " + str.split(" ")[1]);
                    userName = str.split(" ")[1];
                } else {
                    System.out.println(userName + "changed username to anonymous");
                    userName = "anonymous";
                }
            }

            // check for command to quit
            else if (str.split(" ")[0].equals("/quit")) {
                try {
                    forward(userName + " left the chat");
                    in.close();
                    out.close();
                    clientSocket.close();
                    clientQuit(this);
                } catch (IOException e) {
                    e.getMessage();
                }
            }

            // check for command to shout
            else if (str.split(" ")[0].equals("/shout")) {
                if (str.split(" ").length > 1) {
                    forward(userName + ": " + str.split(" ", 2)[1].toUpperCase());
                }
            }

            // check for command to whisper
            else if (str.split(" ")[0].equals("/whisper")) {
                if (str.split(" ").length > 2) {
                    clientWhisper(this,str.split(" ")[1],str.split(" ",3)[2]);
                }
            }
        }

        private void send(String str){
            // sends messages from server to client
            out.println(str);
        }
    }
}
