package haradeka.media.scearu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import haradeka.media.scearu.UI.MusicActivity;
import haradeka.media.scearu.UTILS.ApplicationContext;
import haradeka.media.scearu.UTILS.GlobalMethods;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.layout_activity_toolbar);
        setSupportActionBar(myToolbar);

        Button music_btn = (Button) findViewById(R.id.main_btn_music);

        ApplicationContext.getInstance().init(getApplicationContext());

        if (GlobalMethods.isGooglePlayServicesAvailable(this) && GlobalMethods.isDeviceOnline()) {
            music_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(v.getContext(), MusicActivity.class));
                }
            });
        } else {
            music_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext().getApplicationContext(), "No Internet!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_item, menu);
        return true;
    }
}