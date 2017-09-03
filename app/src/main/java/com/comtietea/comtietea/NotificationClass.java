package com.comtietea.comtietea;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

public class NotificationClass extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        String type = bundle.getString("type");
        String uid = bundle.getString("uid");
        String codSimId = bundle.getString("codSimId");
        String calObjId = bundle.getString("calObjId");
        String actSchId = bundle.getString("actSchId");
        String fecha = bundle.getString("fecha");
        String camSemId = bundle.getString("camSemId");
        String color = bundle.getString("color");
        String palHabId = bundle.getString("palHabId");
        String hora = bundle.getString("hora");

        String url = bundle.getString("url");
        String nombre = bundle.getString("nombre");
        int id = Integer.parseInt(bundle.getString("id"));

        createNotification(context, nombre, url, id , type, uid, codSimId, calObjId, actSchId, fecha, camSemId, color, palHabId, hora);
    }

    private void createNotification(final Context context, final String nombre, String url, final int id, String type, String uid,
                                    String codSimId, String calObjId, String actSchId, String fecha, String camSemId, String color, String palHabId, String hora) {
        Intent i = new Intent(context, CommonWordDetailActivity.class);
        i.putExtra("type", type);
        i.putExtra("uid", uid);
        i.putExtra("codSimId", codSimId);
        i.putExtra("camSemId", camSemId);
        i.putExtra("color", color);
        i.putExtra("nombreCampoSemantico", "");
        i.putExtra("palHabId", palHabId);
        i.putExtra("anterior", "agenda");
        i.putExtra("calObjId", calObjId);
        i.putExtra("actSchId", actSchId);
        i.putExtra("fecha", fecha);
        i.putExtra("alarma", "");
        i.putExtra("hora", hora);
        final PendingIntent notificIntent = PendingIntent.getActivity(context, id, i, 0);

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
