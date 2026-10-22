package com.northmendo.Appzuku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.ContextCompat;

import static com.northmendo.Appzuku.PreferenceKeys.KEY_AUTO_KILL_ENABLED;
import static com.northmendo.Appzuku.PreferenceKeys.PREFERENCES_NAME;

/**
 * Broadcast receiver for Boot Completed.
 * Restores automation state after reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean automationEnabled = prefs.getBoolean(KEY_AUTO_KILL_ENABLED, false);

            if (automationEnabled) {
                // El worker es lo que sostiene la automatizacion: esta permitido programarlo
                // desde el arranque. Levantar el servicio en primer plano desde una conexion
                // de BroadcastReceiver esta restringido desde Android 14 y revienta el arranque.
                AutoKillWorker.schedule(context);
                try {
                    Intent serviceIntent = new Intent(context, ShappkyService.class);
                    ContextCompat.startForegroundService(context, serviceIntent);
                    Log.d(TAG, "Boot restore complete: automation resumed");
                } catch (Exception e) {
                    Log.w(TAG, "Foreground service not allowed at boot, worker still scheduled", e);
                }
            } else {
                AutoKillWorker.cancel(context);
                Log.d(TAG, "Boot restore skipped: automation disabled");
            }
        }
    }
}
