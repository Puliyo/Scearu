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
public class MyMediaController {
    private MyMediaPlayerControl mPlayer;
    private Handler seekhandler;
    private StringBuilder timebuilder;
    private Formatter timeformatter;

    private LinearLayout mainController;
    private ImageButton shuffle;
    private ImageButton next;
    private ImageButton previous;
    private ImageButton startstop;
    private ImageButton repeat;
    private SeekBar seekbar;
    private TextView currenttime;
    private TextView endtime;

    private boolean showing;
    private boolean shuffleOn;
    private RepeatState repeatState;
    private boolean mDragging;
    private boolean isPrepared;
    private int fetchbuffer;
    private int mbuffer;
    private int fetchduration;
    private int mduration;
    private boolean runningHandler;
    private final static int UPDATE_FREQUENCY = 1000; // millis
    public static enum RepeatState {
        REPEAT, REPEATONE, NOREPEAT
    }

    public MyMediaController(Activity activity, int parent_res) {
        this(activity, parent_res, false);
    }

    public MyMediaController(Activity activity, int parent_res, boolean show) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup parent = (ViewGroup) activity.findViewById(parent_res);

        inflater.inflate(R.layout.mediacontroller, parent);

        timebuilder = new StringBuilder();
        timeformatter = new Formatter(timebuilder, Locale.getDefault());

        mainController = (LinearLayout) activity.findViewById(R.id.mc_main_controller);
        startstop = (ImageButton) activity.findViewById(R.id.mc_startstop);
        next = (ImageButton) activity.findViewById(R.id.mc_next);
        previous = (ImageButton) activity.findViewById(R.id.mc_previous);
        shuffle = (ImageButton) activity.findViewById(R.id.mc_shuffle);
        shuffleOn = true;
        repeat = (ImageButton) activity.findViewById(R.id.mc_repeat);
        repeatState = RepeatState.REPEAT;
        seekbar = (SeekBar) activity.findViewById(R.id.mc_seekbar);

        startstop.setOnClickListener(clickListener);
        next.setOnClickListener(clickListener);
        previous.setOnClickListener(clickListener);
        shuffle.setOnClickListener(clickListener);
        repeat.setOnClickListener(clickListener);
        seekbar.setMax(1000);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        currenttime = (TextView) activity.findViewById(R.id.mc_currenttime);
        endtime = (TextView) activity.findViewById(R.id.mc_endtime);
        seekhandler = new Handler();
        runningHandler = false;

        if (show) {
            show();
        } else {
            hide();
        }
    }

    public void setMediaPlayer(MyMediaPlayerControl mPlayer) {
        this.mPlayer = mPlayer;
    }

    public boolean isShowing() {
        return showing;
    }

    public boolean isShuffleOn() {
        return shuffleOn;
    }

    public RepeatState getRepeatState() {
        return repeatState;
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
        if (mainController.isEnabled() == enabled) return;
        mainController.setEnabled(enabled);
    }

    /**
     * Call prepare to reset the buffer and duration cache.
     * This class will enable the controller if disabled.
     */
    public void prepare() {
        isPrepared = true;
        if (!seekbar.isEnabled()) {
            seekbar.setEnabled(true);
        }
        fetchbuffer = 0;
        mbuffer = -16;
        fetchduration = 0;
        mduration = -16; // no reason for -16, but -1 may cause problem when getDuration return -1.
    }

    /**
     * Call complete to notify the song have finished.
     */
    public void complete() {
        // sometimes seekbar jumps backwards. complete() are created to prevent this.
        runHandler(false);
        seekbar.setProgress(1000);
        if (currenttime != null) {
            currenttime.setText(stringForTime(mduration));
        }
        isPrepared = false;
    }

    private Runnable seekRunner = new Runnable() {
        @Override
        public void run() {
            if (seek()) {
                seekhandler.postDelayed(seekRunner, 1000);
            }
        }
    };

    private void runHandler(boolean run) {
        if (run) {
            if (runningHandler) return;
            seekRunner.run();
        } else {
            if (!runningHandler) return;
            seekhandler.removeCallbacks(seekRunner);
        }
        runningHandler = !runningHandler;
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
            Log.e(App.SCEARU_TAG, "Illegal State Exception in MediaController");
            runHandler(false);
            return false;
        }
        if (mPlayer == null || seekbar == null | mDragging) return true;
        int position = mPlayer.getCurrentPosition();
        if (position < 0) {
            runHandler(false);
            seekbar.setEnabled(false);
            return false;
        }

        final int BUFFERCOUNT = 5;
        int duration;
        if (fetchduration < BUFFERCOUNT) { // prevent overhead
            duration = mPlayer.getDuration();
            if (duration < 0) {
                runHandler(false);
                seekbar.setEnabled(false);
                return false;
            }
            if (endtime != null) {
                endtime.setText(stringForTime(duration));
            }
            if (duration == mduration) {
                fetchduration++;
            } else {
                fetchduration = 0;
                mduration = duration;
            }
        } else {
            duration = mduration;
        }

        if (fetchbuffer < BUFFERCOUNT) { // prevent overhead
            int buffer = mPlayer.getBufferPercentage() * 10;
            seekbar.setSecondaryProgress(buffer);
            if (buffer == mbuffer) {
                fetchbuffer++;
            } else {
                fetchbuffer = 0;
                mbuffer = buffer;
            }
        }

        if (duration > 0) {
            long pos = 1000L * position / duration;
            seekbar.setProgress((int) pos);
        }
        if (currenttime != null) {
            currenttime.setText(stringForTime(position));
        }

        return true;
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.mc_startstop:
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                        ((ImageButton) v).setImageResource(R.drawable.ic_pause);
                    } else {
                        mPlayer.start();
                        ((ImageButton) v).setImageResource(R.drawable.ic_play_arrow);
                        prepare();
                    }
                    break;
                case R.id.mc_shuffle:
                    if (shuffleOn) {
                        v.setAlpha(0.3f);
                    } else {
                        v.setAlpha(1f);
                    }
                    shuffleOn = !shuffleOn;
                    break;
                case R.id.mc_next:
                    mPlayer.next();
                    break;
                case R.id.mc_previous:
                    mPlayer.previous();
                    break;
                case R.id.mc_repeat:
                    switch (repeatState) {
                        case NOREPEAT:
                            ((ImageButton) v).setImageResource(R.drawable.ic_repeat);
                            v.setAlpha(1f);
                            repeatState = RepeatState.REPEAT;
                            break;
                        case REPEAT:
                            ((ImageButton) v).setImageResource(R.drawable.ic_repeat_one);
                            repeatState = RepeatState.REPEATONE;
                            break;
                        case REPEATONE:
                            ((ImageButton) v).setImageResource(R.drawable.ic_repeat);
                            v.setAlpha(0.3f);
                            repeatState = RepeatState.NOREPEAT;
                            break;
                    }
                    break;
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) return;
            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newposition);
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

    public interface MyMediaPlayerControl {
        void start();
        void pause();
        boolean isPlaying();
        int getCurrentPosition();
        int getDuration();
        int getBufferPercentage();
        void seekTo(int pos);
        void next();
        void previous();
    }
}