package haradeka.media.scearu.DAO;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

/**
 * Created by Puliyo on 14/11/2015.
 */
public class GoogleDrive implements FileHostingService {
    private GoogleApiClient gapi;

    public GoogleDrive(Context context) {
        gapi = new GoogleApiClient.Builder(context)
                .addApi(Drive.API).addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) context)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) context)
                .build();
    }

    @Override
    public void connect() {
        gapi.connect();
    }

    @Override
    public void disconnect() {
        gapi.disconnect();
    }

}
