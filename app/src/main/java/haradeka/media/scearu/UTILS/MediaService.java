package haradeka.media.scearu.UTILS;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;

/**
 * Created by Puliyo on 21/12/2015.
 */
public class MediaService extends Service {
    private static final String PREFACCOUNTNAME = "mediaservice_account_name";
    public static final String SHUFFLE_ACTION = "haradeka.media.scearu.shuffle";
    public static final String REPEAT_ACTION = "haradeka.media.scearu.repeat";
    public static final String PREVIOUS_ACTION = "haradeka.media.scearu.previous";
    public static final String PLAY_ACTION = "haradeka.media.scearu.play";
    public static final String NEXT_ACTION = "haradeka.media.scearu.next";
    public static final String SELECTION_ACTION = "haradeka.media.scearu.selection";
    public static final String DESTROY_ACTION = "haradeka.media.scearu.destroy";

    private BroadCastListener receiver;
    private FileHostingService fhs;
    private IBinder mBinder = new LocalBinder();

    private MediaPlayer mp = null;
    private BaseMediaController mediaController;
    private AsyncTask prepareMusicTask;
    private Random random;
    private int bufferPercentage;
    private int itemposition = -1;
    private boolean shuffle;
    private RepeatState repeat;
    public enum RepeatState {
        REPEAT, REPEATONE, NOREPEAT
    }
    private List<Integer> whatToRepeat;

