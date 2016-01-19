package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
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
import java.util.Collections;
import java.util.List;

import haradeka.media.scearu.UTILS.App;

/**
 * Created by Puliyo on 14/11/2015.
 */
public class GoogleDrive extends FileHostingService {

    private static GoogleDrive instance;

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 10;
    public static final int REQUEST_ACCOUNT_PICKER  = 11;
    public static final int REQUEST_AUTHORIZATION = 12;
//    public static final int MISSING_CLIENT_ID = 13;
    private static final String DL_URL_PREFIX_SECURE = "https://www.googleapis.com/drive/v2/files/";
    private static final String DL_URL_SUFFIX_SECURE = "?alt=media";
    private static final String DL_URL_PREFIX_PUBLIC = "https://drive.google.com/uc?export=download&id=";
    private static final String DL_URL_SUFFIX_PUBLIC = "";
    public static final String HASH_KEY_NAMES = "names";
    public static final String HASH_KEY_IDS = "ids";

    private GoogleAccountCredential cred = null;
    private BaseAsyncTask taskRequestFiles = null;
    private List<File> driveFiles = null;
    private GoogleDriveAdapter driveAdapter = null;
    private Object lock = new Object();


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
            cred = GoogleAccountCredential.usingOAuth2(App.getAppContext(), Collections.singleton(DriveScopes.DRIVE));
        }

        accountName = cred.getSelectedAccountName();
        Log.d("SCEARU_DEBUG", "" + accountName);
//        if (accountName == null || accountName.isEmpty()) {
//            accountName = getSavedAccountName();
//            setAccountName(accountName);
//        }

        if (accountName == null || accountName.isEmpty()) {
            // if not logged in to google, start google login activity
            activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            setDriveFiles(activity);
        }
    }

    @Override
    public void disconnect() {
        interrupt();
        if (driveAdapter != null) {
            driveAdapter.release();
            driveAdapter = null;
        }
        if (driveFiles != null) {
            driveFiles.clear();
        }
    }

    @Override
    public void interrupt() {
        if (taskRequestFiles != null && taskRequestFiles.getStatus() == AsyncTask.Status.RUNNING) {
            taskRequestFiles.cancel(true);
            taskRequestFiles = null;
            Log.d(App.SCEARU_TAG, "gDrive Interrupt kill task");
        }
    }

    @Override
    /**
     * Set user account to log in to Google account
     * @param accountName Sets the selected Google account name (e-mail address) -- for example "johndoe@gmail.com"
     */
    public void storeAccountName(String accountName) {
        if (cred != null) { cred.setSelectedAccountName(accountName); }
        super.storeAccountName(accountName);
    }

    @Override
    public FHSAdapter getAdapter(Context context) {
        synchronized (lock) {
            if (driveAdapter == null) {
                driveAdapter = new GoogleDriveAdapter(context, HASH_KEY_NAMES);
            }
            driveAdapter.fixContext(context);
            return driveAdapter;
        }
    }

    public String getToken(Activity activity) {
        try {
            return cred.getToken();
        } catch (IOException | GoogleAuthException e) {
            mOnCancelled(e, activity);
            return null;
        }
    }

    public static String getSecureDownloadURL(String id) {
        return DL_URL_PREFIX_SECURE + id + DL_URL_SUFFIX_SECURE;
    }

    public static String getPublicDownloadURL(String id) {
        return DL_URL_PREFIX_PUBLIC + id + DL_URL_SUFFIX_PUBLIC;
    }

    private class GoogleDriveAdapter extends FHSAdapter {

        public GoogleDriveAdapter(Context context, String defaultKey) {
            super(new WeakReference<Context>(context), defaultKey);
        }

        @Override
        public void update() {
            int size = (driveFiles == null) ? 0 : driveFiles.size();
            hashMap.clear();
            String[] names = new String[size];
            String[] ids = new String[size];
            for (int i = 0; i < size; i++) {
                File file = driveFiles.get(i);
                names[i] = file.getTitle();
                ids[i] = file.getId();
            }
            hashMap.put(HASH_KEY_NAMES, names);
            hashMap.put(HASH_KEY_IDS, ids);

            this.notifyDataSetChanged();
        }
    }

    private List<File> getRootDirs(Drive service) throws IOException {
        Drive.Files.List list = service.files().list();
        list.setQ("mimeType = 'application/vnd.google-apps.folder' and fullText contains '" + ROOT_MEDIA_DIR + "'");
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
        String media_search_term;

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
     * Set google drive files.
     * @param activity
     * @return
     */
    private void setDriveFiles(Activity activity) {
        if (taskRequestFiles != null && taskRequestFiles.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(App.SCEARU_TAG, "Connection task already running!");
            return;
        }
        taskRequestFiles = new BaseAsyncTask<Object, Void, List<File>>(activity) {

            @Override
            protected List<File> doInBackground(Object... params) {
                if (driveFiles != null) driveFiles.clear();
                try {
                    Drive service = new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            JacksonFactory.getDefaultInstance(),
                            cred
                    ).setApplicationName("Scearu Google Drive").build();

                    driveFiles = getMediaFiles(service, MEDIA_DIR_MUSIC);
                } catch (Exception e) {
                    driveFiles = null;
                    setError(e, true);
                }
                return driveFiles;
            }

            @Override
            protected boolean onCancelled(Exception error) {
                // task interrupted while connecting to Google Drive. Ignore error.
                if (error instanceof IOException) {
                    Throwable t = error.getCause();
                    if (t != null && t instanceof InterruptedException) {
                        Log.i(App.SCEARU_TAG, "Terminating Drive Access!");
                        return true;
                    }
                } else {
                    return mOnCancelled(error, getActivity());
                }
                return false;
            }

            @Override
            protected void onPostExecute(List<File> files) {
                super.onPostExecute(files);
                if (driveFiles == null || driveFiles.isEmpty()) return;
                driveAdapter.update();
            }

        };
        taskRequestFiles.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        setTimeoutAsyncTask(taskRequestFiles, 30000);
    }

    private boolean mOnCancelled(Exception error, Activity activity) {
        if (activity != null && error instanceof GooglePlayServicesAvailabilityIOException) {
            // Something gone wrong with Google Play Service
            GooglePlayServicesUtil.getErrorDialog(
                    ((GooglePlayServicesAvailabilityIOException) error).getConnectionStatusCode(),
                    activity, REQUEST_GOOGLE_PLAY_SERVICES).show();
            return true;
        } else if (activity != null && error instanceof UserRecoverableAuthIOException) {
            // Need Authorisation. Call activity to recover authorisation.
            activity.startActivityForResult(
                    ((UserRecoverableAuthIOException) error).getIntent(),
                    REQUEST_AUTHORIZATION);
            return true;
        } else if (activity != null && error instanceof IllegalArgumentException) {
            // Account name is invalid. Redo login.
            activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            return true;
        } else if (error instanceof GoogleAuthIOException) {
            // Missing Client ID in Developer Console
            Toast.makeText(App.getAppContext(), "Credential Error!\nReport Bug!", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }
}
