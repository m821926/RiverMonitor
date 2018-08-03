package com.pj.rivermonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MySMSMonitor extends BroadcastReceiver
{
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    public final static String INTENT_NEW_READING = "NEW READING";
    public final static String INTENT_ALARM_REACHED = "ALARM REACHED";
    public final static String STOP_ALARM = "STOP ALARM";

    @Override public  void onReceive(Context context, Intent intent)
    {
//        Log.d("MySMSMonitor", "Catch SMS Message");
        if(intent!=null && intent.getAction()!=null &&
                ACTION.compareToIgnoreCase(intent.getAction())==0)
        {
            Object[] pduArray= (Object[]) intent.getExtras().get("pdus");
            SmsMessage[] messages = new SmsMessage[pduArray.length];
            for (int i = 0; i<pduArray.length; i++) {
                messages[i] = SmsMessage.createFromPdu(
                        (byte[])pduArray [i]);
//                Log.d("MySMSMonitor", "From: " +
//                        messages[i].getOriginatingAddress());
//                Log.d("MySMSMonitor", "Msg: " +
//                        messages[i].getMessageBody());

                if(messages[i].getMessageBody().contains("Medicion desde Terminal") )
                {
                    // Yes this is one of our readings, do not broadcast in Android this message
                    abortBroadcast();

                    String terminal = FindString(messages[i].getMessageBody(),"desde Terminal ",". Distancia");
                    String distance = FindString(messages[i].getMessageBody(),"Distancia: ","cm");
                    int intDistance = Integer.parseInt(distance);
                    String temperature = FindString(messages[i].getMessageBody(),"Temperatura: "," grados");
                    int intTemperature = Integer.parseInt(temperature);
                    String humidity = FindString(messages[i].getMessageBody(),"Humedad: ","%");
                    int intHumidity =Integer.parseInt(humidity);

                    SensorEntryDbHelper localDbHelper = new SensorEntryDbHelper(context);
                    SQLiteDatabase db;
                    db = localDbHelper.getWritableDatabase();
                    localDbHelper.insertData(terminal,getCurrDate(),intDistance,intTemperature,intHumidity);

                    // Get Alarm value for Sensor
                    int sensor_alarm = localDbHelper.getSensorAlarm(terminal);

                    // Trigger Intent to inform the application that there is a new reading
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


                    // Open application if the user marked it in the preferences or if the Alarm level has been reached
                    Boolean OpenApplication = prefs.getBoolean("new_message_open_application", true);
                    Boolean HasAlertNotification = prefs.getBoolean("notifications_alert",true);
                    Boolean HasReachedAlarm = false;
                    if(intDistance <= sensor_alarm)
                    {
                        HasReachedAlarm = true;
                    }
                    if(OpenApplication || (HasAlertNotification && HasReachedAlarm )) {
                        // Open application and show value
                        Intent myintent = new Intent(context, SensorDrawingLayout.class);
                        myintent.putExtra(INTENT_NEW_READING, terminal);
                        if (HasReachedAlarm)
                        {
                            myintent.putExtra(INTENT_ALARM_REACHED, intDistance);
                        }
                        myintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //myintent(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(myintent);
                    }

                    // Show notification to the user in Notification panel
                    Boolean ShowNotification = prefs.getBoolean("notifications_new_message", true);
                    Boolean VibrateOnAlert = prefs.getBoolean("notifications_vibrate", true);
                    if(ShowNotification || VibrateOnAlert ) {
                        Notification.Builder myNotification = new Notification.Builder(context);
                        myNotification.setSmallIcon(R.drawable.ic_stat_proyecto_icono_gris2);
                        myNotification.setContentTitle(terminal);


                        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
                        bigTextStyle.setBigContentTitle(terminal);
                        bigTextStyle.bigText("Distance: " + distance + "cm, Temperature: " + temperature + "%C ,Humidity: " + humidity + "%");

                        myNotification.setContentText("Distance: " + distance + "cm, Temperature: " + temperature + "%C ,Humidity: " + humidity + "%");
                        myNotification.setStyle(bigTextStyle);
                        myNotification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.bitmap_rivermonitor));

                        // Point the USer to the application if he clicks on the warning
                        Intent resultIntent = new Intent(context, SensorDrawingLayout.class);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                        stackBuilder.addParentStack(SensorDrawingLayout.class);
                        if(HasReachedAlarm) {
                            resultIntent.putExtra(STOP_ALARM, true);

                        }
                        // Adds the Intent that starts the Activity to the top of the stack
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);

                        myNotification.setContentIntent(resultPendingIntent);
                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                        if(HasReachedAlarm) {
                            myNotification.addAction(R.drawable.stop_alarm, "Stop Alarm", resultPendingIntent);
                        }

                        // notificationID allows you to update the notification later on.
                        int notificationID = 234;
                        mNotificationManager.notify(notificationID, myNotification.build());
                    }


//                    String strRingtonePreference = prefs.getString("notifications_new_message_ringtone", "DEFAULT_SOUND");
//
//                    if(HasAlertNotification) {
//                        if (intDistance <= sensor_alarm) {
//                            // Alarm level has been reached, warn the user !!!
//                            try {
//                               // Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//                                Uri notification = Uri.parse(strRingtonePreference);
//                                Ringtone r = RingtoneManager.getRingtone(context, notification);
//                                r.play();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//
////                            try {
////                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
////                                MediaPlayer mp = new MediaPlayer();
////                                mp.setDataSource(notification.toString());
////                                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
////                                mp.prepare();
////                                mp.start();
////                                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
////                                    public void onSeekComplete(MediaPlayer mp) {
////                                        mp.stop();
////                                        mp.release();
////                                    }
////                                });
////                            } catch (Exception e) {
////                                e.printStackTrace();
////                            }
//                        }
//                    }
                    // Toast myToast2 = Toast.makeText(context, "Database Written", Toast.LENGTH_SHORT);
                   // myToast2.show();

//                    Log.d("MySMSMonitor", "Message captured and stored in DB");
//                    Log.d("MySMSMonitor", "Last Entry ID: " + localDbHelper.getLastEntryID());
                }
//                else {
//                    // This is another sms message so leave it as a normal SMS
//
//                }

            }
        }
    }
    public String getCurrDate()
    {
        //SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //sourceFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        //return sourceFormat.toString();
        //String text = DateFormat.getDateTimeInstance().toString();
        //return DateFormat.getDateTimeInstance().toString();
       // GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Madrid"));
        //DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
        //formatter.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

       // String MyCurrentTime = formatter.format(calendar.getTime());
      //  String result = calendar.toString();
      //  Date date = calendar.getTime();

      //  String result2= date.toString();
      //  return MyCurrentTime;

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        //get current date time with Date()
        Date date = new Date();
        String result = dateFormat.format(date);
        return result;

    }

    private String FindString(String messageBody,String KeywordStart, String KeyWordEnd)
    {
        int startString = messageBody.indexOf(KeywordStart) + KeywordStart.length();
        int endString = messageBody.indexOf(KeyWordEnd,startString);
        if((startString == -1) || (endString==-1))
            return "";
        else
            return messageBody.substring(startString,endString);
    }
}