    @Override
    public void onCreate() {
        Log.d(App.SCEARU_TAG, "Hello Service");
        /* FHS */
        fhs = GoogleDrive.getInstance();

        /* Music */
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaController.updateSongName(fhs.getAdapter().getItem(itemposition));
                mediaController.prepare();
                mp.start();
                mediaController.updatePlayIcon(mp.isPlaying());
            }
        });
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                setBufferPercentage(percent);
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaController.complete();
                switch (repeat) {
                    case REPEATONE:
                        if (shuffle) {
                            // if shuffle + repeatOne = Randomly play all songs once.
                            if (whatToRepeat == null) { // initialise whatToRepeat
                                whatToRepeat = new ArrayList<Integer>();
                                int length = fhs.getAdapter().getCount();
                                for (int i = 0; i < length; i++) {
                                    if (i == itemposition) continue;
                                    whatToRepeat.add(i);
                                }
                            } else if (whatToRepeat.isEmpty()) {
                                whatToRepeat = null;
                                break;
                            }
                            int item = random.nextInt(whatToRepeat.size());
                            int nextpos = whatToRepeat.remove(item);
                            Log.d(App.SCEARU_TAG, "whatToRepeat: " + whatToRepeat.toString());
                            playSong(nextpos);
                        } else {
                            // if noShuffle + repeatOne = repeat current song.
                            mp.start();
                        }
                        break;
                    case REPEAT:
                        playSong(nextSong());
                        break;
                    case NOREPEAT:
                        whatToRepeat = null;
                        break;
                }
                Log.d(App.SCEARU_TAG, "completionListener complete");
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(App.SCEARU_TAG, "ONERROR: " + what);
                Log.d(App.SCEARU_TAG, "ONERROR2: " + extra);
                return false;
            }
        });
        mp.setWakeMode(App.getAppContext(), PowerManager.PARTIAL_WAKE_LOCK);

        setPreferences();
        random = new Random();
    }

    @Override
    public void onDestroy() {
        if (prepareMusicTask != null && prepareMusicTask.getStatus() == AsyncTask.Status.RUNNING) {
            prepareMusicTask.cancel(true);
            prepareMusicTask = null;
        }
        if (mediaController != null) mediaController.destroy();
        if (mp != null) mp.release();
        if (fhs != null) fhs.disconnect();
        stopForeground(true);
        if (receiver != null) unregisterReceiver(receiver);
        storePreferences();
        Log.d(App.SCEARU_TAG, "Bye Service");
        super.onDestroy();
    }

    @Override
    /**
     * Called when Activty binds.
     * If notification is displayed, stop it.
     */
    public IBinder onBind(Intent intent) {
        Log.d(App.SCEARU_TAG, "BINDED");
        destroyNotification();
        return mBinder;
    }

    @Override
    /**
     * Called when all Activity unbinds.
     * If mediaplayer is playing, display notification.
     * Otherwise stop service.
     */
    public boolean onUnbind(Intent intent) {
        Log.d(App.SCEARU_TAG, "UNBINDED");
        if (mp.isPlaying()) {
            /* BroadCast */
            receiver = new BroadCastListener();
            IntentFilter filter = new IntentFilter();
            filter.addAction(SHUFFLE_ACTION);
            filter.addAction(REPEAT_ACTION);
            filter.addAction(PREVIOUS_ACTION);
            filter.addAction(PLAY_ACTION);
            filter.addAction(NEXT_ACTION);
            filter.addAction(DESTROY_ACTION);
            registerReceiver(receiver, filter);

            /* Notification */
            if (mediaController != null) {
                mediaController.destroy();
                mediaController = null;
            }
            setMediaController(new NotifyMediaController(this));
            mediaController.updateSongName(fhs.getAdapter().getItem(itemposition));
            updateVariableUIs(mediaController);
            startForeground(NotifyMediaController.NOTIFY_ID,
                    ((NotifyMediaController) mediaController).getNotification());
        } else {
            stopSelf();
        }
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(App.SCEARU_TAG, "REBINDED");
        destroyNotification();
    }

    private void destroyNotification() {
        if (receiver != null) {
            stopForeground(true);
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private void prepare(final int position) {
        if (prepareMusicTask != null && prepareMusicTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(App.SCEARU_TAG, "Current preparing!");
            return;
        }

        prepareMusicTask = new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                return ((GoogleDrive) fhs).getToken(null);
            }

            @Override
            protected void onPostExecute(String s) {
                if (s == null || s.isEmpty()) {
                    Log.d(App.SCEARU_TAG, "Oops! Error! MediaService:1");
                    return; // TODO: Handle token missing error
                }
                String id = fhs.getAdapter().getUniqueItem(position);
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
     * @return Next song in position
     */
    private int nextSong() {
        int pos;
        if (shuffle) {
            do {
                pos = random.nextInt(fhs.getAdapter().getCount());
            } while (pos == itemposition);
        } else {
            pos = itemposition;
            int max = fhs.getAdapter().getCount();
            if (++pos >= max) pos = 0;
        }
        return pos;
    }

    private void playSong(int position) {
        if (mp.isPlaying()) mp.stop();
        prepare(position);
        itemposition = position;
    }

    public void storePreferences() {
        SharedPreferences.Editor editor =
                App.getAppContext().getSharedPreferences(
                        FileHostingService.FHS_ACCOUNT_PREFS,
                        Context.MODE_PRIVATE
                ).edit();
        StringBuilder sb = new StringBuilder();
        sb.append((shuffle) ? 1 : 0);
        sb.append(":");
        switch (repeat) {
            case NOREPEAT: sb.append(0); break;
            case REPEATONE: sb.append(1); break;
            case REPEAT:
            default: sb.append(2); break;
        }
        editor.putString(sb.toString(), PREFACCOUNTNAME);
        editor.apply();
    }

    public void setPreferences() {
        SharedPreferences prefs = App.getAppContext().getSharedPreferences(
                FileHostingService.FHS_ACCOUNT_PREFS,
                Context.MODE_PRIVATE);
        String[] values = prefs.getString(PREFACCOUNTNAME, "1:2").split(":");
        if (values.length < 2) {
            values = new String[]{"1", "2"};
        }

        if (values[0].equals("1")) {
            shuffle = true;
        } else {
            shuffle = false;
        }

        if (values[1].equals("0")) {
            repeat = RepeatState.NOREPEAT;
        } else if (values[1].equals("1")) {
            repeat = RepeatState.REPEAT;
        } else if (values[1].equals("2")) {
            repeat = RepeatState.REPEATONE;
        }
    }

    /************************************
     * Bind helper Classes / Methods    *
     ************************************/

    public class LocalBinder extends Binder {
        protected MediaService getService() {
            return MediaService.this;
        }
    }

    public void setMediaController(BaseMediaController mediaController) {
        if (this.mediaController != null) this.mediaController.destroy();
        this.mediaController = mediaController;
    }

    public boolean isPlaying() {
        if (mp == null) return false;
        return mp.isPlaying();
    }

    public int getCurrentPosition() {
        return mp.getCurrentPosition();
    }

    public int getDuration() {
        return mp.getDuration();
    }

    public int getBufferPercentage() {
        return bufferPercentage;
    }

    public void setBufferPercentage(int bufferPercentage) {
        this.bufferPercentage = bufferPercentage;
    }

    public void seekTo(int pos) {
        mp.seekTo(pos);
    }

    public void updateVariableUIs(BaseMediaController mediaController) {
        boolean isPlaying = (mp != null) ? mp.isPlaying() : false;
        mediaController.updatePlayIcon(isPlaying);
        mediaController.updateShuffleIcon(shuffle);
        mediaController.updateRepeatIcon(repeat);
    }

    public void updateShuffle(BaseMediaController mediaController) {
        mediaController.updateShuffleIcon(shuffle = !shuffle);
    }

    public void updateRepeat(BaseMediaController mediaController) {
        switch (repeat) {
            case NOREPEAT:
                repeat = RepeatState.REPEAT;
                break;
            case REPEAT:
                repeat =  RepeatState.REPEATONE;
                break;
            case REPEATONE:
                repeat = RepeatState.NOREPEAT;
                break;
        }
        mediaController.updateRepeatIcon(repeat);
    }

    public void updatePrevious(BaseMediaController mediaController) {
        mediaController.updatePreviousIcon();
        int pos;
        if (shuffle || fhs.getAdapter().getCount() == 1) { // repeat on shuffle
            mp.seekTo(0);
            if (!mp.isPlaying()) {
                mp.start();
            }
            mediaController.prepare(); // simulate prepare
        } else { // previous song in list on non-shuffle
            if (itemposition == 0) {
                pos = fhs.getAdapter().getCount() - 1;
            } else {
                pos = itemposition - 1;
            }
            if (whatToRepeat != null) whatToRepeat = null;
            playSong(pos);
        }
    }

    public void updatePlay(BaseMediaController mediaController) {
        boolean playing = mp.isPlaying();
        if (playing) {
            mp.pause();
        } else {
            mp.start();
        }
        mediaController.updatePlayIcon(!playing);
    }

    public void updateNext(BaseMediaController mediaController) {
        mediaController.updateNextIcon();
        if (whatToRepeat != null) whatToRepeat = null;
        playSong(nextSong());
    }

    public void selectionPlay(BaseMediaController mediaController, int pos) {
        if (pos < 0) return;
        mediaController.updateNextIcon(); // selectionPlay is similar to calling next()
        if (whatToRepeat != null) whatToRepeat = null;
        playSong(pos);
    }

    /************************************
     * BroadCast Receiver               *
     ************************************/

    protected class BroadCastListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(App.SCEARU_TAG, "Hello Broadcast: " + action);
            if (action.equals(SHUFFLE_ACTION)) { // shuffle
                updateShuffle(mediaController);
            } else if (action.equals(REPEAT_ACTION)) { // repeat
                updateRepeat(mediaController);
            } else if (action.equals(PREVIOUS_ACTION)) { // previous
                updatePrevious(mediaController);
            } else if (action.equals(PLAY_ACTION)) { // start or pause
                updatePlay(mediaController);
            } else if (action.equals(NEXT_ACTION)) { // next
                updateNext(mediaController);
            } else if (action.equals(SELECTION_ACTION)) { // stop service
                int pos = intent.getIntExtra(SELECTION_ACTION, -1);
                selectionPlay(mediaController, pos);
            } else if (action.equals(DESTROY_ACTION)) {
                stopSelf();
            }
        }
    }
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