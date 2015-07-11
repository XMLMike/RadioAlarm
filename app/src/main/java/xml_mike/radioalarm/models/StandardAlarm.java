package xml_mike.radioalarm.models;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import xml_mike.radioalarm.Global;
import xml_mike.radioalarm.managers.AlarmsManager;
import xml_mike.radioalarm.managers.ThreadedMediaPlayer;
import xml_mike.radioalarm.views.AlarmMediaAdapter;

/**
 * Created by MClifford on 23/02/15.
 */
public class StandardAlarm extends AlarmAbstract {

    public static final Parcelable.Creator<StandardAlarm> CREATOR
            = new Parcelable.Creator<StandardAlarm>() {
        public StandardAlarm createFromParcel(Parcel in) {
            return new StandardAlarm(in);
        }

        public StandardAlarm[] newArray(int size) {
            return new StandardAlarm[size];
        }
    };

    public StandardAlarm() {super();}

    public StandardAlarm(Parcel in){
        super(in);
    }

    /**
     *  Had Memory leak from unclosed cursor resolved, TODO note: on some devices do not have internal ring tones defined, as such will return an empty result
     * @param context
     * @param groupPosition
     * @return
     */
    @Override
    public View.OnClickListener getDataOnClickListener(final Context context, final int groupPosition) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(context,"StandardAlarm", Toast.LENGTH_SHORT).show();

                final RingtoneManager ringtoneManager = new RingtoneManager(Global.getInstance().getApplicationContext());
                ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
                Cursor ringtones = ringtoneManager.getCursor();

                int i = 0;
                AlarmMedia[] alarmMedias = new AlarmMedia[ringtones.getCount()] ;

                for (ringtones.moveToFirst(); !ringtones.isAfterLast(); ringtones.moveToNext()) {


                    String name = ringtones.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                    String data = "";//ringtoneManager.getRingtoneUri(0).toString();//ringtones.getString(RingtoneManager.URI_COLUMN_INDEX);
                    String id = ringtones.getString(RingtoneManager.ID_COLUMN_INDEX);

                    Log.e("data", data);

                    alarmMedias[i] = new AlarmMedia(id, "", name, data, name, "0");

                    i++;
                }

                final AlarmMediaAdapter adapter = new AlarmMediaAdapter(context, android.R.layout.simple_list_item_1,alarmMedias);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if(ringtones.getCount() > 0) {
                    builder
                            .setTitle("Select Alarm")
                            .setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Toast.makeText(context, ringtoneManager.getRingtoneUri(which).toString(), Toast.LENGTH_SHORT).show();
                                    //StandardAlarm.this.setData(adapter.getItem(groupPosition).getData());
                                    StandardAlarm.this.setData(ringtoneManager.getRingtoneUri(which).toString());

                                    AlarmsManager.getInstance().update(groupPosition, StandardAlarm.this, false);
                                    AlarmsManager.getInstance().notifyObservers();
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    AlarmsManager.getInstance().notifyObservers();
                                }
                            })
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    AlarmsManager.getInstance().notifyObservers();
                                }
                            });
                    Log.e("","");
                } else {
                    Toast.makeText(context, "No local Alarm Sounds found", Toast.LENGTH_SHORT).show();
                }

                builder.create().show();
                Log.e("local", "Total:" + ringtones.getCount());
                //ringtones.close();

            }
        };
    }

    @Override
    public void setupAlarmData(Context context, ThreadedMediaPlayer mediaPlayer) throws IOException {
        mediaPlayer.changeDataSource(context, getData());
    }
}
