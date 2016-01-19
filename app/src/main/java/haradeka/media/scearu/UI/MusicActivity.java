package haradeka.media.scearu.UI;

import android.content.Intent;
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

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.ActivityMediaController;
import haradeka.media.scearu.UTILS.App;
import haradeka.media.scearu.UTILS.MediaService;
import haradeka.media.scearu.UTILS.ScearuActivity;

public class MusicActivity extends ScearuActivity {
    private Handler killTimer;
    private TextView musicTitle;
    private ActivityMediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.layout_activity_toolbar);
        setSupportActionBar(myToolbar);

        startService(new Intent(this, MediaService.class));
        killTimer = new Handler();

        ListView mediaFiles = (ListView) findViewById(R.id.music_list_files);
        mediaFiles.setEmptyView(findViewById(R.id.music_tview_empty));
        mediaFiles.setOnItemClickListener(itemClickListener);

        FileHostingService.FHSAdapter fhsAdapter = fhs.getAdapter(mediaFiles.getContext());
        mediaFiles.setAdapter(fhsAdapter);

        if (fhsAdapter.getCount() == 0) { // cache
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
    protected void onDestroy() {
        killTimer.removeCallbacks(killRunner);
        if (mediaController != null) {
            mediaController.destroy();
            mediaController = null;
        }
        Log.d(App.SCEARU_TAG, "Bye Music Activity");
        super.onDestroy();
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
                FileHostingService.FHSAdapter fhsAdapter = fhs.getAdapter();
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

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!isBounded()) {
                Toast.makeText(getApplicationContext(), "Waiting for media player..", Toast.LENGTH_SHORT).show();
                return;
            }
            mediaController.show();
            mService.selectionPlay(mediaController, position);
            Log.d(App.SCEARU_TAG, "itemClickListener complete");
        }
    };

    @Override
    public void mOnServiceConnected() {
        musicTitle = (TextView) findViewById(R.id.toolbar_tview);
        musicTitle.setEnabled(true);

        mediaController = new ActivityMediaController(this, R.layout.activity_music);
        mediaController.setActivityControlListener(new ActivityMediaController.ActivityControlListener() {
            @Override
            public void play() {
                mService.updatePlay(mediaController);
            }

            @Override
            public void shuffle() {
                mService.updateShuffle(mediaController);
            }

            @Override
            public void repeat() {
                mService.updateRepeat(mediaController);
            }

            @Override
            public void previous() {
                mService.updatePrevious(mediaController);
            }

            @Override
            public void next() {
                mService.updateNext(mediaController);
            }

            @Override
            public void updateSongName(String songName) {
                musicTitle.setText(songName);
            }

            @Override
            public int getCurrentPosition() {
                return mService.getCurrentPosition();
            }

            @Override
            public int getDuration() {
                return mService.getDuration();
            }

            @Override
            public int getBufferPercentage() {
                return mService.getBufferPercentage();
            }

            @Override
            public void seekTo(int pos) {
                mService.seekTo(pos);
            }
        });
        mService.setMediaController(mediaController);

        mService.updateVariableUIs(mediaController);
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

        if (mService.isPlaying()) {
            mediaController.prepare();
            mediaController.show();
        }
    }
}
