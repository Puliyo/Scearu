package haradeka.media.scearu.FHS;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import haradeka.media.scearu.UTILS.ApplicationContext;

/**
 * Created by Puliyo on 14/11/2015.
 */
public abstract class FileHostingService {
    /**
     * SharedPreferences storage name
     */
    public static final String FHS_ACCOUNT_PREFS = "FHS_ACCOUNT_PREFS";

    /**
     * Methods to be implemented by subclass.
     * @param activity Activity to handle UI.
     */
    public abstract void connect(Activity activity);
    public abstract void disconnect();

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
}
