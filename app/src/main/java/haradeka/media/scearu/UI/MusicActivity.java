package haradeka.media.scearu.UI;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.R;
import haradeka.media.scearu.UTILS.GlobalMethods;
import haradeka.media.scearu.UTILS.ScearuActivity;

public class MusicActivity extends ScearuActivity {
    private ListView mediaFiles;
    private Handler killTimer;
    private Runnable killRunner;

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


        final ImageButton btn_startpause = (ImageButton) findViewById(R.id.music_imgbtn_startpause);
        btn_startpause.setOnClickListener(viewlistener);

        mediaFiles = (ListView) findViewById(R.id.music_list_files);
        mediaFiles.setEmptyView(findViewById(R.id.music_tview_empty));

        final FileHostingService.FHSAdapter fhsAdapter = fhs.getAdapter(mediaFiles.getContext());

        mediaFiles.setAdapter(fhsAdapter);
        mediaFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), fhsAdapter.getItem(position), Toast.LENGTH_SHORT).show();
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
        killTimer.removeCallbacks(killRunner);
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

    private View.OnClickListener viewlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.music_imgbtn_startpause:
                    Log.d(GlobalMethods.SCEARU_LOG, "Processing MP");
//                    if (mp.isPlaying()) {
//                        mp.pause();
//                        ((ImageButton) v).setImageResource(R.drawable.ic_play_arrow);
//                    } else {
//                        mp.start();
//                        ((ImageButton) v).setImageResource(R.drawable.ic_pause);
//                    }
                    break;
                default:
                    Log.i(GlobalMethods.SCEARU_LOG, "Unhandled onclick");
            }
        }
    };
}
