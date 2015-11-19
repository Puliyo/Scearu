package haradeka.media.scearu.UI;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;

public class MusicActivity extends AppCompatActivity {

    private static final String TAG = "scearu-log";
    private FileHostingService fhs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        fhs = GoogleDrive.INSTANCE;
    }

    @Override
    protected void onStart() {
        super.onStart();
        switch (fhs.connect(this.getApplicationContext())) {
            case GoogleDrive.GOOGLEDRIVE_ACC_PICKER:
                startActivityForResult(((GoogleDrive) fhs).getCredential().newChooseAccountIntent(), GoogleDrive.GOOGLEDRIVE_ACC_PICKER);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        fhs.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GoogleDrive.GOOGLEDRIVE_ACC_PICKER:
                Toast.makeText(this.getApplicationContext(), "ResultCode: " + resultCode, Toast.LENGTH_LONG).show();
                break;
        }
    }
}
