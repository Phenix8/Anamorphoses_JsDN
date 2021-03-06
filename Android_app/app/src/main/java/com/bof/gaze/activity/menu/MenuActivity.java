package com.bof.gaze.activity.menu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.bof.gaze.R;
import com.bof.gaze.activity.common.CommonGazeActivity;
import com.bof.gaze.activity.game.AnamorphosisChoiceActivity;
import com.bof.gaze.network.Util;

public class MenuActivity extends CommonGazeActivity {

    WifiManager wifiManager;

    private static final int REQUEST_CODE = 13;

    private int numberOfTouchOnGazeLogo;
    private int numberOfTimeBackWasPressed;

    private ImageButton backgroundImg;

    private final Context self = this;

    private void requestCameraPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                showInfo(getResources().getString(R.string.menuCameraExplainationInfo));

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.CAMERA }, REQUEST_CODE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        final LinearLayout debugOptionsLayout = (LinearLayout) findViewById(R.id.menu_act_debug_layout);
        debugOptionsLayout.setVisibility(View.INVISIBLE);

        // Boutton de débug pour commencer directement une partie
        final Button debugButton = (Button) findViewById(R.id.Debug_game_button);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(self, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), AnamorphosisChoiceActivity.class);
                intent.putExtra("debug", true);
                startActivity(intent);
                debugOptionsLayout.setVisibility(View.INVISIBLE);
            }
        });

        final Button changeBoardButton = (Button) findViewById(R.id.menu_act_change_board_btn);
        changeBoardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ChangeBoardActivity.class);
                startActivity(intent);
                debugOptionsLayout.setVisibility(View.INVISIBLE);
            }
        });

        backgroundImg = (ImageButton) findViewById(R.id.menu_act_background_img);
        backgroundImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numberOfTouchOnGazeLogo++;
                if (numberOfTouchOnGazeLogo >= 10) {
                    debugOptionsLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        AssignImageButtonRedirection(R.id.createGameButton, LobbyActivity.class, true, true);
        AssignImageButtonRedirection(R.id.joinGameButton, JoinRoomActivity.class, true, true);
        AssignImageButtonRedirection(R.id.rulesButton, TutorialActivity.class, false, false);
        AssignImageButtonRedirection(R.id.highscoresButton, NicknameActivity.class, false, false);

    }

    @Override
    protected void onResume() {
        numberOfTimeBackWasPressed = 0;
        numberOfTouchOnGazeLogo = 0;

        super.onResume();
    }

    // Assigne à l'ImageButton dont l'ID est passé en paramètre
    // une fonction de redirection vers la classe 'redirectionClass'
    private void AssignImageButtonRedirection(int id, final Class redirectionClass, final boolean checkForWifi, final boolean checkForCamera)
    {
        ImageButton currentButton = (ImageButton) findViewById(id);
        currentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkForCamera) {
                    if (ContextCompat.checkSelfPermission(self, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestCameraPermission();
                        return;
                    }
                }

                if (checkForWifi) {
                    if (!Util.isWifiEnabled(wifiManager)) {
                        showToast(getResources().getString(R.string.menuEnableWifiToast));
                        return;
                    }
                }
                Intent menuOptionActivity = new Intent(getApplicationContext(), redirectionClass);

                if (redirectionClass.getName() == "TutorialActivity")
                    menuOptionActivity.putExtra("isFirstLaunch", false);

                startActivity(menuOptionActivity);
            }
        });
    }

    @Override
    public void onBackPressed() {
        numberOfTimeBackWasPressed++;
        if (numberOfTimeBackWasPressed >= 2) {
            finish();
        } else {
            showToast(getResources().getString(R.string.menuOnBackPressedToast));
        }
    }
}
