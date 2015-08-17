package org.c99.calories;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class CaloriesSyncReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, CaloriesSyncService.class);
        i.putExtras(intent);
        startWakefulService(context, i);
    }
}
