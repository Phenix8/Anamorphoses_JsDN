package com.bof.gaze.activity.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bof.gaze.R;
import com.bof.gaze.activity.common.CommonGameActivity;
import com.bof.gaze.model.Player;
import com.bof.gaze.network.client.Client;
import com.bof.gaze.network.server.Protocol;
import com.bof.gaze.view.AutoFitTextureView;
import com.bof.gaze.camera.CameraProcessor;
import com.bof.gaze.model.Anamorphosis;
import com.bof.gaze.view.CameraGlassSurfaceView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.bof.gaze.detection.ObjectDetector;

public class CameraActivity extends CommonGameActivity
        implements View.OnClickListener, CameraProcessor.CameraProcessorListener, View.OnTouchListener, Client.GameEventListener {

    private ImageView cancelImg, littleAnamorphImg, largeAnamorphImg, cameraImg;
    private CameraGlassSurfaceView cameraGlassSurfaceView;
    private AutoFitTextureView textureView;

    private Anamorphosis targetAnamorphosis;

    private boolean canCancel = true;

    private CameraProcessor cameraProcessor = new CameraProcessor(this);

    ArrayList<Player> playerListTest = new ArrayList<Player>();

    // Adapter to fill (Image + text) the player scores list
    private ArrayAdapter<Player> adapter;

    private class CustomAdapter extends ArrayAdapter<Player> {
        CustomAdapter(Context context, int resource, ArrayList<Player> players) {
            super(context, resource, players);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            Player player = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.camera_player_score_item, parent, false);

            if (player == null)
                return convertView;

            TextView txtPlayerScore = convertView.findViewById(R.id.camera_J_score_txt);
            ImageView imgPlayerScore = convertView.findViewById(R.id.camera_J_score_img);

            txtPlayerScore.setText(String.valueOf(player.getScore()));

            // The current player case
            if (player.getPlayerId().equals(getGameClient().getPlayerId()))
            //if (player.getPlayerId().equals("99"))
            {
                updateFoundAnamorphosisImg(convertView, player);
                imgPlayerScore.setImageResource(R.drawable.camera_player_score);
            }
            else
                imgPlayerScore.setImageResource(R.drawable.camera_other_player_score);

            return convertView;
        }
    }

    private void updateFoundAnamorphosisImg(View convertView, Player player)
    {
        convertView.findViewById(R.id.camera_player_anam1_img).setVisibility( (player.getNbFoundAnamorphosis() > 0) ? View.VISIBLE : View.INVISIBLE);
        convertView.findViewById(R.id.camera_player_anam2_img).setVisibility( (player.getNbFoundAnamorphosis() > 1) ? View.VISIBLE : View.INVISIBLE);
        convertView.findViewById(R.id.camera_player_anam3_img).setVisibility( (player.getNbFoundAnamorphosis() > 2) ? View.VISIBLE : View.INVISIBLE);
        convertView.findViewById(R.id.camera_player_anam4_img).setVisibility( (player.getNbFoundAnamorphosis() > 3) ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadComponents() {
        cancelImg = (ImageView) findViewById(R.id.camera_act_cancel_img);
        cancelImg.setOnClickListener(this);

        littleAnamorphImg = (ImageView) findViewById(R.id.camera_act_little_anamorph_img);
        littleAnamorphImg.setOnClickListener(this);

        largeAnamorphImg = (ImageView) findViewById(R.id.camera_act_large_anamorph_img);
        largeAnamorphImg.setOnClickListener(this);

        cameraImg = (ImageView) findViewById(R.id.camera_act_camera_img);
        cameraImg.setOnClickListener(this);

        cameraGlassSurfaceView = (CameraGlassSurfaceView) findViewById(R.id.camera_act_grid_img);
        cameraGlassSurfaceView.setOnTouchListener(this);

        textureView = (AutoFitTextureView) findViewById(R.id.camera_act_surface);
    }

    private void loadTargetAnamorphosis() {
        targetAnamorphosis = (Anamorphosis) getIntent().getSerializableExtra("anamorphosis");

        if (targetAnamorphosis == null) {
            showToast("Error : no target anamorphosis defined");
            return;
        }

        littleAnamorphImg.setImageResource(targetAnamorphosis.getDrawableImage());
        largeAnamorphImg.setImageResource(targetAnamorphosis.getLargeDrawableImage());
    }

    private void cancelAnamorphosis(){
        if (!canCancel) {
            showToast("Please wait a few more seconds");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you  really want to try another anamorphosis ?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id)
            {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void searchTargetAnamorphosis() {
        cameraProcessor.captureImage();
    }

    private void toggleLargeAnamorphosisImg() {
        View v = (View) largeAnamorphImg.getParent();
        v.setVisibility(v.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
    }

    private void checkForAlreadyCanceledState() {
        if (!getIntent().getBooleanExtra("alreadyCanceled", false) ||
                getIntent().getBooleanExtra("debug", false)) {
            return;
        }

        canCancel = false;
        cancelImg.setImageResource(R.drawable.camera_cancel_disabled);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        canCancel = true;
                        cancelImg.setImageResource(R.drawable.camera_cancel);
                    }
                });
            }
        }, 30000);
    }

    // Play the sound of a found anamorphosis depending on the number of already found anamorphosis (0 to 3)
    private void playFoundSound()
    {
        int imageId, nbAnamorphosis = getGameClient().getNbFoundAnamorphosis();

        if (nbAnamorphosis == 3)            imageId = R.raw.anam_valid4;
        else if (nbAnamorphosis == 2)       imageId = R.raw.anam_valid3;
        else if (nbAnamorphosis == 1)       imageId = R.raw.anam_valid2;
        else                                imageId = R.raw.anam_valid1;

        MediaPlayer mp = MediaPlayer.create(this, imageId);
        mp.start();
    }

    @Override
    public void onError(String error) {
        showError(error);
    }

    @Override
    public void onImageAvailable(Image img) {
        Log.d("CameraActivity", "Got an image, analysing...");
        int result =
                ObjectDetector.getInstance()
                        .checkForObjects(
                                img,
                                targetAnamorphosis.getDetectorName(),
                                4
                        );

        if (result == -1) {
            showToast("An error occured");
        } else if (result > 0) {
            cameraGlassSurfaceView.displayFeedbackFound();
            playFoundSound();
            // TEST

            //playerListTest.get(0).setNbFoundAnamorphosis(playerListTest.get(0).getNbFoundAnamorphosis()+1);
            //playerListTest.get(0).setScore(playerListTest.get(0).getScore()+3);
            /*
            try {
                getGameClient().setFound(new Anamorphosis(0,0, Anamorphosis.Difficulty.HARD, "Exception"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            setResult(RESULT_OK);
            finish();
        } else {
            cameraGlassSurfaceView.displayFeedbackNotFound();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (view == cameraGlassSurfaceView) {

                boolean focusIsSupported = cameraProcessor.focusOnPoint(
                        motionEvent.getX() / (float) view.getWidth(),
                        motionEvent.getY() / (float) view.getHeight()
                );

                if (focusIsSupported) {
                    cameraGlassSurfaceView.displayFeedbackFocus(
                            (int) motionEvent.getX(),
                            (int) motionEvent.getY()
                    );
                }
            }
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == cancelImg) {
            cancelAnamorphosis();
        } else if (view == littleAnamorphImg){
            toggleLargeAnamorphosisImg();
        } else if (view == largeAnamorphImg) {
            toggleLargeAnamorphosisImg();
        } else if (view == cameraImg) {
            searchTargetAnamorphosis();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity_layout);

        loadComponents();
        loadTargetAnamorphosis();
        checkForAlreadyCanceledState();

        // TEST
        /*
        playerListTest.add(new Player("Loyld", 0, true, "99"));
        playerListTest.add(new Player("Sabine", 0, true, "100"));
        playerListTest.add(new Player("Fouine", 0, true, "101"));
        playerListTest.add(new Player("JF Coppé", 0, true, "102"));
        */
        //

        // Player scores list
        ListView playerList = (ListView) findViewById(R.id.playerScoresListview);
        //adapter = new CameraActivity.CustomAdapter(this, R.layout.camera_player_score_item, playerListTest);
        adapter = new CameraActivity.CustomAdapter(this, R.layout.camera_player_score_item, (ArrayList<Player>) getGameClient().getPlayerList());
        //Given array adapter will be synchronized with the server player list.
        setPlayerAdapter(adapter);
        // adapter.addAll(getGameClient().getPlayerList());
        // adapter.notifyDataSetChanged();
        playerList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        Log.d("CameraActivity", "onResume() start");
        super.onResume();
        cameraProcessor.start(this, textureView);
        Log.d("CameraActivity", "onResume() end");
    }

    @Override
    protected void onPause() {
        Log.d("CameraActivity", "onPause() start");
        super.onPause();
        cameraProcessor.stop();
        Log.d("CameraActivity", "onPause() end");
    }
}
