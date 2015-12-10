package haradeka.media.scearu.UI;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.GlobalMethods;

// TODO: HTTPclient to login for google-account ..

public class MusicActivity extends AppCompatActivity {

    private FileHostingService fhs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        fhs = GoogleDrive.getInstance();
        fhs.connect(this);

        Button music_btn = (Button) findViewById(R.id.button2);
        music_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((GoogleDrive) fhs).playMusic(getBaseContext());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
//        fhs.disconnect();
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
