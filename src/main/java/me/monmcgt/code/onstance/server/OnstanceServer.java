package me.monmcgt.code.onstance.server;

import me.monmcgt.code.onstance.server.socket.SocketServer;

public class OnstanceServer {
    private final int port;

    public OnstanceServer() {
        this(56780);
    }

    public OnstanceServer(int port) {
        this.port = port;
    }

    public void start() {
        SocketServer socketServer = new SocketServer(this.port);
        socketServer.start();
    }
}
