package haradeka.media.scearu.UTILS;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import haradeka.media.scearu.FHS.GoogleDrive;

/**
 * Created by Puliyo on 28/12/2015.
 */
public class App extends Application {
    public static final String SCEARU_TAG = "SCEARU_LOG";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }

    public static boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        final int statusCode = api.isGooglePlayServicesAvailable(activity);

        if (api.isUserResolvableError(statusCode)) {
            api.getErrorDialog(activity, statusCode, GoogleDrive.REQUEST_GOOGLE_PLAY_SERVICES).show();
            return false;
        } else if (statusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }
}
