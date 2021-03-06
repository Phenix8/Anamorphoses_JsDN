package com.bof.gaze.activity.common;

import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bof.gaze.activity.game.DeathMatchAnnounceActivity;
import com.bof.gaze.activity.game.LeaderboardActivity;
import com.bof.gaze.activity.menu.MenuActivity;
import com.bof.gaze.application.GazeApplication;
import com.bof.gaze.model.Player;
import com.bof.gaze.network.client.Client;

import java.util.List;

/**
 * Created by root on 07/07/2017.
 */

public class CommonGameActivity extends CommonGazeActivity implements Client.GameEventListener {

    private ArrayAdapter<Player> playerAdapter = null;

    protected void setPlayerAdapter(ArrayAdapter<Player> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Given adapter is null.");
        }

        this.playerAdapter = adapter;
        this.playerAdapter.clear();
        this.playerAdapter.addAll(getGameClient().getPlayerList());
    }

    protected boolean isServerStarted() {
        return ((GazeApplication) this.getApplication()).isServerStarted();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getGameClient().setGameEventListener(this);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onGameEvent(Client.GameEventListener.GameEventType type, final Object data) {
        switch (type) {
            case GAME_ENDED:
                Intent intent = new Intent(this, LeaderboardActivity.class);
                intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;

            case DEATH_MATCH:
                String anamorphId = (String) data;
                Intent intent1 = new Intent(this, DeathMatchAnnounceActivity.class);
                intent1.putExtra("id", anamorphId);
                intent1.setFlags(intent1.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent1);
                break;

            case PLAYER_LIST_CHANGED:
                if (playerAdapter != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playerAdapter.clear();
                            playerAdapter.addAll((List<Player>) data);
                        }
                    });
                }
            break;

            case ERROR_OCCURED:
                final Exception e = (Exception) data;
                // TODO : manage error display on the next activity.
                /*
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                showError(e.getLocalizedMessage());
                            }
                        }
                );
                */
                e.printStackTrace();
                Intent intent2 = new Intent(this, MenuActivity.class);
                intent2.setFlags(intent2.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent2);
                break;

            case SERVER_STOPPED:
                // TODO : manage error display on the next activity.
                //showToast("Server was stopped", Toast.LENGTH_LONG);
                Intent intent3 = new Intent(this, MenuActivity.class);
                intent3.setFlags(intent3.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent3);
                break;
        }
    }
}
