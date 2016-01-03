package haradeka.media.scearu.UTILS;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import haradeka.media.scearu.R;

public class AboutActivity extends ListActivity {
    final private String ITEM = "ITEM";
    final private String SUBITEM = "SUBITEM";
    private List<Map<String, String>> values;
    private String[] item_values;
    private String[] subitem_values;
    private boolean dialogshown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        dialogshown = false;

        values = new ArrayList<Map<String, String>>();
        item_values = new String[] {
                "Legal Notices", "Google Legal Notices"
        };
        subitem_values = new String[] {
                "License legal notices", "Google License and Terms of Use\n*Intensive Mem and CPU usage"
        };

        for (int i = 0; i < item_values.length; i++) {
            Map<String, String> map = new HashMap<String, String>(2);
            map.put(ITEM, item_values[i]);
            map.put(SUBITEM, subitem_values[i]);
            values.add(map);
        }

        ListAdapter adapter = new SimpleAdapter(
                this, values, R.layout.list_about_item,
                new String[] {ITEM, SUBITEM},
                new int[] {R.id.about_text_item, R.id.about_text_subitem}
        );

        setListAdapter(adapter);

        getListView().setOnItemClickListener(adapterClickListener);
    }

    @Override
    protected void onStop() {
        finish();
        super.onStop();
    }

    private AdapterView.OnItemClickListener adapterClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!dialogshown) {
                showLicenseDialog(position);
                dialogshown = true;
            }
        }
    };

    private void showLicenseDialog(final int position) {
        new AsyncTask<Void, Void, AlertDialog.Builder>() {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(AboutActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Loading legal notices..");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected AlertDialog.Builder doInBackground(Void... params) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
                String title = item_values[position];
                String message;

                switch (position) {
                    case 0:
                        message = getBaseContext().getResources().getString(R.string.legalNotices);
                        break;
                    case 1:
                        message = GoogleApiAvailability.getInstance()
                                .getOpenSourceSoftwareLicenseInfo(AboutActivity.this);
                        if (message == null) {
                            message = "Failed to get google license!\n";
                        }
                        break;
                    default:
                        message = "Oops! Nothing to show!\n";
                }

                builder.setTitle(title).setMessage(message).setCancelable(true);
                return builder;
            }

            @Override
            protected void onPostExecute(AlertDialog.Builder builder) {
                AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                });
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialogshown = false;
                    }
                });
                alertDialog.show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }
}
