package haradeka.media.scearu.UTILS;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Created by Puliyo on 4/01/2016.
 */
public abstract class BaseMediaController {
    private WifiManager.WifiLock wifiLock;

    public BaseMediaController() {
        wifiLock = ((WifiManager) App.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL,
                        App.getAppContext().getPackageName() + "_WIFILOCK");
    }

    /**
     * Acquire wake lock when music is playing
     */
    public void prepare() {
        wifiLock.acquire();
    }

    /**
     * Acquire wake lock when music is finished
     */
    public void complete() {
        wifiLock.release();
    }

    public void destroy() {}

    public abstract void updatePlayIcon(boolean isPlayed);
    public abstract void updateSongName(String songName);
    public abstract void updateRepeatIcon(MediaService.RepeatState isRepeated);
    public abstract void updateShuffleIcon(boolean isShuffled);
    public abstract void updateNextIcon();
    public abstract void updatePreviousIcon();
}
