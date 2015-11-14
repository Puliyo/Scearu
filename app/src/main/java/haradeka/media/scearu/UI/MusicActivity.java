package haradeka.media.scearu.UI;

import android.content.IntentSender;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import haradeka.media.scearu.DAO.FileHostingService;
import haradeka.media.scearu.DAO.GoogleDrive;
import haradeka.media.scearu.R;

public class MusicActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Scearu";

    private FileHostingService fhs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        fhs = new GoogleDrive(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fhs.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fhs.disconnect();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Successfully connected to google drive");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection to google drive suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, ConnectionResult.RESOLUTION_REQUIRED);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while starting resolution activity", e);
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            Log.e(TAG, "Google Play Service failed to start");
        }
    }
}
