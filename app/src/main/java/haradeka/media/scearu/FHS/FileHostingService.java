package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.ApplicationContext;

/**
 * Created by Puliyo on 14/11/2015.
 */
public abstract class FileHostingService {
    /**
     * SharedPreferences storage name
     */
    public static final String FHS_ACCOUNT_PREFS = "FHS_ACCOUNT_PREFS";
    public static final String ROOT_MEDIA_DIR = "Scearu";
    public static final String MEDIA_DIR_MUSIC = "Music";
    public static final String MEDIA_DIR_VIDEO = "Video";
    public static final String MEDIA_DIR_PICTURE = "Picture";

    /**
     * Methods to do credential / authentication.
     * @param activity Activity to handle UI.
     */
    public abstract void connect(Activity activity);
    public abstract void disconnect();

    /**
     * <i>prepare()</i> will be called to Prepares MediaPlayer.
     *
     * @param activity Used to start intent when Credential / Authentication goes wrong
     * @param player
     * @param adapter
     * @param position Position in <i>adapter</i>
     * @throws IllegalStateException
     * @throws IOException
     */
    public abstract void prepareMedia(Activity activity, MediaPlayer player, FHSAdapter adapter, int position)
            throws IllegalStateException, IOException;

    /**
     * Store account name to SharedPreferences.
     * Caller class is used as key name.
     * @param accountName Account Name to store.
     */
    public void storeAccountName(String accountName) {
        SharedPreferences.Editor editor =
                ApplicationContext.get().getSharedPreferences(
                        FileHostingService.FHS_ACCOUNT_PREFS,
                        Context.MODE_PRIVATE
                ).edit();
        editor.putString(getClass().getSimpleName() + "_account_name", accountName);
        editor.apply();
    }

    /**
     * Get account name stored in SharedPreferences.
     * Caller class is used as key name.
     * @return account name.
     */
    public String getSavedAccountName() {
        SharedPreferences prefs = ApplicationContext.get().getSharedPreferences(
                FileHostingService.FHS_ACCOUNT_PREFS,
                Context.MODE_PRIVATE);
        return prefs.getString(getClass().getSimpleName() + "_account_name", "");
    }

    /**
     * Create Adapter
     * @param context
     * @return
     */
    public abstract FHSAdapter getAdapter(Context context);

    /**
     * Custom adapter used to list media detail in listview.
     * The adapter contains <i>hashMap</i> to where you store list items.
     * The <i>defaultKey</i> will be used as a title on generating list view.
     * When extending, generally, you only have to override <i>update</i> method.
     * Within <i>update</i> method, make change to <i>hashMap</i> to update items.
     */
    public abstract class FHSAdapter extends BaseAdapter {
        private WeakReference<Context> weakContext;
        protected HashMap<String, String[]> hashMap;
        protected String defaultKey;

        /**
         *
         * @param weakContext
         * @param defaultKey Will be used for title when listing view
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

            if (v == null) {
                v = ((LayoutInflater) weakContext.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.media_item, null);
            }

            TextView name = (TextView) v.findViewById(R.id.media_item_text);
            name.setText(hashMap.get(defaultKey)[position]);

            return v;
        }
    }
}
