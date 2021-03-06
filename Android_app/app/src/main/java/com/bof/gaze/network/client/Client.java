package com.bof.gaze.network.client;

import android.util.Log;

import com.bof.gaze.model.Anamorphosis;
import com.bof.gaze.model.Player;
import com.bof.gaze.network.Common;
import com.bof.gaze.network.server.Protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements Runnable {

    private static String TAG = "Client";

    private ReentrantLock mutex = new ReentrantLock();

    private Socket socketServer;
    private String playerName;
    private BufferedWriter out;
    private BufferedReader in;
    private InetAddress serverAddress;

    private GameEventListener listener = null;

    private boolean reconnecting;
    private boolean running;
    private Thread thread = null;

    private String playerId = null;
    private boolean lobby = true;

    private List<Player> players = new ArrayList<>();
    private String roomName = null;

    public interface GameEventListener {
        enum GameEventType {
            PLAYER_LIST_CHANGED,
            GAME_STARTED,
            GAME_ENDED,
            DEATH_MATCH,
            ERROR_OCCURED,
            SERVER_STOPPED,
            ROOM_NAME_CHANGED
        }

        void onGameEvent(GameEventType type, Object data);
    }

    public synchronized void connectServer(String playerName, InetAddress serverAddress) {
        if (thread != null) {
            joinThread();
        }

        this.playerName = playerName;
        this.serverAddress = serverAddress;
        this.playerId = null;
        this.running = true;
        this.reconnecting = false;
        this.thread = new Thread(this);
        this.thread.start();
        this.players = new ArrayList<>();
    }

    private void joinThread() {
        while (this.thread != null) {
            try {
                this.thread.join();
                this.thread = null;
            } catch (InterruptedException e) {}
        }
    }

    private void configureStreams(Socket socket)
        throws IOException {
        this.socketServer = socket;
        out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
    }

    private void sendInstruction(final String instruction)
        throws IOException {
        if (out == null) {
            return;
        }

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    out.write(instruction);
                    out.flush();
                } catch (IOException e) {
                    notifyListener(GameEventListener.GameEventType.ERROR_OCCURED, e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
        while (t != null) {
            try {
                t.join();
                t = null;
            } catch (InterruptedException e) {}
        }
    }

    private void sendPlayerName()
        throws IOException {
        sendInstruction(
                Protocol.buildConnectInstruction(playerName)
        );
    }

    public void sendRoomName(String roomName) throws IOException {
        sendInstruction(Protocol.buildRoomNameInstruction(roomName));
    }

    public void toggleReady()throws IOException {
        sendInstruction(Protocol.buildReadyInstruction(playerId));
    }

    public void setFound(Anamorphosis a) throws IOException {
        sendInstruction(Protocol.buildAnamorphosisFoundMessage(a));
    }

    public void startGame() throws IOException {
        sendInstruction(Protocol.buildStartInstruction());
    }

    public void disconnect() {
        boolean threadJoined = false;
        try {
            sendInstruction(Protocol.buildQuitInstruction());
        } catch (IOException e) {};
        running = false;
    }

    public List<Player> getPlayerList() {
        return players;
    }

    public String getRoomName() { return roomName; }

    public String getPlayerId() {
        return this.playerId;
    }

    public Player getCurrentPlayer() {
        for (Player p : players) {
            if (p.getPlayerId().equals(this.playerId)) {
                return p;
            }
        }
        return null;
    }

    private synchronized void notifyListener(GameEventListener.GameEventType type, Object data) {
        if (listener != null) {
            listener.onGameEvent(type, data);
        }
    }

    public synchronized void setGameEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return out != null;
    }

    @Override
    public void run() {
        try {
            this.socketServer = new Socket(this.serverAddress, Common.TCP_PORT);
            configureStreams(this.socketServer);

            if (reconnecting) {
                sendInstruction(Protocol.buildReconnectMessageType(playerId));
            } else {
                sendPlayerName();
            }

            reconnecting = false;

            while (running) {
                String message = in.readLine();
                Log.d(TAG, String.format("received : %s", message));

                String instructionType = Protocol.parseInstructionType(message);

                switch (instructionType) {
                    case Protocol.PLAYERS_INSTRUCTION_TYPE:
                        players = Protocol.parsePlayerListData(Protocol.parseInstructionData(message));
                        notifyListener(GameEventListener.GameEventType.PLAYER_LIST_CHANGED, players);
                    break;

                    case Protocol.PLAYER_ID_INSTRUCTION_TYPE:
                        this.playerId = Protocol.parseInstructionData(message);
                    break;

                    case Protocol.ALREADY_STARTED_INSTRUCTION_TYPE:
                        notifyListener(
                                GameEventListener.GameEventType.ERROR_OCCURED,
                                "Game already stated"
                        );
                    break;

                    case Protocol.START_INSTRUCTION_TYPE:
                        notifyListener(GameEventListener.GameEventType.GAME_STARTED, null);
                        lobby = false;
                    break;

                    case Protocol.NOT_READY_INSTRUCTION_TYPE:
                        notifyListener(
                                GameEventListener.GameEventType.ERROR_OCCURED,
                                "Not all players are ready.");
                        break;

                    case Protocol.DEATHMATCH_INSTRUCTION_TYPE:
                        String id =
                                Protocol.parseDeathMatchInstruction(
                                        Protocol.parseInstructionData(message),
                                        this.playerId
                                );
                        notifyListener(GameEventListener.GameEventType.DEATH_MATCH, id);
                    break;

                    case Protocol.FINISHED_INSTRUCTION_TYPE:
                        players = Protocol.parsePlayerListData(Protocol.parseInstructionData(message));
                        notifyListener(GameEventListener.GameEventType.GAME_ENDED, players);
                    break;

                    case Protocol.SERVER_STOPPED_INSTRUCTION:
                        running = false;
                        players.clear();
                        notifyListener(GameEventListener.GameEventType.SERVER_STOPPED, null);
                    break;

                    case Protocol.ROOM_NAME_INSTRUCTION_TYPE:
                        notifyListener(GameEventListener.GameEventType.ROOM_NAME_CHANGED,
                            Protocol.parseRoomNameInstruction(
                                Protocol.parseInstructionData(message)
                            )
                        );
                    break;
                }
            }
        } catch (Exception e) {
            notifyListener(GameEventListener.GameEventType.ERROR_OCCURED, e);
            reconnecting = true;
        } finally {
            try {
                if (socketServer != null) {
                    socketServer.close();
                }
            } catch (IOException e) {}
            out = null;
            if (reconnecting) {
                try {
                    Thread.sleep(3000);
                    this.thread = new Thread(this);
                    this.thread.start();
                } catch (InterruptedException e) {}
            }
        }
    }
}
