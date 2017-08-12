package com.comtietea.comtietea;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

public class NotificationClass extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        String url = bundle.getString("url");
        String nombre = bundle.getString("nombre");
        int id = Integer.parseInt(bundle.getString("id"));

        createNotification(context, nombre, url, id);
    }

    private void createNotification(final Context context, final String nombre, String url, final int id) {
        final PendingIntent notificIntent = PendingIntent.getActivity(context, id, new Intent(context, MainActivity.class), 0);

        if(url.equals("")) {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(nombre);

            builder.setContentIntent(notificIntent);
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            builder.setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, builder.build());
        } else {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.logo);
            final NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle(builder);
            Glide.with(context).load(url).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    style.bigPicture(resource)
                            .setBigContentTitle(nombre);

                    builder.setContentIntent(notificIntent);
                    builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
                    builder.setAutoCancel(true);

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(id, builder.build());
                }
            });
        }
    }
}
