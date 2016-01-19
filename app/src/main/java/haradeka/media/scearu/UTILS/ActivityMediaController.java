package haradeka.media.scearu.UTILS;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;

import haradeka.media.scearu.R;

/**
 * Created by Puliyo on 31/12/2015.
 */
public class ActivityMediaController extends BaseMediaController {
    private ActivityControlListener controlListener;
    private Handler seekhandler;
    private StringBuilder timebuilder;
    private Formatter timeformatter;
    private final Object lock = new Object();

    private LinearLayout mainController;
    private ImageButton shuffle;
    private ImageButton next;
    private ImageButton previous;
    private ImageButton play;
    private ImageButton repeat;
    private SeekBar seekbar;
    private TextView currenttime;
    private TextView endtime;

    private boolean showing;
    private boolean mDragging;
    private boolean isPrepared = false;
    private int prevDuration;
    private int prevBuffer;
    private int prevPosition;
    private boolean runningHandler = false;
    private final static int UPDATE_FREQUENCY = 1000; // millis
    private final static int MAX_PROGRESS = 1000; // the value 1000 must not be altered

    /**
     *
     * @param activity Activity containing mediacontroller.xml
     * @param parent_res Parent layout to insert media controller into
     */
    public ActivityMediaController(Activity activity, int parent_res) {
        this(activity, parent_res, false);
    }

    public ActivityMediaController(Activity activity, int parent_res, boolean show) {
        super();

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup parent = (ViewGroup) activity.findViewById(parent_res);

        inflater.inflate(R.layout.mediacontroller, parent);

        timebuilder = new StringBuilder();
        timeformatter = new Formatter(timebuilder, Locale.getDefault());

        mainController = (LinearLayout) activity.findViewById(R.id.mc_main_controller);
        play = (ImageButton) activity.findViewById(R.id.mc_play);
        next = (ImageButton) activity.findViewById(R.id.mc_next);
        previous = (ImageButton) activity.findViewById(R.id.mc_previous);
        shuffle = (ImageButton) activity.findViewById(R.id.mc_shuffle);
        repeat = (ImageButton) activity.findViewById(R.id.mc_repeat);
        seekbar = (SeekBar) activity.findViewById(R.id.mc_seekbar);

        play.setOnClickListener(clickListener);
        next.setOnClickListener(clickListener);
        previous.setOnClickListener(clickListener);
        shuffle.setOnClickListener(clickListener);
        repeat.setOnClickListener(clickListener);
        seekbar.setMax(MAX_PROGRESS);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        currenttime = (TextView) activity.findViewById(R.id.mc_currenttime);
        endtime = (TextView) activity.findViewById(R.id.mc_endtime);
        seekhandler = new Handler();

        if (showing = show) {
            show();
        } else {
            hide();
        }
    }

    @Override
    /*
     * Reset the buffer and duration cache.
     * Enables the controller if disabled.
     */
    public void prepare() {
        super.prepare();
        isPrepared = true;
        setEnabled(true);
        prevDuration = -16;
        prevBuffer = -16;
        prevPosition = -16;
        // no reason for -16, but -1 may cause problem when getDuration return -1.
    }

    @Override
    public void complete() {
        super.complete();
        runHandler(false);
        // sometimes seekbar jumps back few seconds
        seekbar.setProgress(MAX_PROGRESS);
        if (currenttime != null) {
            currenttime.setText(stringForTime(prevDuration));
        }
        play.setImageResource(R.drawable.ic_pause);
        isPrepared = false;
    }

    @Override
    public void destroy() {
        controlListener = null;
    }

