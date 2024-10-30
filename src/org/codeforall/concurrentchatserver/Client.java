package org.codeforall.concurrentchatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private String hostName;
    private int portNumber;
    private String userName;
    private Socket clientSocket;
    private BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
    private BufferedReader serverIn;
    private PrintWriter serverOut;

    private void connect(){
        try {
            // get server details from user
            System.out.println("Please type the host IP address: ");
            hostName = userIn.readLine();
            System.out.println("Please type the host port number: ");
            portNumber = Integer.valueOf(userIn.readLine());
            System.out.println("Please type your username: ");
            userName = userIn.readLine();

            // create socket
            clientSocket = new Socket(hostName, portNumber);
            System.out.println("Connected to: " + clientSocket.toString());

            // create in/out streams
            serverOut = new PrintWriter(clientSocket.getOutputStream(), true);
            serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // start chat session
            chat();

        } catch (IOException e) {
            e.getMessage();
        }
    }

    private void chat(){
        // new thread to receive messages from server
        Thread t = new Thread(new MsgReceived());
        t.start();

        // send username to server
        serverOut.println("/name " + userName);

        // current thread sends messages to server
        while (true){
            try {
                serverOut.println(userIn.readLine());
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connect();
    }

    private class MsgReceived implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String str = serverIn.readLine();
                    if (str != null) {
                        System.out.println(str);
                    }
                    else {
                        break;
                    }
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }
    }
}
