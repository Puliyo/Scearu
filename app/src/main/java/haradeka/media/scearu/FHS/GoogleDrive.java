package haradeka.media.scearu.FHS;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final String ROOT_SHARE_DIR = "Scearu";
    private final String MEDIA_DIR_MUSIC = "Music";
    private final String MEDIA_DIR_VIDEO = "Video";
    private final String MEDIA_DIR_PICTURE = "Picture";
    private final String DL_URL_PREFIX = "https://www.googleapis.com/drive/v2/files/";
    private final String DL_URL_SUFFIX = "?alt=media";

    private String lastid = "";
    private String weburl = "";
    private String token = "";


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
        if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
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

    public void playMusic(Context context) {
        Log.d("SCEARU_DEBUG", "MP INIT");
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // @TargetApi(14)
                Log.d("SCEARU_DEBUG", "API14");
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + token);
                mp.setDataSource(context, Uri.parse(DL_URL_PREFIX + lastid + DL_URL_SUFFIX), headers);
            } else {
                Log.d("SCEARU_DEBUG", "API<14");
                mp.setDataSource(weburl);
            }
            mp.prepare();
            Log.d("SCEARU_DEBUG", "MP START!");
            mp.start();
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }
    public void playMusic(Activity activity) {
        playMusic(activity.getBaseContext());
    }

    private List<File> getRootDirs(Drive service) throws IOException {
        Drive.Files.List list = service.files().list();
        list.setQ("mimeType = 'application/vnd.google-apps.folder' and fullText contains '" + ROOT_SHARE_DIR + "'");
        return list.execute().getItems();
    }

    private List<File> getMediaDirs(Drive service, final String MEDIA_DIR) throws IOException {
        String parent_search_term = "";
        List<File> rootDirs = getRootDirs(service);
        if (rootDirs == null) return null;
        int size = rootDirs.size();
        if (size == 0) return null;

        for (int i = 1; i < size; i++) {
            parent_search_term += "'" + rootDirs.get(i).getId() + "' in parents or ";
        }
        parent_search_term += "'" + rootDirs.get(0).getId() + "' in parents";

        Drive.Files.List list = service.files().list();
        list.setQ("mimeType = 'application/vnd.google-apps.folder' and (" + parent_search_term + ") and fullText contains '" + MEDIA_DIR + "'");
        return list.execute().getItems();
    }

    private List<File> getMediaFiles(Drive service, final String MEDIA_DIR) throws IOException {
        String parent_search_term = "";
        String media_search_term = "";

        switch (MEDIA_DIR) {
            case MEDIA_DIR_MUSIC:
                media_search_term = "mimeType contains 'audio/'";
                break;
            case MEDIA_DIR_VIDEO:
                media_search_term = "mimeType contains 'video/'";
                break;
            case MEDIA_DIR_PICTURE:
                media_search_term = "mimeType contains 'image/'";
                break;
            default:
                return null;
        }

        List<File> mediaDirs = getMediaDirs(service, MEDIA_DIR);
        if (mediaDirs == null) return null;
        int size = mediaDirs.size();
        if (size == 0) return null;

        for (int i = 1; i < size; i++) {
            parent_search_term += "'" + mediaDirs.get(i).getId() + "' in parents or ";
        }
        parent_search_term += "'" + mediaDirs.get(0).getId() + "' in parents";

        Drive.Files.List list = service.files().list();
        list.setQ(media_search_term + "and (" + parent_search_term + ")");
        return list.execute().getItems();
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
//                Drive.Files.List list = service.files().list();
//                list.setQ("mimeType = 'application/vnd.google-apps.folder' and fullText contains 'Scearu'");
//                FileList fileList = list.execute();
//                List<File> files = fileList.getItems();
                
                List<File> files = getMediaFiles(service, MEDIA_DIR_MUSIC);

                if (files != null && !files.isEmpty()) {
                    for (File file : files) {
                        fileInfo.add(String.format("%s %s\n%s\n\n", file.getTitle(), file.getId(), file.getMimeType()));
                        lastid = file.getId();
                        weburl = file.getWebContentLink();
                    }
                } else {
                    Log.d("SCEARU_DEBUG", "MISSING FILES");
                }
                Log.d("SCEARU_DEBUG", "BACK DONE: " + lastid);
                token = cred.getToken();

                return fileInfo;
            } catch (IOException | IllegalArgumentException | GoogleAuthException e) {
                lastError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        // Called when doInBackground completes
        protected void onPostExecute(List<String> output) {
            Log.d("SCEARU_DEBUG", "Hello Post!");
            if (output != null && output.size() != 0) {
                String str = TextUtils.join("\n", output);
                Log.d("SCEARU_DEBUG", "Output exists: " + str);
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
                    Log.d(GlobalMethods.SCEARU_LOG, "STARTING REQUEST_AUTHORIZATION");
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

//    private String[] getRootIDs(Drive service) throws IOException {
//        final String PREF_KEY = "ROOT_IDS";
//        final String DELIMITER = ";";
//        String[] ids = null;
//        String pref = null;
//
//        // try to get stored id first
//        SharedPreferences prefs = ApplicationContext.get().getSharedPreferences(
//                FileHostingService.FHS_ACCOUNT_PREFS,
//                Context.MODE_PRIVATE);
//        pref = prefs.getString(PREF_KEY, "");
//        if (pref == null || pref.isEmpty()) {
//            List<File> rootDirs = getRootDirs(service);
//            if (rootDirs == null) return null;
//            int size = rootDirs.size();
//            ids = new String[size];
//            for (int i = 0; i < size; i++) {
//                String id = rootDirs.get(i).getId();
//                ids[i] = id;
//                pref += id + DELIMITER;
//            }
//            // store id
//            SharedPreferences.Editor editor =
//                    ApplicationContext.get().getSharedPreferences(
//                            FileHostingService.FHS_ACCOUNT_PREFS,
//                            Context.MODE_PRIVATE
//                    ).edit();
//            editor.putString(PREF_KEY, pref);
//            editor.apply();
//        } else {
//            ids = pref.split(DELIMITER);
//        }
//
//        return ids;
//    }

//    private String[] getMusicIDs(Drive service) throws IOException {
//        final String PREF_KEY = "MUSIC_IDS";
//        final String DELIMITER = ";";
//        String[] ids = null;
//        String pref = null;
//
//        // try to get stored id first
//        SharedPreferences prefs = ApplicationContext.get().getSharedPreferences(
//                FileHostingService.FHS_ACCOUNT_PREFS, Context.MODE_PRIVATE);
//        pref = prefs.getString(PREF_KEY, "");
//        if (pref == null || pref.isEmpty()) {
//            List<File> musicDirs = getMusicDirs(service);
//            if (musicDirs == null) return null;
//            int size = musicDirs.size();
//            ids = new String[size];
//            for (int i = 0; i < size; i++) {
//                String id = musicDirs.get(i).getId();
//                ids[i] = id;
//                pref += id + DELIMITER;
//            }
//            // store id
//            SharedPreferences.Editor editor =
//                    ApplicationContext.get().getSharedPreferences(
//                            FileHostingService.FHS_ACCOUNT_PREFS, Context.MODE_PRIVATE).edit();
//            editor.putString(PREF_KEY, pref);
//            editor.apply();
//        } else {
//            ids = pref.split(DELIMITER);
//        }
//
//        return ids;
//    }

}
