package com.comtietea.comtietea;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.RingtonePreference;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.comtietea.comtietea.Domain.FirebaseReferences;

/**
 * Created by HP on 12/08/2017.
 */
public class AlarmClass extends BroadcastReceiver {
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

        Intent i = createAlarm(context, type, uid, codSimId, calObjId, actSchId, fecha, camSemId, color, palHabId);

        context.startActivity(i);

    }

    private Intent createAlarm(final Context context, String type, String uid,
                               String codSimId, String calObjId, String actSchId, String fecha, String camSemId, String color, String palHabId) {
        Intent i = new Intent(context, CreateActivityScheduleActivity.class);
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
        i.putExtra("action", "alarma");
        i.putExtra("calObjId", calObjId);

        return i;
    }
}
