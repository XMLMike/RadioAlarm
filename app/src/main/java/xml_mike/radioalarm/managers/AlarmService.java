package xml_mike.radioalarm.managers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;

import xml_mike.radioalarm.Global;
import xml_mike.radioalarm.GlobalStrings;
import xml_mike.radioalarm.R;
import xml_mike.radioalarm.StaticWakeLock;
import xml_mike.radioalarm.controllers.AlarmActivity;
import xml_mike.radioalarm.models.Alarm;

/**
 * Created by MClifford on 09/04/15.
 * This will handle all the various events in regards to alarm. Service is only on or off, and if it is on it will play the audio
 */
public class AlarmService extends Service implements AudioService {

    //TODO move these to global as static final strings or enums
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    private static final String ACTION_STOP = "com.example.action.STOP";
    private static final String ACTION_PAUSE = "com.example.action.PAUSE";

    private final IBinder mBinder = new AlarmServiceBinder();

    //private int maxVolume = 15; //android default
    //private MediaPlayer mMediaPlayer = null;
    private ThreadedMediaPlayer threadedMediaPlayer;
    private WifiManager.WifiLock wifiLock = null;
    private EasingThread easingQueue;
    private DurationThread durationQueue;
    private Alarm alarm = null ;
    private Vibrator vibrator = null;
    private Notification.Builder notificationBuilder = null;
    private NotificationManager notificationManager = null;

    @Override
    public IBinder onBind(Intent intent) {

        long alarmId = intent.getLongExtra("alarmId", -1L);
        alarm = AlarmsManager.getInstance().getAlarm(alarmId);
        threadedMediaPlayer = new ThreadedMediaPlayer();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        return mBinder;
    }

    @Override
    public void onDestroy() {

        if (threadedMediaPlayer != null)
            threadedMediaPlayer.release();
        if(notificationManager != null)
            notificationManager.cancelAll();

        if (wifiLock != null)
            wifiLock.release();
    }

    private void setupMediaPlayer(Alarm alarm){

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "xml_mike.radioalarm.wifilock");
        wifiLock.acquire();

        try {
            if(!alarm.getData().equals("") || !alarm.getData().isEmpty()) {
                //setup mediaplayer based on alarm type
                alarm.setupAlarmData(Global.getInstance().getApplicationContext(), threadedMediaPlayer);
                Log.d("Audio Found", ""+alarm.getData());
            }
            else {
                threadedMediaPlayer.changeDataSource(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
                Log.d("Audio NOT Found",""+alarm.getId());
            }
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            Log.e(this.getClass().toString(), e.toString());
        }
    }

    /**
     * will change depending on android version
     */
    protected Notification.Builder generateNotification(){

        Intent intent = new Intent(getApplicationContext() , AlarmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // assign the song name to songName
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
               intent , 0);

        Notification.Builder nBuilder = new Notification.Builder(this)
                //.setTicker(alarm.getName())
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setUsesChronometer(true)
                .setContentTitle(alarm.getName()+""+alarm.getData())
                .setContentText("")
                .setContentIntent(pi);

        nBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;

        // Gets an instance of the NotificationManager service

        return nBuilder;
    }

    /**
     *
     */
    protected void updateNotification(String updateString){
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        notificationBuilder.setContentText(updateString);
        mNotifyMgr.notify(alarm.getIntId(), notificationBuilder.build());
    }

    @Override
    public void stopAudio(){

        if(easingQueue != null)
            easingQueue.cancel(true);
        if(durationQueue != null)
            durationQueue.cancel(true);

        if(vibrator !=null)
            vibrator.cancel();

        threadedMediaPlayer.stop();

        this.stopForeground(true);
        this.stopSelf();
        StaticWakeLock.lockOff(this);

    }

    @Override
    public void startAudio(String path, TextView view) {
        vibrator= (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        if(alarm.isVibrate() && vibrator.hasVibrator()) {
            long[] pattern = {0, 300, 1000};
            vibrator.vibrate(pattern, 0);
        }

        AudioManager myAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolumeForAlarm = myAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int maxVolumeToSet = (maxVolumeForAlarm * alarm.getMaxVolume()) / 100; //max volume is anywhere from 1-100; max value is 7;
        myAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolumeToSet, 0);

        Log.d("MaxVolume Tracking", "Vol:" + maxVolumeToSet + ":" + (maxVolumeForAlarm * alarm.getMaxVolume()));
        Log.d("MaxVolume Tracking", "Vol:" + myAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

        notificationBuilder = startNotification(alarm);
        updateNotification(alarm.getTimeHour() + ":" + alarm.getTimeMinute());

        easingQueue = new EasingThread();
        durationQueue=  new DurationThread();

        setupMediaPlayer(alarm);

        //allow multiple threads to run depending on android OS version.
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
            easingQueue.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            durationQueue.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            easingQueue.execute();
            durationQueue.execute();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public Notification.Builder startNotification(Alarm alarm){

        Intent dismissIntent = new Intent(this, AlarmActivity.StopAlarmReceiver.class);
        dismissIntent.setAction(GlobalStrings.STOP_ALARM.toString());
        PendingIntent piDismiss = PendingIntent.getBroadcast(this, 111111, dismissIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent snoozeIntent = new Intent(this, AlarmActivity.StopAlarmReceiver.class);
        snoozeIntent.setAction(GlobalStrings.SET_SNOOZE_ALARM.toString());
        PendingIntent piSnooze = PendingIntent.getBroadcast(this, 111112, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder mNotifyBuilder = new Notification.Builder(this)
                .setContentTitle("R.A.dio")
                .setContentText(alarm.getDataDescription())
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(alarm.getDataDescription()))
                .addAction(android.R.drawable.ic_delete, "Dismiss", piDismiss)
                .addAction(android.R.drawable.ic_input_add, "Snooze", piSnooze)
              ;

        return mNotifyBuilder;
    }

    private class EasingThread extends AsyncTask<String , String, String> {

        @Override
        protected String doInBackground(String... string) {
           synchronized (this) {
               if (threadedMediaPlayer != null && alarm != null) {
                   if (alarm.getEasing() != 0) {
                       int timing = ((1000 * 60 * alarm.getEasing()) / 1000);
                       float easing = 0.00f;
                       while(easing < 1f) {
                           if (!isCancelled()) {
                               easing += 0.001f;//*maxVolume;
                               threadedMediaPlayer.setVolume(easing);
                               android.os.SystemClock.sleep(timing);
                               //Log.i("Service",maxVolume +" increase Volume to " + easing);
                           } else break;
                       }
                   }
                   threadedMediaPlayer.setVolume(1f);
               }
           }
            return "";
        }
    }

    private class DurationThread extends AsyncTask<String , String, String>  {
        @Override
        protected String doInBackground(String... params) {

            synchronized (this) {
                if (threadedMediaPlayer != null && alarm != null) {
                    final int duration = alarm.getDuration() * 60; //time in minutes from milliseconds
                    final int oneSecond = 1000; //One second in milliseconds

                    for (int i = 0; i < duration; i++) {
                        if (!isCancelled()) {
                            //Log.d("Update Notification", ":" + (duration - i)); //what should be shown inside the notification
                            android.os.SystemClock.sleep(oneSecond);
                            //AlarmService.this.updateNotification("TimeLeft:" + (duration - i));
                        } else {
                            break;
                        }
                    }

                    if(notificationManager != null)
                       notificationManager.cancelAll();

                }
            }
            return "";
        }
    }

    public class AlarmServiceBinder extends Binder {
        public AlarmService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AlarmService.this;
        }
    }
}