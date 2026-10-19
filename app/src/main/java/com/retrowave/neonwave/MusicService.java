package com.retrowave.neonwave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;

/** Foreground music playback notification, complements the Web Audio graph. */
public final class MusicService extends Service {
    public static final String PLAY="com.retrowave.neonwave.PLAY", STOP="com.retrowave.neonwave.STOP";
    private static final String CHANNEL="neonwave_music";
    private MediaSession session;
    @Override public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"NEON/WAVE playback",NotificationManager.IMPORTANCE_LOW));session=new MediaSession(this,"NEON/WAVE");session.setActive(true);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||STOP.equals(intent.getAction())){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        String title=intent.getStringExtra("title"),artist=intent.getStringExtra("artist");
        PendingIntent launch=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setContentTitle(title==null?"NEON/WAVE":title).setContentText(artist==null?"Local music":artist).setSmallIcon(com.retrowave.neonwave.R.drawable.app_icon).setContentIntent(launch).setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()));
        try {startForeground(101,b.build());}catch(Exception e){android.util.Log.e("NEONWAVE","Foreground notification not permitted",e);stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){if(session!=null){session.release();session=null;}super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
