package org.codeforall.concurrentchatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private int portNumber = 8085;
    private ServerSocket serverSocket;
    private List<ServerWorker> serverWorkers = Collections.synchronizedList(new ArrayList<ServerWorker>());
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private void start(){
        try {
            // starts server
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

                // client joined, adds to list of clients, starts thread
                serverWorkers.add(worker);
                threadPool.submit(worker);
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }

    private void forward(String str) {
        // forwards message to all clients in list
        synchronized (serverWorkers) {
            for (ServerWorker worker : serverWorkers) {
                worker.send(str);
                System.out.println("Sending \"" + str + "\" to " + worker.userName);
            }
        }
    }

    private void clientQuit(ServerWorker worker){
        // removes client from list when they manually quit
        serverWorkers.remove(worker);
    }

    private void clientWhisper(ServerWorker sender, String recipient, String str) {
        // checks all clients for whisper recipient and sends it to both sender and recipient
        synchronized (serverWorkers) {
            for (ServerWorker worker : serverWorkers) {
                if (worker.userName.equals(recipient)) {
                    worker.send("whisper from " + sender.userName + ": " + str);
                    sender.send("whisper sent to " + recipient + ": " + str);
                    System.out.println("Sending whisper \"" + str + "\" to " + worker.userName);
                }
            }
        }
    }

    private void listClients(ServerWorker sender){
        // creates string with all clients' usernames and sends it only to the one who requested it
        synchronized (serverWorkers){
            StringBuilder str = new StringBuilder("The following user(s) are online: ");
            for (ServerWorker worker : serverWorkers){
                str.append(worker.userName + ", ");
            }
            str.setLength(str.length() - 2);
            sender.send(str.toString());
            System.out.println("Sending \"" + str + "\" to " + sender.userName);
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
            // initializes client socket and in/out streams
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

                    // checks if client is still connected, if not closes connection
                    if (str == null){
                        leave();
                    }
                    // checks if message is a valid command
                    else if (str.startsWith("/")) {
                        checkCommands(str);
                    }
                    // forwards if not
                    else {
                        forward(userName + ": " + str);
                    }
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }

        private void checkCommands(String str){
            // checks for command to change username, if left blank username becomes anonymous
            if (str.split(" ")[0].equals("/name")) {
                if (str.split(" ").length > 1) {
                    System.out.println(userName + " changed username to " + str.split(" ")[1]);
                    userName = str.split(" ")[1];
                } else {
                    System.out.println(userName + "changed username to anonymous");
                    userName = "anonymous";
                }
            }

            // checks for command to quit
            else if (str.split(" ")[0].equals("/quit")) {
                leave();
            }

            // checks for command to shout
            else if (str.split(" ")[0].equals("/shout")) {
                if (str.split(" ").length > 1) {
                    forward(userName + ": " + str.split(" ", 2)[1].toUpperCase());
                }
            }

            // checks for command to whisper
            else if (str.split(" ")[0].equals("/whisper")) {
                if (str.split(" ").length > 2) {
                    clientWhisper(this,str.split(" ")[1],str.split(" ",3)[2]);
                }
            }

            // checks for command to list online users
            else if (str.split(" ")[0].equals("/list")) {
                listClients(this);
            }
        }

        private void leave(){
            // closes in/out stream, socket, removes from serverworker list
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

        private void send(String str){
            // sends messages from server to client
            out.println(str);
        }
    }
}
