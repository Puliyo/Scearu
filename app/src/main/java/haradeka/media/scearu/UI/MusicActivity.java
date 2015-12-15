package haradeka.media.scearu.UI;

import android.accounts.AccountManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.GlobalMethods;

public class MusicActivity extends AppCompatActivity {

    private FileHostingService fhs;
    private FileHostingService.FHSAdapter fhsAdapter;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });

        fhs = GoogleDrive.getInstance();

        ListView mediaFiles = (ListView) findViewById(R.id.music_list_files);
        mediaFiles.setEmptyView(findViewById(R.id.music_tview_empty));

        fhsAdapter = fhs.getAdapter(getBaseContext());
        mediaFiles.setAdapter(fhsAdapter);
        mediaFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), fhsAdapter.getItem(position), Toast.LENGTH_SHORT).show();
                try {
                    fhs.prepareMedia(MusicActivity.this, mp, fhsAdapter, position);
//                    playMusic();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        fhs.connect(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        fhs.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GoogleDrive.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        ((GoogleDrive) fhs).setAccountName(accountName);
                        fhs.storeAccountName(accountName);
                        Toast.makeText(this.getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                        fhs.connect(this);
                    }
                } else {
                    Log.d("SCEARU_DEBUG", "REQUEST_ACCOUNT_PICKER");
                    Toast.makeText(this.getApplicationContext(), "Error logging in!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case GoogleDrive.REQUEST_AUTHORIZATION:
                Log.d("SCEARU_DEBUG", "REQUEST_AUTHORIZATION");
                if (resultCode != RESULT_OK) {
                    Log.d("SCEARU_DEBUG", "REQUEST_AUTHORIZATION FAILED");
                    Toast.makeText(this.getApplicationContext(), "Error authenticating!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    fhs.connect(this);
                }
                break;
            case GoogleDrive.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    GlobalMethods.isGooglePlayServicesAvailable(this);
                }
                break;
//            case GoogleDrive.MISSING_CLIENT_ID:
//                Toast.makeText(this.getApplicationContext(), "Credential Error!", Toast.LENGTH_LONG).show();
//                finish();
//                break;
        }
    }
}
