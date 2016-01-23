package haradeka.media.scearu.FHS;

/**
 * Created by Puliyo on 23/01/2016.
 */


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import haradeka.media.scearu.R;

/**
 * Custom adapter used to list media detail in listview.
 * This adapter does not rely on context. This adapter should be capable of surviving without context.
 * This adapter contains <i>hashMap</i> where list items detail are stored:
 * <ol>
 *     <li><i>HASH_KEY_TITLE</i>: Used for item title.</li>
 *     <li><i>HASH_KEY_SUB</i>: (Optional) Used for item subtitle.</li>
 *     <li><i>HASH_KEY_UNIQUE</i>: Unique identifier for the item. Used for FHS to find the item.</li>
 * </ol>
 * Override <i>update</i> method to define how you want the <i>hashMap</i> to be updated.
 */
public abstract class FHSAdapter extends BaseAdapter {
    public static final String HASH_KEY_TITLE = "fhsadapter_title";
    public static final String HASH_KEY_SUB = "fhsadapter_sub";
    public static final String HASH_KEY_UNIQUE = "fhsadapter_unique";

    private WeakReference<Context> weakContext;
    private HashMap<String, String[]> hashMap;
    private OnEditListener onEditListener;
    private boolean attached = false;

    public FHSAdapter() {
        this.hashMap = new HashMap<String, String[]>();
    }

    /**
     * Attach to this Adapter
     * @param context Context used when displaying list item
     * @param onEditListener Listener used when item tapped
     */
    public void attach(Context context, OnEditListener onEditListener) {
        if (weakContext != null) weakContext.clear();
        weakContext = null;
        weakContext = new WeakReference<Context>(context);
        this.onEditListener = onEditListener;
        attached = true;
    }

    /**
     * Detach to this Adapter.
     * It is always important to detach if attached yourself.
     */
    public void detach() {
        onEditListener = null;
        weakContext.clear();
        attached = false;
    }

    public boolean isAttached() {
        return attached;
    }

    /**
     * Update the HashMap here.
     */
    public abstract void update();

    /**
     * Cleans up hashmap
     */
    public void clear() {
        hashMap.clear();
    }

    @Override
    public int getCount() {
        String[] values = hashMap.get(HASH_KEY_TITLE);
        return (values == null) ? 0 : values.length;
    }

    @Override
    public String getItem(int position) {
        return getItem(HASH_KEY_TITLE, position);
    }

    /**
     * names and unique must have same length
     * @param names title of the items
     * @param subs (optional: parse null) sub title for the items
     * @param unique unique identifier of the items
     */
    public void setItem(String[] names, String[] subs, String[] unique) {
        hashMap.put(HASH_KEY_TITLE, names);
        hashMap.put(HASH_KEY_SUB, subs);
        hashMap.put(HASH_KEY_UNIQUE, unique);
    }

    public String getUniqueItem(int position) {
        return getItem(HASH_KEY_UNIQUE, position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public String getItem(String key, int position) {
        String[] values = hashMap.get(key);
        return (values == null) ? null : values[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        Context context = weakContext.get();

        if (context == null) return v;

        if (v == null) {
            v = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.list_media_item, null);
        }

        final String t_t = hashMap.get(HASH_KEY_TITLE)[position];
        final String t_s = hashMap.get(HASH_KEY_SUB)[position];
        final String t_u = hashMap.get(HASH_KEY_UNIQUE)[position];

        TextView title = (TextView) v.findViewById(R.id.media_item_title);
        if (title != null && t_t != null)
            title.setText(t_t);

        TextView sub = (TextView) v.findViewById(R.id.media_item_sub);
        if (sub != null && t_s != null)
            sub.setText(t_s);

        if (onEditListener != null) {
            ImageButton edit = (ImageButton) v.findViewById(R.id.media_item_edit);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEditListener.onEditClick(t_t, t_u);
                }
            });
        }

        return v;
    }

    public interface OnEditListener {
        void onEditClick(String title, String unique);
    }
}
