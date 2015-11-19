package haradeka.media.scearu.FHS;

import android.content.Context;

/**
 * Created by Puliyo on 14/11/2015.
 */
public interface FileHostingService {
    /**
     *
     * @param context Make sure to pass in Application Context
     */
    public int connect(Context context);
    public void disconnect();
}
