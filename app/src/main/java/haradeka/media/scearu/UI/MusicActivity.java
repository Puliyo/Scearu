package haradeka.media.scearu.UI;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.GlobalMethods;

public class MusicActivity extends AppCompatActivity {

    private static final String TAG = "scearu-log";
    private FileHostingService fhs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        fhs = GoogleDrive.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fhs.connect(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
//        fhs.disconnect();
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
                        ((GoogleDrive) fhs).storeAccountName(accountName);
                        Toast.makeText(this.getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                        fhs.connect(this);
                    }
                } else {
                    Toast.makeText(this.getApplicationContext(), "Error logging in!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case GoogleDrive.REQUEST_AUTHORIZATION:
                Log.d("SCEARU_DEBUG", "REQUEST_AUTHORIZATION");
                if (resultCode != RESULT_OK) {
                    fhs.connect(this);
                }
                break;
            case GoogleDrive.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    GlobalMethods.isGooglePlayServicesAvailable(this);
                }
                break;
            case GoogleDrive.MISSING_CLIENT_ID:
                Toast.makeText(this.getApplicationContext(), "Credential Error!", Toast.LENGTH_LONG).show();
                finish();
                break;
        }
    }
}
