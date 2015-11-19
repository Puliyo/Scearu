package haradeka.media.scearu;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import haradeka.media.scearu.UI.MusicActivity;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button music_btn = (Button) findViewById(R.id.main_btn_music);

        if (isGooglePlayServicesAvailable() && isDeviceOnline()) {
            music_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(v.getContext(), MusicActivity.class));
                    finish();
                }
            });
        } else {
            music_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext().getApplicationContext(), "No Internet!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        final int statusCode = api.isGooglePlayServicesAvailable(this);

        if (api.isUserResolvableError(statusCode)) {
            api.getErrorDialog(MainActivity.this, statusCode, REQUEST_GOOGLE_PLAY_SERVICES).show();
            return false;
        } else if (statusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}