/*
 * Copyright (c) 2014
 *
 * ApkTrack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ApkTrack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ApkTrack.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.commonsware.cwac.wakeful;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

abstract public class WakefulIntentService extends IntentService {
  abstract protected void doWakefulWork(Intent intent);

  static final String NAME=
      "com.commonsware.cwac.wakeful.WakefulIntentService";
  static final String LAST_ALARM="lastAlarm";
  private static volatile PowerManager.WakeLock lockStatic=null;

  synchronized private static PowerManager.WakeLock getLock(Context context) {
    if (lockStatic == null) {
      PowerManager mgr=
          (PowerManager)context.getSystemService(Context.POWER_SERVICE);

      lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
      lockStatic.setReferenceCounted(true);
    }

    return(lockStatic);
  }

  public static void sendWakefulWork(Context ctxt, Intent i) {
    getLock(ctxt.getApplicationContext()).acquire();
    ctxt.startService(i);
  }

  public static void sendWakefulWork(Context ctxt, Class<?> clsService) {
    sendWakefulWork(ctxt, new Intent(ctxt, clsService));
  }

  public static void scheduleAlarms(AlarmListener listener, Context ctxt) {
    scheduleAlarms(listener, ctxt, true);
  }

  public static void scheduleAlarms(AlarmListener listener,
                                    Context ctxt, boolean force) {
    SharedPreferences prefs=ctxt.getSharedPreferences(NAME, 0);
    long lastAlarm=prefs.getLong(LAST_ALARM, 0);

    if (lastAlarm == 0
        || force
        || (System.currentTimeMillis() > lastAlarm && System.currentTimeMillis()
            - lastAlarm > listener.getMaxAge())) {
      AlarmManager mgr=
          (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
      Intent i=new Intent(ctxt, AlarmReceiver.class);
      PendingIntent pi=PendingIntent.getBroadcast(ctxt, 0, i, 0);

      listener.scheduleAlarms(mgr, pi, ctxt);
    }
  }

  public static void cancelAlarms(Context ctxt) {
    AlarmManager mgr=
        (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
    Intent i=new Intent(ctxt, AlarmReceiver.class);
    PendingIntent pi=PendingIntent.getBroadcast(ctxt, 0, i, 0);

    mgr.cancel(pi);
  }

  public WakefulIntentService(String name) {
    super(name);
    setIntentRedelivery(true);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    PowerManager.WakeLock lock=getLock(this.getApplicationContext());

    if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
      lock.acquire();
    }

    super.onStartCommand(intent, flags, startId);

    return(START_REDELIVER_INTENT);
  }

  @Override
  final protected void onHandleIntent(Intent intent) {
    try {
      doWakefulWork(intent);
    }
    finally {
      PowerManager.WakeLock lock=getLock(this.getApplicationContext());
      
      if (lock.isHeld()) {
        lock.release();
      }
    }
  }

  public interface AlarmListener {
    void scheduleAlarms(AlarmManager mgr, PendingIntent pi, Context ctxt);

    void sendWakefulWork(Context ctxt);

    long getMaxAge();
  }
}
