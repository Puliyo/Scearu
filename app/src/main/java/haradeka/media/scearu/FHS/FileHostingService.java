package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.App;

/**
 * Created by Puliyo on 14/11/2015.
 */
public abstract class FileHostingService {
    /**
     * SharedPreferences storage name
     */
    public static final String FHS_ACCOUNT_PREFS = "FHS_ACCOUNT_PREFS";

    /**
     * Storage dirname
     */
    protected static final String ROOT_MEDIA_DIR = "Scearu";
    protected static final String MEDIA_DIR_MUSIC = "Music";
    protected static final String MEDIA_DIR_VIDEO = "Video";
    protected static final String MEDIA_DIR_PICTURE = "Picture";

    /**
     * Methods to do credential / authentication.
     * @param activity Activity to handle UI.
     */
    public abstract void connect(Activity activity);

    /**
     * Sets FHS ready to be destroyed (back to initialised state).
     */
    public abstract void disconnect();

    /**
     * Interrupt any intermittent or update task.
     */
    public void interrupt() {}

    /**
     * Store account name to SharedPreferences.
     * Caller class is used as key name.
     * @param accountName Account Name to store
     */
    public void storeAccountName(String accountName) {
        SharedPreferences.Editor editor =
                App.getAppContext().getSharedPreferences(
                        FileHostingService.FHS_ACCOUNT_PREFS,
                        Context.MODE_PRIVATE
                ).edit();
        editor.putString(getClass().getSimpleName() + "_account_name", accountName);
        editor.apply();
    }

    /**
     * Get account name stored in SharedPreferences.
     * Caller class is used as key name.
     * @return account name
     */
    public String getSavedAccountName() {
        SharedPreferences prefs = App.getAppContext().getSharedPreferences(
                FileHostingService.FHS_ACCOUNT_PREFS,
                Context.MODE_PRIVATE);
        return prefs.getString(getClass().getSimpleName() + "_account_name", "");
    }

    public FHSAdapter getAdapter() {
        return getAdapter(null);
    }

    /**
     * Gets adapter. Create adapter here if not initialised.
     * @param context Context passed to FHSAdapter
     * @return FHSAdapter
     */
    public abstract FHSAdapter getAdapter(Context context);

    /**
     * Custom adapter used to list media detail in listview.
     * The adapter contains <i>hashMap</i> where you store list items.
     * The <i>defaultKey</i> will be used as a title on generating list view.
     * When extending, generally, you only have to override <i>update</i> method.
     * Within <i>update</i> method, make change to <i>hashMap</i> to update items.
     *
     * Subclass must be designed so the adapter can survive without context.
     */
    public static abstract class FHSAdapter extends BaseAdapter {
        protected WeakReference<Context> weakContext;
        protected HashMap<String, String[]> hashMap;
        protected String defaultKey;

        /**
         *
         * @param weakContext Context used when displaying list item
         * @param defaultKey default key value used for getItem()
         */
        public FHSAdapter(WeakReference<Context> weakContext, String defaultKey) {
            this.weakContext = weakContext;
            this.hashMap = new HashMap<String, String[]>();
            this.defaultKey = defaultKey;
            this.update();
        }

        /**
         * Make sure to preserve defaultKey.
         */
        public abstract void update();

        public void clear() {
            hashMap.clear();
        }

        public void release() {
            hashMap.clear();
            weakContext.clear();
        }

        public void fixContext(Context context) {
            if (weakContext.get() == null && context != null) {
                weakContext = new WeakReference<Context>(context);
            }
        }

        @Override
        public int getCount() {
            String[] values = hashMap.get(defaultKey);
            return (values == null) ? 0 : values.length;
        }

        @Override
        public String getItem(int position) {
            String[] values = hashMap.get(defaultKey);
            return (values == null) ? null : values[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public String getItem(String key, int position) {
            String[] values = hashMap.get(key);
            return (values == null) ? null : values[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Context context = weakContext.get();

            if (context == null) return v;

            if (v == null) {
                v = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.list_media_item, null);
            }

            TextView name = (TextView) v.findViewById(R.id.media_item_text);
            name.setText(hashMap.get(defaultKey)[position]);

            return v;
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
    public static abstract class BaseAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
        private WeakReference<Activity> weakActivity = null;
        private Exception error = null;
        private boolean enforceError = false;

        public BaseAsyncTask(Activity activity) {
            this.weakActivity = new WeakReference<Activity>(activity);
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
        protected void onCancelled(Result result) {
            onCancelled();
        }

        /**
         * Define your own error by overriding this method.
         * @param error The Exception to be processed.
         * @return Return false to allow normal error processing to proceed, true to consume it here.
         */
        protected boolean onCancelled(Exception error) {
            return false;
        }

        @Override
        /**
         * If overriding, always call super method.
         * Also this method will release weakActivity.
         * If require access to activity context,
         * get the context using getActivity() before calling super().
         */
        protected void onPostExecute(Result result) {
            if (isCancelled() || enforceError) { /*&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB */
                // cancel(true) directly calls onCancelled in sdk >= 11.
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
                Log.i(App.SCEARU_TAG, "Missing error");
                Toast.makeText(
                        App.getAppContext(), "Terminating", Toast.LENGTH_SHORT).show();
            } else if (onCancelled(error)) {
                // error defined in sub-class returned correctly.
            } else if (error instanceof CancellationException) { // task.cancel()
                Toast.makeText(App.getAppContext(), "Request timed out.\nCancelling..",
                        Toast.LENGTH_SHORT).show();
            } else if (error instanceof ExecutionException) { // task threw an exception
                Toast.makeText(App.getAppContext(), "Error in task.\nCancelling..",
                        Toast.LENGTH_SHORT).show();
            } else if (error instanceof InterruptedException) { // task interrupted while waiting
                Toast.makeText(App.getAppContext(), "Task interrupted.\nCancelling..",
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.e(App.SCEARU_TAG, "Unhandled Error: "
                        + error.getClass().getSimpleName() + " - " + error.getMessage());
                Toast.makeText(
                        App.getAppContext(),
                        "Error occurred:\n" + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            weakActivity.clear();
        }
    }

    public static void setTimeoutAsyncTask(final AsyncTask task, final long millis) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
                    task.cancel(true);
                }
            }
        }, millis);
    }
}
