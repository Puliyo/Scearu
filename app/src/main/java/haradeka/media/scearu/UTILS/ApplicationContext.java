package haradeka.media.scearu.UTILS;

import android.content.Context;

/**
 * Created by Puliyo on 21/11/2015.
 */
public class ApplicationContext {

    private static ApplicationContext instance;
    private Context appContext;

    private ApplicationContext() {}

    public static ApplicationContext getInstance() {
        return instance == null ? (instance = new ApplicationContext()) : instance;
    }

    public void init(Context context) {
        if(appContext == null){
            appContext = context;
        }
    }

    public static Context get() {
        return getInstance().getContext();
    }

    private Context getContext() {
        return appContext;
    }
}