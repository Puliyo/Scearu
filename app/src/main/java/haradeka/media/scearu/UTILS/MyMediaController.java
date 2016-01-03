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
    private boolean mDragging;
    private boolean isPrepared;
    private int fetchbuffer;
    private int mbuffer;
    private int fetchduration;
    private int mduration;
    private StringBuilder timebuilder;
    Formatter timeformatter;
    private final static int UPDATE_FREQUENCY = 1000; // millis
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

        shuffle = (ImageButton) activity.findViewById(R.id.mc_shuffle);
        next = (ImageButton) activity.findViewById(R.id.mc_next);
        previous = (ImageButton) activity.findViewById(R.id.mc_previous);
        startstop = (ImageButton) activity.findViewById(R.id.mc_startstop);
        repeat = (ImageButton) activity.findViewById(R.id.mc_startstop);
        seekbar = (SeekBar) activity.findViewById(R.id.mc_seekbar);

        startstop.setOnClickListener(clickListener);

        seekbar.setMax(1000);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        currenttime = (TextView) activity.findViewById(R.id.mc_currenttime);
        endtime = (TextView) activity.findViewById(R.id.mc_endtime);
        seekhandler = new Handler();

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

    public void show() {
        if (showing) return;
        if (mainController != null) mainController.setVisibility(View.VISIBLE);
        seekRunner.run();
        showing = true;
    }

    public void hide() {
        if (!showing) return;
        seekhandler.removeCallbacks(seekRunner);
        if (mainController != null) mainController.setVisibility(View.GONE);
        showing = false;
    }

    /**
     * Call prepare to reset the buffer and duration cache.
     */
    public void prepare() {
        isPrepared = true;
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
        seekhandler.removeCallbacks(seekRunner);
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
            seekhandler.removeCallbacks(seekRunner);
            return false;
        }
        if (mPlayer == null || seekbar == null | mDragging) return true;
        int position = mPlayer.getCurrentPosition();
        if (position < 0) {
            seekhandler.removeCallbacks(seekRunner);
            return false;
        }

        final int BUFFERCOUNT = 5;
        int duration;
        if (fetchduration < BUFFERCOUNT) { // prevent overhead
            duration = mPlayer.getDuration();
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
            seekhandler.removeCallbacks(seekRunner);
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
    }
}