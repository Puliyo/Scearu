package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

/**
 * Created by Puliyo on 14/11/2015.
 */
public enum GoogleDrive implements FileHostingService {
    INSTANCE;

    public static final int GOOGLEDRIVE_ACC_PICKER = 1001;
    private GoogleAccountCredential cred;
    private Drive service;
    private GoogleApiClient gapi;

    private GoogleDrive() {}

    @Override
    public int connect(Context context) {
        if (cred == null) {
            cred = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
        }
        if (cred.getSelectedAccountName() == null) {
            return GOOGLEDRIVE_ACC_PICKER;
        }
//        service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), cred).build();
//        gapi.connect();
        return 0;
    }

    @Override
    public void disconnect() {
//        gapi.disconnect();
    }

    public GoogleAccountCredential getCredential() {
        return cred;
    }

}
