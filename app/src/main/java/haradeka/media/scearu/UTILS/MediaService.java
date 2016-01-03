package haradeka.media.scearu.UTILS;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;

/**
 * Created by Puliyo on 21/12/2015.
 */
public class MediaService extends Service implements MyMediaController.MyMediaPlayerControl {
    private FileHostingService fhs;
    private IBinder mBinder = new LocalBinder();

    private MediaPlayer mp = null;
    private AsyncTask prepareMusicTask;
    private int bufferPercentage;

    @Override
    public void onCreate() {
        fhs = GoogleDrive.getInstance();
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                setBufferPercentage(percent);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (prepareMusicTask != null && prepareMusicTask.getStatus() == AsyncTask.Status.RUNNING) {
            prepareMusicTask.cancel(true);
            prepareMusicTask = null;
        }
        if (mp != null) mp.release();
        if (fhs != null) fhs.disconnect();
        Log.d(App.SCEARU_TAG, "Bye Service");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(App.SCEARU_TAG, "BINDED: ");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(App.SCEARU_TAG, "UNBINDED");
        return super.onUnbind(intent);
    }

    /************************************
     * Bind helper Classes / Methods    *
     ************************************/

    public class LocalBinder extends Binder {
        protected MediaService getService() {
            return MediaService.this;
        }
    }

    /**
     * Do not override setOnBufferingUpdateListener() unless absolutely required.
     * Overriding will destroy buffering function.
     * @return
     */
    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    public void setBufferPercentage(int bufferPercentage) {
        this.bufferPercentage = bufferPercentage;
    }

    public void prepare(final int position) {
        if (prepareMusicTask != null && prepareMusicTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(App.SCEARU_TAG, "Current preparing!");
            return;
        }

        prepareMusicTask = new AsyncTask<Object, Void, String>() {

            @Override
            protected String doInBackground(Object... params) {
                return (String) ((GoogleDrive) fhs).getToken(null);
            }

            @Override
            protected void onPostExecute(String s) {
                if (s == null || s.isEmpty()) {
                    Log.d(App.SCEARU_TAG, "Oops! Error! MediaService:1");
                    return; // TODO: Handle token missing error
                }
                String id = fhs.getAdapter(null).getItem(GoogleDrive.HASH_KEY_IDS, position);
                if (id == null || id.isEmpty()) {
                    Log.d(App.SCEARU_TAG, "Oops! Error! MediaService:2");
                    return; // TODO: Handle id missing error
                }
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + s);
                String dlurl = GoogleDrive.getSecureDownloadURL(id);
                Log.d(App.SCEARU_TAG, "LOOOOOOG: " + dlurl);
                try {
                    mp.reset();
                    mp.setDataSource(getBaseContext(), Uri.parse(dlurl), headers);
                    mp.prepareAsync();
                } catch (IOException e) {
                    // TODO: error handling
                    e.printStackTrace();
                }
            }
        };
        prepareMusicTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    /**
     * For future reference.
     * setDataSource(Context, URI, Map) are hidden in API 11.
     * Below code reveals it.
     */
//    private void prepare_v11() {
//        try {
//            Method method = mp.getClass().getMethod("setDataSource", new Class[] { Context.class, Uri.class, Map.class });
//            method.invoke(mp, new Object[] {getBaseContext(), Uri.parse(dlurl), headers});
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//    }


    /************** Media Player Controller **************/

    @Override
    public void start() {
        mp.start();
    }

    @Override
    public void pause() {
        mp.pause();
    }

    @Override
    public int getDuration() {
        try {
        return mp.getDuration();
        } catch (IllegalStateException e) {
            Log.w(App.SCEARU_TAG, "mp state not gracefully handled..");
            return -1;
        }
    }

    @Override
    public int getCurrentPosition() {
        try {
            return mp.getCurrentPosition();
        } catch (IllegalStateException e) {
            Log.w(App.SCEARU_TAG, "mp state not elegantly handled..");
            return -1;
        }
    }

    @Override
    public void seekTo(int pos) {
        mp.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mp.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return bufferPercentage;
    }
}
