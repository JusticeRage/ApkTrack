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

import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import com.commonsware.cwac.wakeful.WakefulIntentService.AlarmListener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AlarmReceiver extends BroadcastReceiver {
  private static final String WAKEFUL_META_DATA="com.commonsware.cwac.wakeful";
  
  @Override
  public void onReceive(Context ctxt, Intent intent) {
    AlarmListener listener=getListener(ctxt);
    
    if (listener!=null) {
      if (intent.getAction()==null) {
        SharedPreferences prefs=ctxt.getSharedPreferences(WakefulIntentService.NAME, 0);

        prefs
          .edit()
          .putLong(WakefulIntentService.LAST_ALARM, System.currentTimeMillis())
          .commit();
        
        listener.sendWakefulWork(ctxt);
      }
      else {
        WakefulIntentService.scheduleAlarms(listener, ctxt, true);
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  private WakefulIntentService.AlarmListener getListener(Context ctxt) {
    PackageManager pm=ctxt.getPackageManager();
    ComponentName cn=new ComponentName(ctxt, getClass());
    
    try {
      ActivityInfo ai=pm.getReceiverInfo(cn,
                                         PackageManager.GET_META_DATA);
      XmlResourceParser xpp=ai.loadXmlMetaData(pm,
                                               WAKEFUL_META_DATA);
      
      while (xpp.getEventType()!=XmlPullParser.END_DOCUMENT) {
        if (xpp.getEventType()==XmlPullParser.START_TAG) {
          if (xpp.getName().equals("WakefulIntentService")) {
            String clsName=xpp.getAttributeValue(null, "listener");
            Class<AlarmListener> cls=(Class<AlarmListener>)Class.forName(clsName);
            
            return(cls.newInstance());
          }
        }
        
        xpp.next();
      }
    }
    catch (NameNotFoundException e) {
      throw new RuntimeException("Cannot find own info???", e);
    }
    catch (XmlPullParserException e) {
      throw new RuntimeException("Malformed metadata resource XML", e);
    }
    catch (IOException e) {
      throw new RuntimeException("Could not read resource XML", e);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException("Listener class not found", e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException("Listener is not public or lacks public constructor", e);
    }
    catch (InstantiationException e) {
      throw new RuntimeException("Could not create instance of listener", e);
    }
    
    return(null);
  }
}