    @Override
    public void updatePlayIcon(boolean isPlayed) {
        if (isPlayed) {
            play.setImageResource(R.drawable.ic_pause);
        } else {
            play.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    @Override
    public void updateSongName(String songName) {
        controlListener.updateSongName(songName);
    }

    @Override
    public void updateRepeatIcon(MediaService.RepeatState isRepeated) {
        switch (isRepeated) {
            case NOREPEAT:
                repeat.setImageResource(R.drawable.ic_repeat);
                invertAlpha(repeat, false);
                break;
            case REPEAT:
                repeat.setImageResource(R.drawable.ic_repeat);
                invertAlpha(repeat, true);
                break;
            case REPEATONE:
                repeat.setImageResource(R.drawable.ic_repeat_one);
                invertAlpha(repeat, true);
                break;
        }
    }

    @Override
    public void updateShuffleIcon(boolean isShuffled) {
        invertAlpha(shuffle, isShuffled);
    }

    @Override
    public void updateNextIcon() {
        setEnabled(false);
    }

    @Override
    public void updatePreviousIcon() {
        setEnabled(false);
    }

    public void setActivityControlListener(ActivityControlListener controlListener) {
        this.controlListener = controlListener;
    }

    public boolean isShowing() {
        return showing;
    }

    public void show() {
        if (showing) return;
        if (mainController != null && mainController.getVisibility() != View.VISIBLE)
            mainController.setVisibility(View.VISIBLE);
        runHandler(true);
        showing = true;
    }

    public void hide() {
        if (!showing) return;
        runHandler(false);
        if (mainController != null && mainController.getVisibility() != View.GONE)
            mainController.setVisibility(View.GONE);
        showing = false;
    }

    public void setEnabled(boolean enabled) {
        runHandler(enabled);
        if (play.isEnabled() == enabled) return;
        play.setEnabled(enabled);
        next.setEnabled(enabled);
        previous.setEnabled(enabled);
        seekbar.setEnabled(enabled);
        invertAlpha(play, enabled);
        invertAlpha(next, enabled);
        invertAlpha(previous, enabled);
    }

    private Runnable seekRunner = new Runnable() {
        @Override
        public void run() {
            if (seek()) {
                seekhandler.postDelayed(seekRunner, UPDATE_FREQUENCY);
            }
        }
    };

    private void runHandler(boolean run) {
        if (run == runningHandler) return;
        synchronized (lock) {
            if (run) {
                seekRunner.run();
            } else {
                seekhandler.removeCallbacks(seekRunner);
            }
            runningHandler = !runningHandler;
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;
        timebuilder.setLength(0);
        if (hours > 0) {
            return timeformatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return timeformatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private boolean seek() {
        if (!isPrepared) {
            Log.w(App.SCEARU_TAG, "Illegal State in MediaController");
            runHandler(false);
            return false;
        }
        if (controlListener == null || seekbar == null) return false;
        if (mDragging) return true;

        int position = controlListener.getCurrentPosition();
        if (position < 0) {
            runHandler(false);
            seekbar.setEnabled(false);
            return false;
        }

        if (position != prevPosition) { // prevent overhead
            int duration = controlListener.getDuration();
            if (duration < 0) {
                Log.e(App.SCEARU_TAG, "Failed to get Music Duration");
                runHandler(false);
                seekbar.setEnabled(false);
                return false;
            }
            if (endtime != null) {
                endtime.setText(stringForTime(duration));
            }
            if (duration > 0) {
                long pos = 1000L * position / duration;
                seekbar.setProgress((int) pos);
            }
            prevDuration = duration;
        }

        if (currenttime != null && position != prevPosition) {
            currenttime.setText(stringForTime(position));
            prevPosition = position;
        }

        int buffer = controlListener.getBufferPercentage();
        if (buffer != prevBuffer) { // prevent overhead
            seekbar.setSecondaryProgress(buffer * 10);
            prevBuffer = buffer;
        }

        return true;
    }

    private void invertAlpha(View v, boolean invert) {
        if (invert) {
            v.setAlpha(1f);
        } else {
            v.setAlpha(0.3f);
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.mc_play:
                    controlListener.play();
                    break;
                case R.id.mc_shuffle:
                    controlListener.shuffle();
                    break;
                case R.id.mc_next:
                    controlListener.next();
                    break;
                case R.id.mc_previous:
                    controlListener.previous();
                    break;
                case R.id.mc_repeat:
                    controlListener.repeat();
                    break;
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) return;
            long duration = controlListener.getDuration();
            long newposition = (duration * progress) / 1000L;
            controlListener.seekTo((int) newposition);
            if (currenttime != null) {
                currenttime.setText(stringForTime((int) newposition));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            runHandler(false);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            seekRunner.run();
        }
    };

    public interface ActivityControlListener {
        void play();
        void shuffle();
        void repeat();
        void previous();
        void next();
        void updateSongName(String songName);
        int getCurrentPosition();
        int getDuration();
        int getBufferPercentage();
        void seekTo(int pos);
    }
}