package haradeka.media.scearu.UTILS;

import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import haradeka.media.scearu.FHS.FileHostingService;
import haradeka.media.scearu.FHS.GoogleDrive;

/**
 * Created by Puliyo on 26/12/2015.
 *
 * Activity class which does basic service binding /unbinding and AccountManagement for you.
 */
public abstract class ScearuActivity extends AppCompatActivity {
    protected FileHostingService fhs;
    protected MediaService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fhs = GoogleDrive.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MediaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        /**
         * The Android system calls this when the connection to the service is unexpectedly lost,
         * such as when the service has crashed or has been killed.
         * This is not called when the client unbinds.
         */
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GoogleDrive.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        fhs.storeAccountName(accountName);
                        Toast.makeText(this.getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                        fhs.connect(this);
                    }
                } else {
                    Log.d("SCEARU_DEBUG", "REQUEST_ACCOUNT_PICKER");
                    Toast.makeText(this.getApplicationContext(), "Error logging in!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case GoogleDrive.REQUEST_AUTHORIZATION:
                Log.d("SCEARU_DEBUG", "REQUEST_AUTHORIZATION");
                if (resultCode != RESULT_OK) {
                    Log.d("SCEARU_DEBUG", "REQUEST_AUTHORIZATION FAILED");
                    Toast.makeText(this.getApplicationContext(), "Error authenticating!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    fhs.connect(this);
                }
                break;
            case GoogleDrive.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    GlobalMethods.isGooglePlayServicesAvailable(this);
                }
                break;
//            case GoogleDrive.MISSING_CLIENT_ID:
//                Toast.makeText(this.getApplicationContext(), "Credential Error!", Toast.LENGTH_LONG).show();
//                finish();
//                break;
        }
    }
}
