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

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.App;
import haradeka.media.scearu.UTILS.MyMediaController;
import haradeka.media.scearu.UTILS.ScearuActivity;

public class MusicActivity extends ScearuActivity {
    private ListView mediaFiles;
    private Handler killTimer;
    private TextView musicMarquee;
    private MyMediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.layout_activity_toolbar);
        setSupportActionBar(myToolbar);

        killTimer = new Handler();

        mediaFiles = (ListView) findViewById(R.id.music_list_files);
        mediaFiles.setEmptyView(findViewById(R.id.music_tview_empty));

        musicMarquee = (TextView) findViewById(R.id.toolbar_tview);
        musicMarquee.setSelected(true); // required. Otherwise focus it in xml.
        musicMarquee.setOnClickListener(new View.OnClickListener() {
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

        final FileHostingService.FHSAdapter fhsAdapter = fhs.getAdapter(mediaFiles.getContext());

        mediaFiles.setAdapter(fhsAdapter);
        mediaFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mBound == Status.PENDING) {
                    Toast.makeText(getApplicationContext(), "Waiting for media player..", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), fhsAdapter.getItem(position), Toast.LENGTH_SHORT).show();
                marqueePrepare(fhsAdapter.getItem(GoogleDrive.HASH_KEY_NAMES, position));
                mService.prepare(position);
            }
        });

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

    @Override
    public void mOnServiceConnected() {
        // Overwrite onPreparedListener
        MediaPlayer mp = mService.getMediaPlayer();
        mp.setOnPreparedListener(preparedListener);
        mp.setOnCompletionListener(completionListener);
        mp.setOnErrorListener(errorListener);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mediaController.setMediaPlayer(mService);
            }
        });
    }

    private Runnable killRunner = new Runnable() {
        @Override
        public void run() {
            Log.d(App.SCEARU_TAG, "Killing MusicAct");
            finish();
        }
    };

    private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {
            marqueeStart();
            mp.start();
            mediaController.prepare();
            mediaController.show();
        }
    };

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(App.SCEARU_TAG, "SONG COMPLETE");
            mediaController.complete();
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

    private void marqueePrepare(CharSequence text) {
        if (musicMarquee.getVisibility() != View.GONE) musicMarquee.setVisibility(View.GONE);
        musicMarquee.setText(text);
    }

    private void marqueeStart() {
        if (!musicMarquee.isEnabled()) musicMarquee.setEnabled(true);
        if (musicMarquee.getVisibility() != View.VISIBLE) musicMarquee.setVisibility(View.VISIBLE);
    }
}
