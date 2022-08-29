package me.monmcgt.code.onstance.server.socket;

import me.monmcgt.code.onstance.server.storage.InstanceList;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private final ServerSocket serverSocket;

    private final ExecutorService executorService;

    private final InstanceList instanceList;

    public SocketServer(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.executorService = Executors.newFixedThreadPool(50);
            this.instanceList = new InstanceList();
            System.out.println("Socket server started on port " + port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        SocketServerThread socketServerThread = new SocketServerThread();
        socketServerThread.start();
    }

    private class SocketServerThread extends Thread  {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket, executorService, instanceList);
                    boolean running = clientHandler.startRunning();
                    if (running) {
//                        System.out.println("Client connected: " + socket.getInetAddress());
                    } else {
//                        System.out.println("Client connection failed: " + socket.getInetAddress());
                        System.out.println("[Onstance] Client connection failed: " + socket.getInetAddress());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
