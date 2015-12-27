package haradeka.media.scearu.UTILS;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.MediaController;

import java.io.IOException;
import java.util.HashMap;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;

/**
 * Created by Puliyo on 21/12/2015.
 */
public class MediaService extends Service implements MediaController.MediaPlayerControl {
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
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(GlobalMethods.SCEARU_LOG, "ONERROR: " + what);
                Log.d(GlobalMethods.SCEARU_LOG, "ONERROR2: " + extra);
                return false;
            }
        });
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
        Log.d(GlobalMethods.SCEARU_LOG, "Bye Service");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(GlobalMethods.SCEARU_LOG, "UNBINDED");
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

    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    public synchronized void setBufferPercentage(int bufferPercentage) {
        this.bufferPercentage = bufferPercentage;
    }

    public void prepare(final int position) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            prepare_v14(position);
        } else {
            prepare_v1(position);
        }
    }

    // TODO: prepare_v1 not working
    private void prepare_v1(final int position) {
        String weburl = fhs.getAdapter(null).getItem(GoogleDrive.HASH_KEY_WEBLINKS, position);
        Log.d(GlobalMethods.SCEARU_LOG, weburl);
        if (weburl == null || weburl.isEmpty()) return;
        try {
            mp.setDataSource(weburl);
        } catch (IOException e) {
            // TODO:
            Log.d(GlobalMethods.SCEARU_LOG, "Oops! Error! MediaService:4");
            e.printStackTrace();
        }
        mp.prepareAsync();
    }

    @TargetApi(14)
    private void prepare_v14(final int position) {
        if (prepareMusicTask != null && prepareMusicTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(GlobalMethods.SCEARU_LOG, "Current preparing!");
            return;
        }

        prepareMusicTask = new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                return (String) ((GoogleDrive) fhs).getToken(null);
            }

            @Override
            protected void onPostExecute(String s) {
                if (s == null || s.isEmpty()) {
                    Log.d(GlobalMethods.SCEARU_LOG, "Oops! Error! MediaService:1");
                    return; // TODO: Handle token missing error
                }
                String id = fhs.getAdapter(null).getItem(GoogleDrive.HASH_KEY_IDS, position);
                if (id == null || id.isEmpty()) {
                    Log.d(GlobalMethods.SCEARU_LOG, "Oops! Error! MediaService:2");
                    return; // TODO: Handle id missing error
                }
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + s);
                try {
                    mp.setDataSource(getBaseContext(), Uri.parse(GoogleDrive.generateDownloadURL(id)), headers);
                    mp.prepareAsync();
                } catch (IOException e) {
                    // TODO: Handle invalid url
                    Log.d(GlobalMethods.SCEARU_LOG, "Oops! Error! MediaService:3");
                    e.printStackTrace();
                }
            }
        };
        prepareMusicTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

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
        return mp.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mp.getCurrentPosition();
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

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return mp.getAudioSessionId();
    }

}
