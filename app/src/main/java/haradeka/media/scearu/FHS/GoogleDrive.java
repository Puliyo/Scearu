package haradeka.media.scearu.FHS;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ListView;
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
    private static final String DL_URL_PREFIX = "https://www.googleapis.com/drive/v2/files/";
    private static final String DL_URL_SUFFIX = "?alt=media";
    private static final String HASH_KEY_NAMES = "names";
    private static final String HASH_KEY_IDS = "ids";
    private static final String HASH_KEY_WEBLINKS = "weblinks";

    private GoogleAccountCredential cred = null;
    private BaseAsyncTask taskRequestFiles = null;
    private BaseAsyncTask taskRequestToken = null;
    private List<File> driveFiles = null;
    private GoogleDriveAdapter driveAdapter = null;


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
            // if not logged in to google, start google login activity
            activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            setDriveFiles(activity);
        }
    }

    @Override
    public void disconnect() {
        if (taskRequestFiles != null && taskRequestFiles.getStatus() == AsyncTask.Status.RUNNING) {
            taskRequestFiles.cancel(true);
            taskRequestFiles = null;
        }
        if (taskRequestToken != null && taskRequestToken.getStatus() == AsyncTask.Status.RUNNING) {
            taskRequestToken.cancel(true);
            taskRequestToken = null;
        }
        if (driveAdapter != null) {
            driveAdapter.release();
            driveAdapter = null;
        }
    }

    @Override
    public synchronized FHSAdapter getAdapter(Context context) {
        if (driveAdapter == null) {
            driveAdapter = new GoogleDriveAdapter(context, HASH_KEY_NAMES);
        }
        return driveAdapter;
    }

    @TargetApi(14)
    @Override
    public void prepareMedia(final Activity activity, final MediaPlayer player, final FHSAdapter adapter, final int position)
            throws IllegalStateException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Log.d("SCEARU_DEBUG", "API14");
            if (taskRequestToken != null && taskRequestToken.getStatus() == AsyncTask.Status.RUNNING) {
                Log.i(GlobalMethods.SCEARU_LOG, "Current preparing!");
                return;
            }
            // TODO: Task manager to ensure on task instance running at a time
            taskRequestToken = new BaseAsyncTask<Object, Void, String>(new WeakReference<Activity>(activity)) {
                @Override
                protected String doInBackground(Object... params) {
                    try { return cred.getToken(); }
                    catch (IOException | GoogleAuthException e) { setError(e, true); }
                    return null;
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    if (s == null || s.isEmpty()) return;
                    Log.d("SCEARU_DEBUG", "Got token: " + s);
                    String id = adapter.getItem(HASH_KEY_IDS, position);
                    if (id == null || id.isEmpty()) return;
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Authorization", "Bearer " + s);
                    try {
                        player.setDataSource(activity, Uri.parse(DL_URL_PREFIX + id + DL_URL_SUFFIX), headers);
                        player.prepare();
                    } catch (IOException e) {
                        // TODO: Handle invalid url
                        e.printStackTrace();
                    }
                }
            };
            taskRequestToken.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        } else { // API < 14
            // TODO: API < 14 is not working
            String weburl = adapter.getItem(HASH_KEY_WEBLINKS, position);
            Log.d(GlobalMethods.SCEARU_LOG, weburl);
            if (weburl == null || weburl.isEmpty()) return;
            player.setDataSource(weburl);
            player.prepare();
        }
        // TODO: prepare -> prepareAsync
    }

    public class GoogleDriveAdapter extends FHSAdapter {

        public GoogleDriveAdapter(Context context, String defaultKey) {
            super(new WeakReference<Context>(context), defaultKey);
        }

        @Override
        public void update() {
            int size = (driveFiles == null) ? 0 : driveFiles.size();
            hashMap.clear();
            String[] names = new String[size];
            String[] ids = new String[size];
            String[] weblinks = new String[size];
            for (int i = 0; i < size; i++) {
                File file = driveFiles.get(i);
                names[i] = file.getTitle();
                ids[i] = file.getId();
                weblinks[i] = file.getWebContentLink();
            }
            hashMap.put(HASH_KEY_NAMES, names);
            hashMap.put(HASH_KEY_IDS, ids);
            hashMap.put(HASH_KEY_WEBLINKS, weblinks);

            this.notifyDataSetChanged();
        }
    }

    /**
     * Set user account to log in to Google account
     * @param accountName Sets the selected Google account name (e-mail address) -- for example "johndoe@gmail.com"
     */
    public void setAccountName(String accountName) {
        cred.setSelectedAccountName(accountName);
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
     * Set google drive files.
     * @param activity
     * @return
     */
    private void setDriveFiles(Activity activity) {
        if (taskRequestFiles != null && taskRequestFiles.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(GlobalMethods.SCEARU_LOG, "Connection task already running!");
            return;
        }
        taskRequestFiles = new BaseAsyncTask<Object, Void, List<File>>(new WeakReference<Activity>(activity)) {
            @Override
            protected List<File> doInBackground(Object... params) {
                if (driveFiles != null) driveFiles.clear();
                try {
                    Drive service = new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            JacksonFactory.getDefaultInstance(),
                            cred
                    ).setApplicationName("Scearu Google Drive").build();
                    List<String> fileInfo = new ArrayList<String>();

                    driveFiles = getMediaFiles(service, MEDIA_DIR_MUSIC);
                } catch (IOException | IllegalArgumentException e) {
                    driveFiles = null;
                    setError(e, true);
                }
                return driveFiles;
            }

            @Override
            protected void onPostExecute(List<File> files) {
                super.onPostExecute(files);
                if (driveFiles == null || driveFiles.isEmpty()) return;

                driveAdapter.update();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // @TargetApi 11
            taskRequestFiles.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        } else {
            taskRequestFiles.execute();
        }
    }

    /**
     * Base AsyncTask which does activity and error handling for you.
     * Beware of <i>cannot be cast to ...</i> error.
     * http://stackoverflow.com/questions/20455644/object-cannot-be-cast-to-void-in-asynctask
     * @param <Params>
     * @param <Progress>
     * @param <Result>
     */
    private abstract class BaseAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
        private WeakReference<Activity> weakActivity = null;
        private Exception error = null;
        private boolean enforceError = false;

        public BaseAsyncTask(WeakReference<Activity> weakActivity) {
            this.weakActivity = weakActivity;
        }

        protected Activity getActivity() {
            return weakActivity.get();
        }

        protected Exception getError() {
            return error;
        }

        protected void setError(Exception e) {
            setError(e, false);
        }

        protected void setError(Exception e, boolean enforce) {
            error = e;
            enforceError = enforce;
        }

        @Override
        /**
         * If overriding, always call super method.
         * Also this method will release weakActivity. Use getActivity() before getting released.
         */
        protected void onPostExecute(Result result) {
            if (isCancelled() || enforceError) { /*&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB */
                // cancel(true) directly calls onCancelled in sdk >= 11.
                // sdk < 11 does not.
                onCancelled();
            } else {
                weakActivity.clear();
            }
        }

        @Override
        /**
         * If overriding, always call super method.
         * onCancelled() is called if boolean enforceError is true.
         */
        protected void onCancelled() {
            if (error == null) {
                Log.i(GlobalMethods.SCEARU_LOG, "Unhandled error");
                Toast.makeText(
                        ApplicationContext.get(), "Terminating", Toast.LENGTH_SHORT).show();
                return;
            }
            Activity activity = weakActivity.get();
            if (activity == null) {
                Toast.makeText(
                        ApplicationContext.get(), "Activity not found", Toast.LENGTH_SHORT).show();
                return;
            }
            if (error instanceof GooglePlayServicesAvailabilityIOException) {
                // Something gone wrong with Google Play Service
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) error).getConnectionStatusCode(),
                        activity,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            } else if (error instanceof UserRecoverableAuthIOException) {
                // Need Authorisation. Call activity to recover authorisation.
                Log.d(GlobalMethods.SCEARU_LOG, "STARTING REQUEST_AUTHORIZATION");
                activity.startActivityForResult(
                        ((UserRecoverableAuthIOException) error).getIntent(),
                        REQUEST_AUTHORIZATION);
            } else if (error instanceof IllegalArgumentException) {
                // Account name is invalid. Redo login.
                activity.startActivityForResult(cred.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            } else if (error instanceof GoogleAuthIOException) {
                // Missing Client ID in Developer Console
                Toast.makeText(ApplicationContext.get(), "Credential Error!\nReport Bug!", Toast.LENGTH_LONG).show();
            } else {
                // TODO: Handle task.cancel() exception
                Toast.makeText(
                        ApplicationContext.get(),
                        "Error occurred:\n" + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            weakActivity.clear();
        }
    }
}
