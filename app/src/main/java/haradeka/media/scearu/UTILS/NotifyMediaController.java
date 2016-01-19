package haradeka.media.scearu.UTILS;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import haradeka.media.scearu.MainActivity;
import haradeka.media.scearu.R;

/**
 * Created by Puliyo on 9/01/2016.
 */
public class NotifyMediaController extends BaseMediaController {
    private RemoteViews remoteViews;
    private Notification notification;
    private NotificationManager notificationManager;
    private boolean mShuffle = true;
    private MediaService.RepeatState mRepeat = MediaService.RepeatState.REPEAT;

    public static final int NOTIFY_ID = 112901414;

    public NotifyMediaController(Context context) {
        /* Notification */
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.notify_mediacontroller);
        remoteViews.setImageViewResource(R.id.notify_icon, R.mipmap.ic_launcher);
        remoteViews.setTextViewText(R.id.notify_title, "Scearu");

        // notify startstop button
        Intent intent = new Intent(MediaService.PLAY_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_play, PendingIntent.getBroadcast(context, 0, intent, 0));
        // notify previous button
        Intent intent1 = new Intent(MediaService.PREVIOUS_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_previous, PendingIntent.getBroadcast(context, 0, intent1, 0));
        // notify next button
        Intent intent2 = new Intent(MediaService.NEXT_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_next, PendingIntent.getBroadcast(context, 0, intent2, 0));
        // notify shuffle button
        Intent intent3 = new Intent(MediaService.SHUFFLE_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_shuffle, PendingIntent.getBroadcast(context, 0, intent3, 0));
        // notify repeat button
        Intent intent4 = new Intent(MediaService.REPEAT_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_repeat, PendingIntent.getBroadcast(context, 0, intent4, 0));
        // notify close button
        Intent intent5 = new Intent(MediaService.DESTROY_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.notify_close, PendingIntent.getBroadcast(context, 0, intent5, 0));

        PendingIntent pIntent = PendingIntent.getActivity(App.getAppContext(), 0,
                new Intent(App.getAppContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(App.getAppContext())
                .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(false).setOngoing(true)
                .setContentIntent(pIntent).setContent(remoteViews)
                .setTicker(context.getResources().getString(R.string.notification_ticker));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
    }

    @Override
    public void prepare() {
        super.prepare();
        setEnabled(true);
        updatePlayIcon(true);
    }

    @Override
    public void complete() {
        super.complete();
        remoteViews.setImageViewResource(R.id.notify_play, R.drawable.ic_nc_stop);
        notificationManager.notify(NOTIFY_ID, notification);
    }

    @Override
    public void updatePlayIcon(boolean isPlayed) {
        if (isPlayed) {
            remoteViews.setImageViewResource(R.id.notify_play, R.drawable.ic_nc_pause);
        } else {
            remoteViews.setImageViewResource(R.id.notify_play, R.drawable.ic_nc_play_arrow);
        }
        notificationManager.notify(NOTIFY_ID, notification);
    }

    @Override
    public void updateSongName(String songName) {
        remoteViews.setTextViewText(R.id.notify_title, songName);
        // assume updatePlayIcon() will be called when calling this method
//        notificationManager.notify(NOTIFY_ID, notification);
    }

    @Override
    public void updateRepeatIcon(MediaService.RepeatState isRepeated) {
        updateShuffleRepeatIcons(mShuffle, mRepeat = isRepeated);
    }

    @Override
    public void updateShuffleIcon(boolean isShuffled) {
        updateShuffleRepeatIcons(mShuffle = isShuffled, mRepeat);
    }

    @Override
    public void updateNextIcon() {
        setEnabled(false);
    }

    @Override
    public void updatePreviousIcon() {
        setEnabled(false);
    }

    public Notification getNotification() {
        return notification;
    }

    private void setEnabled(boolean enabled) {
        //TODO: This is not working
        String buttonMethod = "setEnabled";
        remoteViews.setBoolean(R.id.notify_play, buttonMethod, enabled);
        remoteViews.setBoolean(R.id.notify_next, buttonMethod, enabled);
        remoteViews.setBoolean(R.id.notify_previous, buttonMethod, enabled);
    }

    private void updateShuffleRepeatIcons(boolean isShuffled, MediaService.RepeatState isRepeated) {
        StringBuilder control = new StringBuilder();
        if (isShuffled) {
            control.append("ON ");
        } else {
            control.append("OFF");
        }
        control.append("/");
        switch (isRepeated) {
            case REPEATONE:
                control.append("ONE");
                break;
            case REPEAT:
                control.append(" ON");
                break;
            case NOREPEAT:
                control.append("OFF");
                break;
        }
        remoteViews.setTextViewText(R.id.notify_control, control.toString());
        notificationManager.notify(NOTIFY_ID, notification);
    }
}
