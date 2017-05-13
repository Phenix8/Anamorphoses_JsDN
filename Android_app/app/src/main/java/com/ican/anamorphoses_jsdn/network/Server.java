package com.ican.anamorphoses_jsdn.network;

import android.util.Log;
import java.util.ArrayList;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by root on 12/04/2017.
 */

public abstract class Server extends Thread
    implements ClientHandler.ClientHandlerListener {

    private static String TAG = "Server";

    private RoomNotifier roomNotifier;

    private int tcpPort;

    private boolean listening = true;

    private int maxPlayer;

    private ArrayList<ClientHandler> client = new ArrayList<>();

    public void stopListening() {
        this.listening = false;
    }

    public void sendMessageToAll(String message) {
        for (ClientHandler client : this.client) {
            client.sendMessage(message);
        }
    }

    @Override
    public void run() {
        ServerSocket listeningSocket = null;

        try {
            listeningSocket = new ServerSocket(tcpPort);
            listeningSocket.setSoTimeout(1000);
            roomNotifier.startNotifying();

            while (listening && client.size() < maxPlayer) {
                try {
                    Socket socketClient = listeningSocket.accept();
                    Log.d(TAG, "Client connected (" + socketClient.getInetAddress() + ")");

                    ClientHandler client = new ClientHandler(socketClient);
                    client.addListener(this);
                    this.client.add(client);
                    client.start();
                } catch (SocketTimeoutException e) {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            roomNotifier.stopNotifying();
            try {
                if (listeningSocket != null) {
                    listeningSocket.close();
                }
            } catch (IOException e) {

            }
        }
    }
}
