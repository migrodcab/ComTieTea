package com.comtietea.comtietea;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
