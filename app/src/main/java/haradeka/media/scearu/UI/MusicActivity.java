package haradeka.media.scearu.UI;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.App;
import haradeka.media.scearu.UTILS.MyMediaController;
import haradeka.media.scearu.UTILS.ScearuActivity;

public class MusicActivity extends ScearuActivity {
    private ListView mediaFiles;
    private Handler killTimer;
    private FileHostingService.FHSAdapter fhsAdapter;
    private TextView musicTitle;
    private MyMediaController mediaController;
    private Random random;
    private List<Integer> whatRepeated;
    private int currposition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.layout_activity_toolbar);
        setSupportActionBar(myToolbar);

        killTimer = new Handler();
        random = new Random();

        mediaFiles = (ListView) findViewById(R.id.music_list_files);
        mediaFiles.setEmptyView(findViewById(R.id.music_tview_empty));
        mediaFiles.setOnItemClickListener(itemClickListener);

        fhsAdapter = fhs.getAdapter(mediaFiles.getContext());
        mediaFiles.setAdapter(fhsAdapter);

        musicTitle = (TextView) findViewById(R.id.toolbar_tview);
        musicTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaController.isShowing()) {
                    mediaController.hide();
                } else {
                    mediaController.show();
                }
            }
        });

        mediaController = new MyMediaController(this, R.layout.activity_music);

        if (fhsAdapter.getCount() == 0) {
            fhs.connect(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        killTimer.removeCallbacks(killRunner);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fhs.interrupt();
        killTimer.postDelayed(killRunner, 5 * 60 * 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.music_toolbar_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.music_toolbar_refresh:
                final FileHostingService.FHSAdapter fhsAdapter = fhs.getAdapter(mediaFiles.getContext());
                if (fhsAdapter != null) {
                    fhsAdapter.clear();
                    fhsAdapter.notifyDataSetChanged();
                    fhs.connect(this);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Runnable killRunner = new Runnable() {
        @Override
        public void run() {
            Log.d(App.SCEARU_TAG, "Killing MusicAct");
            finish();
        }
    };

    private int nextSong() {
        int pos;
        if (mediaController.isShuffleOn()) {
            do {
                pos = random.nextInt(fhsAdapter.getCount());
            } while (pos == currposition);
        } else {
            pos = currposition;
            int max = fhsAdapter.getCount();
            if (++pos >= max) pos = 0;
        }
        return pos;
    }

    private void playSong(int position) {
        mediaController.setEnabled(false);
        musicTitle.setText(fhsAdapter.getItem(GoogleDrive.HASH_KEY_NAMES, position));
        mService.prepare(position);
        currposition = position;
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!isBounded()) {
                Toast.makeText(getApplicationContext(), "Waiting for media player..", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaController.getRepeatState() == MyMediaController.RepeatState.REPEATONE)
                whatRepeated = null;
            playSong(position);
            Log.d(App.SCEARU_TAG, "itemClickListener complete");
        }
    };

    @Override
    public void mOnServiceConnected() {
        // Overwrite onPreparedListener
        final MediaPlayer mp = mService.getMediaPlayer();
        mp.setOnPreparedListener(preparedListener);
        mp.setOnCompletionListener(completionListener);
        mp.setOnErrorListener(errorListener);
        musicTitle.setEnabled(true);
        mediaController.setMediaPlayer(new MyMediaController.MyMediaPlayerControl() {
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
            public void next() {
                if (mediaController.getRepeatState() == MyMediaController.RepeatState.REPEATONE)
                    whatRepeated = null;
                playSong(nextSong());
            }

            @Override
            public void previous() {
                int pos;
                if (mediaController.isShuffleOn()) {
                    mp.seekTo(0);
                    if (!mp.isPlaying()) {
                        mp.start();
                    }
                } else {
                    if (currposition == 0) {
                        pos = fhsAdapter.getCount() - 1;
                    } else {
                        pos = currposition - 1;
                    }
                    if (mediaController.getRepeatState() == MyMediaController.RepeatState.REPEATONE)
                        whatRepeated = null;
                    playSong(pos);
                }
            }

            @Override
            public boolean isPlaying() {
                return mp.isPlaying();
            }

            @Override
            public int getBufferPercentage() {
                return mService.getBufferPercentage();
            }
        });
    }

    private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {
            mediaController.prepare();
            mp.start();
            mediaController.setEnabled(true);
            mediaController.show();
            Log.d(App.SCEARU_TAG, "preparedListener complete");
        }
    };

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(App.SCEARU_TAG, "SONG COMPLETE");
            mediaController.complete();
            switch (mediaController.getRepeatState()) {
                case REPEATONE:
                    if (mediaController.isShuffleOn()) {
                        // if shuffle + repeatOne = Randomly play all songs once.
                        // initialise whatRepeated
                        if (whatRepeated == null) {
                            whatRepeated = new ArrayList<Integer>();
                            int length = fhsAdapter.getCount();
                            for (int i = 0; i < length; i++) {
                                if (i == currposition) continue;
                                whatRepeated.add(i);
                            }
                        }
                        if (whatRepeated.isEmpty()) {
                            whatRepeated = null;
                            break;
                        }
                        int index = random.nextInt(whatRepeated.size());
                        int nextpos = whatRepeated.remove(index);
                        Log.d(App.SCEARU_TAG, "whatRepeated: " + whatRepeated.toString());
                        playSong(nextpos);
                    } else {
                        // if noShuffle + repeatOne = repeat current song.
                        try {
                            mp.prepare();
                            mp.start();
                        } catch (IOException e) {
                            Log.w(App.SCEARU_TAG, "Failed to repeat current song");
                        }
                    }
                    break;
                case REPEAT:
                    playSong(nextSong());
                    break;
                case NOREPEAT:
                    whatRepeated = null;
                    break;
            }
            Log.d(App.SCEARU_TAG, "completionListener complete");
        }
    };

    private MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(App.SCEARU_TAG, "ONERROR: " + what);
            Log.d(App.SCEARU_TAG, "ONERROR2: " + extra);
            return false;
        }
    };
}
