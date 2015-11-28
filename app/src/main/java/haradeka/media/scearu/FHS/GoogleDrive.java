package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.ApplicationContext;
import haradeka.media.scearu.UTILS.GlobalMethods;

/**
 * Created by Puliyo on 14/11/2015.
 */
public class GoogleDrive extends FileHostingService {

    private static GoogleDrive instance;

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 10;
    public static final int REQUEST_ACCOUNT_PICKER  = 11;
    public static final int REQUEST_AUTHORIZATION = 12;
//    public static final int MISSING_CLIENT_ID = 13;

    private GoogleAccountCredential cred = null;
    private MakeRequestTask task = null;

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
        Log.d("SCEARU_DEBUG", "" + accountName);
//        if (accountName == null || accountName.isEmpty()) {
//            accountName = getSavedAccountName();
//            setAccountName(accountName);
//        }

        if (accountName == null || accountName.isEmpty()) {
            Log.d("SCEARU_DEBUG", "CALLING REQUEST_ACCOUNT_PICKER");
            // if not logged in to google, start google login activity
            activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            if (task == null || task.getStatus() != AsyncTask.Status.RUNNING) {
                // get google drive file content
                Log.d(GlobalMethods.SCEARU_LOG, "CALLING ASYNCTASK!");
                task = new MakeRequestTask(activity, cred);
                task.execute();
            } else {
                Log.i(GlobalMethods.SCEARU_LOG, "Connection task already running!");
            }
        }
    }

    @Override
    public void disconnect() {
        if (task != null || task.getStatus() == AsyncTask.Status.RUNNING) {
            task.cancel(true);
        }
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
        private WeakReference<Activity> activity_ref = null;
        private Drive service = null;
        private Exception lastError = null;

        public MakeRequestTask(Activity activity, GoogleAccountCredential credential) {
            service = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("Scearu Google Drive").build();
            activity_ref = new WeakReference<Activity>(activity);
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
                TextView tview = (TextView) activity_ref.get().findViewById(R.id.textView2);
                tview.setText(str);
            } else if (isCancelled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                // cancel(true) directly calls onCancelled in sdk >= 11.
                // for sdk < 11, check cancel in onPostExecute.
                Log.d("SCEARU_DEBUG", "CALLING ONCANCELLED");
                onCancelled();
            }
        }

        @Override
        // Called when doInBackground calls cancel(true).
        // Once cancelled, onPostExecute will not be called.
        protected void onCancelled() {
            if (lastError != null) {
                Activity activity = activity_ref.get();

                if (lastError instanceof GooglePlayServicesAvailabilityIOException) {
                    // Something gone wrong with Google Play Service
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) lastError).getConnectionStatusCode(),
                            activity,
                            REQUEST_GOOGLE_PLAY_SERVICES);
                    dialog.show();
                } else if (lastError instanceof UserRecoverableAuthIOException) {
                    // Need Authorisation. Call activity to recover authorisation.
                    Log.d("SCEARU_DEBUG", "STARTING REQUEST_AUTHORIZATION");
                    activity.startActivityForResult(
                            ((UserRecoverableAuthIOException) lastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else if (lastError instanceof IllegalArgumentException) {
                    // Account name is invalid. Redo login.
                    activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                } else if (lastError instanceof GoogleAuthIOException) {
                    // Missing Client ID in Developer Console
                    Toast.makeText(ApplicationContext.get(), "Credential Error!\nReport Bug!", Toast.LENGTH_LONG).show();
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
