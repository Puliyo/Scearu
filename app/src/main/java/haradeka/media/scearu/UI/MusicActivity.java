package haradeka.media.scearu.UI;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.GlobalMethods;
import haradeka.media.scearu.UTILS.ScearuActivity;

public class MusicActivity extends ScearuActivity {
    private ListView mediaFiles;
    private Handler killTimer;
    private Runnable killRunner;
    private MediaController mediaController;
    private TextView musicMarquee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.layout_activity_toolbar);
        setSupportActionBar(myToolbar);


        killTimer = new Handler();
        killRunner = new Runnable() {
            @Override
            public void run() {
                Log.d(GlobalMethods.SCEARU_LOG, "Killing MusicAct");
                finish();
            }
        };

        mediaController = new MediaController(this, true);
        mediaController.setAnchorView(findViewById(R.id.music_linear_space));

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
                    mediaController.show(0);
                }
            }
        });

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
        killTimer.removeCallbacksAndMessages(killRunner);
    }

    @Override
    protected void onStop() {
        super.onStop();
        killTimer.removeCallbacksAndMessages(killRunner);
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
        mService.getMediaPlayer().setOnPreparedListener(preparedListener);
    }

    private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(final MediaPlayer mp) {
            mediaController.setMediaPlayer(mService);
            marqueeStart();
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (!mediaController.isEnabled()) mediaController.setEnabled(true);
                    mediaController.show();
                    mp.start();
                }
            });
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
