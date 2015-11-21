package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.ApplicationContext;

/**
 * Created by Puliyo on 14/11/2015.
 */
public class GoogleDrive extends FileHostingService {

    private static GoogleDrive instance;

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 10;
    public static final int REQUEST_ACCOUNT_PICKER  = 11;
    public static final int REQUEST_AUTHORIZATION = 12;
    public static final int MISSING_CLIENT_ID = 13;

    private GoogleAccountCredential cred = null;

    private GoogleDrive() {}

    /**
     * GoogleDrive Singleton
     * @return GoogleDrive instance
     */
    public static synchronized GoogleDrive getInstance() {
        if (instance == null) { // double checked locking
            synchronized (GoogleDrive.class) {
                if (instance == null) {
                    instance = new GoogleDrive();
                }
            }
        }
        return instance;
    }

    @Override
    /**
     * Attempt to connect to stored credential first.
     * If fails, requests google account picker.
     */
    public void connect(Activity activity) {
        String accountName;

        if (cred == null) {
            // Declare to use Google API
            cred = GoogleAccountCredential.usingOAuth2(ApplicationContext.get(), Collections.singleton(DriveScopes.DRIVE));
        }

        accountName = cred.getSelectedAccountName();
//        if (accountName == null || accountName.isEmpty()) {
//            accountName = getSavedAccountName();
//            setAccountName(accountName);
//        }

        if (accountName == null || accountName.isEmpty()) {
            // if not logged in to google, start google login activity
            activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            // get google drive file content
            new MakeRequestTask(activity, cred).execute();
        }
    }

    @Override
    public void disconnect() {
    }

    /**
     * Set user account to log in to Google account
     * @param accountName Sets the selected Google account name (e-mail address) -- for example "johndoe@gmail.com"
     */
    public void setAccountName(String accountName) {
        cred.setSelectedAccountName(accountName);
    }

    /**
     * Execute file transaction in background.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private Activity async_act = null;
        private Drive service = null;
        private Exception lastError = null;

        public MakeRequestTask(Activity activity, GoogleAccountCredential credential) {
            service = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
            ).build();
            async_act = activity;
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                List<String> fileInfo = new ArrayList<String>();
                FileList fileList = service.files().list().execute();
                List<File> files = fileList.getItems();
                if (files != null) {
                    for (File file : files) {
                        fileInfo.add(String.format("%s %s\n", file.getTitle(), file.getId()));
                    }
                }
                return fileInfo;
            } catch (IOException | IllegalArgumentException e) {
                lastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        // Called when doInBackground completes
        protected void onPostExecute(List<String> output) {
            if (output != null && output.size() != 0) {
                String str = TextUtils.join("\n", output);
                TextView tview = (TextView) async_act.findViewById(R.id.textView2);
                tview.setText(str);
            }
        }

        @Override
        // Called when doInBackground calls cancel(true).
        // Once cancelled, onPostExecute will not be called.
        protected void onCancelled() {
            if (lastError != null) {
                if (lastError instanceof GooglePlayServicesAvailabilityIOException) {
                    // Something gone wrong with Google Play Service
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) lastError).getConnectionStatusCode(),
                            async_act,
                            REQUEST_GOOGLE_PLAY_SERVICES);
                    dialog.show();
                } else if (lastError instanceof UserRecoverableAuthIOException) {
                    // Need Authorisation. Call activity to recover authorisation.
                    async_act.startActivityForResult(
                            ((UserRecoverableAuthIOException) lastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else if (lastError instanceof IllegalArgumentException) {
                    // Account name is invalid. Redo login.
                    async_act.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                } else if (lastError instanceof GoogleAuthIOException) {
                    // Missing Client ID in Developer Console
                    async_act.setResult(MISSING_CLIENT_ID);
                } else {
                    Toast.makeText(
                            ApplicationContext.get(),
                            "Error occurred:\n" + lastError.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(
                        ApplicationContext.get(),
                        "Request Cancelled",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}
