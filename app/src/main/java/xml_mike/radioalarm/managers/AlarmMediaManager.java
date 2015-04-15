package xml_mike.radioalarm.managers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xml_mike.radioalarm.AlarmActivity;
import xml_mike.radioalarm.R;
import xml_mike.radioalarm.models.Alarm;
import xml_mike.radioalarm.models.RadioAlarm;

/**
 * Created by MClifford on 09/04/15.
 */
public class AlarmMediaManager extends Service implements MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnErrorListener {
    private static final String ACTION_PLAY = "com.example.action.PLAY";
    private static final String ACTION_STOP = "com.example.action.STOP";
    MediaPlayer mMediaPlayer = null;
    WifiManager.WifiLock wifiLock = null;
    ExecutorService queue;

    public AlarmMediaManager() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(ACTION_PLAY)) {

            queue = Executors.newFixedThreadPool(1);
            long alarmId = intent.getLongExtra("alarmId", -1L);
            Alarm alarm = AlarmsManager.getInstance().getAlarm(alarmId);
            //Log.e("AlarmMediaManager","alarm:"+alarm.getId() alarm);

            if (alarm.getId() >= 0L) {
                if(alarm instanceof RadioAlarm){
                    //getAudioStreamUrl("http://bbc.co.uk/radio/listen/live/r1x.asx", alarm);
                    this.setupMediaplayer("", alarm);
                }
                else {
                    //getAudioStreamUrl("http://bbc.co.uk/radio/listen/live/r1x.asx", alarm);
                    this.setupMediaplayer("", alarm);
                }
            }
        }else if (intent.getAction().equals(ACTION_STOP)){
            this.stopForeground(true);
            this.stopSelf();
        }

        return startId;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initMediaPlayer() {
        // ...initialize the MediaPlayer here...
        if (mMediaPlayer == null)
            this.mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(this);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("alarmMediaManager", "Is there really a problem");
        return false;
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) mMediaPlayer.release();
        if (wifiLock != null) wifiLock.release();

    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private void getAudioStreamUrl(final String urlString, final Alarm alarm) {

        final AlarmMediaManager context = this;
/*
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String returnString = "";
                try {
                    URL url = new URL(urlString);
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    int i = 0;
                    while ((returnString += in.readLine()) != null) {
                        i++;
                    }
                    //return returnString;
                } catch (IOException e) {
                    Log.e("getAudioStreamUrl", e.toString());
                }

                SAXParserFactory factory = SAXParserFactory.newInstance();
                AlarmMediaManager.HandlerContent handler = new HandlerContent();
                try {
                    SAXParser saxParser = factory.newSAXParser();
                    saxParser.parse(new InputSource(new StringReader(returnString)), handler);

                } catch (Throwable e) {
                    Log.e("getAudioStreamUrl", e.toString());
                }

                context.setupMediaplayer(urlString, alarm);
            }
        };

        queue.execute(runnable);
*/
    }


    public static class HandlerContent extends DefaultHandler {

        protected boolean inEntry = true;
        protected boolean inTitle = true;
        protected String title;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("ENTRY")) {
                inEntry = true;
            }
            if (inEntry && localName.equals("TITLE")) {
                inTitle = true;
            }
            Log.e("",""+attributes.toString());

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("ENTRY")) {
                inEntry = false;
            }
            if (inEntry && localName.equals("TITLE")) {
                inTitle = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inEntry && inTitle) {
                title = new String(ch);
            }
        }

        public String getTitle() {
            return title;
        }
    }

    public void setupMediaplayer(String string, Alarm alarm){
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        wifiLock.acquire();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);

        try {
            //mMediaPlayer.setDataSource("http://static.bbci.co.uk/radiolisten/audio/windows_media_over.mp3");
            mMediaPlayer.setDataSource("http://sc8.1.fm:8030/");
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            Log.e("Media:setDataSource", e.toString());
        }
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.prepareAsync(); // prepare async to not block main thread

        // assign the song name to songName
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), AlarmActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification();
        notification.tickerText = alarm.getName();
        notification.icon = R.drawable.abc_textfield_search_material;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(getApplicationContext(), "Alarm",
                "Playing: " + alarm.getData(), pi);
        startForeground(alarm.getIntId(), notification);
    }
}