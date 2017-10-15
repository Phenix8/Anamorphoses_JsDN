package com.ican.gaze.activity.menu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ican.gaze.R;

import dlibwrapper.DLibWrapper;

public class HomeActivity extends AppCompatActivity {

    private ImageView splashScreenImg;
    private TextView permissionTxt;
    private Button permissionBtn;
    private LinearLayout permissionPopup;

    int WIFI_PERMISSIONS_REQUEST = 1, CAMERA_PERMISSIONS_REQUEST = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Permission pop-up
        permissionTxt = (TextView) findViewById(R.id.homePermissionTxt);
        permissionPopup = (LinearLayout) findViewById(R.id.homePermissionLayout);

        permissionBtn = (Button) findViewById(R.id.homePermissionBtn);
        permissionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionPopup.setVisibility(View.INVISIBLE);
                SystemPermissionRequest(permissionBtn.getText() == "Enable Camera");
            }
        });

        // IMAGEVIEW SPLASHSCREEN
        splashScreenImg = (ImageView) findViewById(R.id.splashscreenImg) ;
        splashScreenImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CheckForPersmissions();
                SharedPreferences sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE);
                String nickname = sharedPref.getString("nickname", "");

                if (nickname.isEmpty()) {
                    Intent intent = new Intent(getApplicationContext(), NicknameActivity.class);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            }
        });

        DLibWrapper.getInstance().loadDetectors(this.getAssets(), "detectors");
    }

    @Override
    public void onBackPressed() {
        return;
    }

    // Vérification of WIFI and CAMERA permissions
    public void CheckForPersmissions()
    {
        // Permission to use the "WiFi" component
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_WIFI_STATE)!= PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE))
            {
                permissionTxt.setText("The Wifi permission is required to play");
                permissionBtn.setText("Enable Wifi");
                permissionPopup.setVisibility(View.VISIBLE);
            }
            else
                SystemPermissionRequest(false);
        }

        // Permission to use the "Camera" component
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
            {
                permissionTxt.setText("The Camera permission is required to play");
                permissionBtn.setText("Enable Camera");
                permissionPopup.setVisibility(View.VISIBLE);
            }
            else
                SystemPermissionRequest(true);
        }
    }

    private void SystemPermissionRequest(boolean isForCamera)
    {
        if (isForCamera)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST);
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, WIFI_PERMISSIONS_REQUEST);
    }
}
