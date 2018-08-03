package com.pj.rivermonitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import static android.R.attr.x;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.support.v4.app.ActivityCompat.startActivity;
import static com.pj.rivermonitor.R.id.spinTerminal;

public class SensorDrawingLayout extends AppCompatActivity implements View.OnClickListener, Spinner.OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {

    private String DEBUG_TAG = "MySMSMonitor";
    private GestureDetectorCompat mDetector;

    public static SensorEntryDbHelper mDbHelper;

    public static SQLiteDatabase db;
    public static String NO_DATA = "No Data";
    public final static String EXTRA_MESSAGE = "com.pj.rivermonitor.MESSAGE";
    public final static String INTENT_NEW_READING = "NEW READING";
    public final static String INTENT_ALARM_REACHED = "ALARM REACHED";
    public final static String STOP_ALARM = "STOP ALARM";
    public static  String SensorReported;
    public static int Alarm_Reported_Distance;
    public static Boolean HasReachedAlarmLevel;
    // public static Ringtone ringtone;
    public static MediaPlayer mediaplayer;
    public static Boolean Extra_stop_alarm;
    private GestureDetector mGestureDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_drawing_layout);



        // Get the selected Terminal from previous screen
        Bundle extras = getIntent().getExtras();
        SensorReported = "";
        if(getIntent().hasExtra(INTENT_NEW_READING)) {
            SensorReported = extras.getString(INTENT_NEW_READING);
        }

        Extra_stop_alarm = false;
        if(getIntent().hasExtra(STOP_ALARM)){
            Extra_stop_alarm = getIntent().getBooleanExtra(STOP_ALARM,false);
        }

        if(Extra_stop_alarm) {
            Stop_Alarm();
        }

        Alarm_Reported_Distance=240;
        HasReachedAlarmLevel= false;
        if(getIntent().hasExtra(INTENT_ALARM_REACHED)) {
            Alarm_Reported_Distance = extras.getInt(INTENT_NEW_READING);
            HasReachedAlarmLevel = true;

        }


        final ImageButton btnUpdate = (ImageButton) findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(this);
        final ImageButton btnShowSensorPreviousValue = (ImageButton) findViewById(R.id.btnShowSensorPreviousValue);
        btnShowSensorPreviousValue.setOnClickListener(this);
        final ImageButton btnShowSensorNextValue = (ImageButton) findViewById(R.id.btnShowSensorNextValue);
        btnShowSensorNextValue.setOnClickListener(this);
        final ImageButton btnLast10Readings = (ImageButton) findViewById(R.id.btnLast10Readings);
        btnLast10Readings.setOnClickListener(this);
        final ImageButton btnStopAlarm = (ImageButton) findViewById(R.id.btnStopAlarm);
        btnStopAlarm.setOnClickListener(this);
        final SeekBar AlarmValue = (SeekBar) findViewById(R.id.AlarmValue);
        AlarmValue.setOnSeekBarChangeListener(this);

        Log.d("MySMSMonitor", "River Started with Graphical View.");

        // Start Database Helper
        mDbHelper = new SensorEntryDbHelper(this);
        db = mDbHelper.getWritableDatabase();
        Log.d("MySMSMonitor", "Database activated.");




        // Play Warning Sound if alarm level has been reached
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = prefs.getString("notifications_new_message_ringtone", "DEFAULT_SOUND");
        Boolean ShowAlert = prefs.getBoolean("notifications_alert", true);
        Boolean VibrateOnAlert = prefs.getBoolean("notifications_vibrate", true);
        if (HasReachedAlarmLevel) {
            if(ShowAlert) {
                btnStopAlarm.setVisibility(View.VISIBLE);

                try {
                    // Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    Uri notification = Uri.parse(strRingtonePreference);
//                ringtone = RingtoneManager.getRingtone(this, notification);
//                ringtone..setLooping(true);
//                ringtone.play();
                    mediaplayer = MediaPlayer.create(this, notification);
                    mediaplayer.setLooping(true);
                    mediaplayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Vibrate the mobile phone if the user marked it in the preferences
            if(VibrateOnAlert) {
                Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 10000 milliseconds
                v.vibrate(10000);
            }
        }

        // Check if the application has the necessary permissions else ask for them to the user
        Log.d("MySMSMonitor", "Check permissions");
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("MySMSMonitor", "Permission is granted");


            } else {


                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS}, 1);
                Log.d("MySMSMonitor", "Permission is revoked");
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.d("MySMSMonitor", "Permission is already granted");
        }
        // Create an object of our Custom Gesture Detector Class
        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(this, customGestureDetector);
        // Attach listeners that'll be called for double-tap and related gestures
        mGestureDetector.setOnDoubleTapListener(customGestureDetector);


    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Get Screen Objects
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        spinTerminal.setOnItemSelectedListener(this);

        final TextView tbID = (TextView) findViewById(R.id.tbID);
        final TextView tbDate = (TextView) findViewById(R.id.tbDate);
        final TextView tbDistance = (TextView) findViewById(R.id.tbDistance);
        final TextView tbTemperature = (TextView) findViewById(R.id.tbTemperature);
        final TextView tbHumidity = (TextView) findViewById(R.id.tbHumidity);




        // Update reading if exists
        if(mDbHelper.getLastEntryID()==0) {
            // There is no reading so set fields to no reading

            // Upate spinner Terminal list with no data
            ArrayList sensorFilterList = new ArrayList();
            sensorFilterList.add(NO_DATA);
            ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
            spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinTerminal.setAdapter(spinSensorFilterAdapter);


            tbID.setText("0");
            //spinTerminal.setSelected();
            tbDate.setText("N/A");
            tbDistance.setText("N/A");
            tbHumidity.setText("N/A");
            tbTemperature.setText("N/A");


        }
        else {  // Populate information with last reading
            updateSensorFilter();
            if(SensorReported.length()> 0)
                selectValue(spinTerminal,SensorReported);
            updateData();
            UpdateGraph();
        }


    }

    private void UpdateGraph() {
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        final TextView AlarmValueText = (TextView) findViewById(R.id.tbAlarmValueText);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> Alertseries = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> Maxseries = new LineGraphSeries<DataPoint>();
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
//                new DataPoint(0, 1),
//                new DataPoint(1, 5),
//                new DataPoint(2, 3),
//                new DataPoint(3, 2),
//                new DataPoint(4, 6)
//        });

        int AmountofReadings=48;
        Cursor cursorEntry = mDbHelper.getLastxReadings(spinTerminal.getSelectedItem().toString(),AmountofReadings);
        int x= 0;

        AmountofReadings =  cursorEntry.getCount();     // Get the exact of readings
        cursorEntry.moveToLast();
        while(cursorEntry.moveToPrevious()) {
            series.appendData(new DataPoint(x,-cursorEntry.getInt(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE))),true, AmountofReadings);
            Alertseries.appendData(new DataPoint(x,-Integer.parseInt(AlarmValueText.getText().toString())),true,AmountofReadings);
           x++;
        }

        Viewport viewport = graph.getViewport();
        //viewport.setMinY(0);
        viewport.setMaxX(AmountofReadings);
        viewport.setXAxisBoundsManual(true);
        //viewport.setYAxisBoundsManual(true);
        graph.addSeries(series);
        Alertseries.setColor(Color.RED);
        graph.addSeries(Alertseries);
        //graph.getGridLabelRenderer().setVerticalAxisTitle("cm");
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalAxisTitleTextSize(40);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

//        Double yMin = Math.floor(0);
//        Double yMax = Math.ceil(240);
//
//        int yLabelsCnt = 1 + (int)(Math.ceil(240) - yMin.intValue());
//        int yLabelInterval = (int)(Math.ceil((double)yLabelsCnt / 5));
//        yLabelsCnt = (yLabelInterval > 1)
//                ? (int)Math.ceil(yLabelsCnt / yLabelInterval)
//                : yLabelsCnt;
//
//        // Y labels
//        String[] yArray = new String[ yLabelsCnt ];
//        int j=yLabelsCnt-1;
//        for(int i = yMin.intValue(); i < (yMin.intValue() + (yLabelInterval * yLabelsCnt)); i+=yLabelInterval){
//            yArray[j--]=Integer.toString(i);
//        }
//
//        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
//        staticLabelsFormatter.setVerticalLabels(yArray);
//        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
    }

    private void selectValue(Spinner spinner, Object value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    private void updateSensorFilter() {
        // This will retrieve all sensors and show it in the spinner
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        String strSelectedValue = NO_DATA;
        if(spinTerminal.getCount() > 0) {
            strSelectedValue = spinTerminal.getSelectedItem().toString();
        }

        ArrayList sensorFilterList = new ArrayList();
        Cursor cursorEntry = mDbHelper.getSensors();
        while(cursorEntry.moveToNext()) {
            sensorFilterList.add(cursorEntry.getString(0));
        }
        ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
        spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinTerminal.setAdapter(spinSensorFilterAdapter);
        if(strSelectedValue != NO_DATA)
        {
            selectValue(spinTerminal,strSelectedValue );
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);//Menu Resource, Menu
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_configuration:
//                Toast.makeText(getApplicationContext(),"Configuration options 1",Toast.LENGTH_LONG).show();
                Intent intentSettings = new Intent(this,SettingsActivity.class);
                startActivity(intentSettings);
                return true;
            case R.id.menu_about:
                Toast.makeText(getApplicationContext(),"River Monitor created by Patrik Jacobs and Javi Cañizares",Toast.LENGTH_LONG).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void updateChange()
    {
        TextView tbDifference = (TextView) findViewById(R.id.tbDifference);
        ImageView ImageChange = (ImageView) findViewById(R.id.ImageChange);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

        int AmountofReadings=2;
        Cursor cursorEntry = mDbHelper.getLastxReadings(spinTerminal.getSelectedItem().toString(),AmountofReadings);
        int x= 0;

        AmountofReadings =  cursorEntry.getCount();     // Get the exact of readings
        cursorEntry.moveToLast();

        int[] WaterLevels = {0,0};

        if(AmountofReadings > 1) { // Check if we have values
            do {
                WaterLevels[x] =cursorEntry.getInt(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
                x++;
            }
            while (cursorEntry.moveToPrevious());
        }

        int intDifference = WaterLevels[0] - WaterLevels[1];
        int AbsoluteDifference = intDifference;
        if(intDifference == 0)
        {
            ImageChange.setImageDrawable(getResources().getDrawable(R.drawable.no_change_32,null));
        }
        else
        {
            if(intDifference>0) {
                // The Water is going up
                ImageChange.setImageDrawable(getResources().getDrawable(R.drawable.going_up_32,null));
            }
            else
            {
                AbsoluteDifference = -AbsoluteDifference;
                // The Water is going down
                ImageChange.setImageDrawable(getResources().getDrawable(R.drawable.going_down_32,null));
            }
        }

        tbDifference.setText(  AbsoluteDifference + " cm");

    }
    private void updateData()
    {
        Log.d("MySMSMonitor", "Updating Data");
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
        final TextView tbID = (TextView) findViewById(R.id.tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
        final TextView tbDate = (TextView) findViewById(R.id.tbDate);
        final TextView tbDistance = (TextView) findViewById(R.id.tbDistance);
        final TextView tbTemperature = (TextView) findViewById(R.id.tbTemperature);
        final TextView tbHumidity = (TextView) findViewById(R.id.tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        final TextView AlarmValueText = (TextView) findViewById(R.id.tbAlarmValueText);
        final SeekBar seekbarAlarmValue = (SeekBar) findViewById(R.id.AlarmValue);
        final TextView AlarmLine = (TextView) findViewById(R.id.AlarmLine);

        String selectedTerminal = spinTerminal.getSelectedItem().toString();
        int LastEntry =  mDbHelper.getLastEntryIDTerminal(selectedTerminal);
        Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
        while(cursorEntry.moveToNext())
        {
            String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
            int intDistance= Integer.parseInt(strDistance);
            tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
            //tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
            tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
            tbDistance.setText(strDistance +" cm");
            tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE))+ " ºC");
            tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY))+" %");
            waterlevel.setProgress(240-intDistance);
        }

        // Get the Alarm Value from the database
        int AlarmValue = mDbHelper.getSensorAlarm(selectedTerminal);
        AlarmValueText.setText(Integer.toString(AlarmValue));
        seekbarAlarmValue.setProgress(AlarmValue);

        // Draw the Alarm Line on the correct position
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) AlarmLine.getLayoutParams();
        ImageView MonitorImage = (ImageView) findViewById(R.id.MonitorImage) ;
        int MonitorHeight = MonitorImage.getMeasuredHeight();
        int newTopMargin = (int) ((MonitorHeight / 240.0) * AlarmValue);
        mlp.setMargins(0,newTopMargin,0, 0);

        // Set the change icon and difference
        updateChange();
    }
    private void UpdateDataSensorNextValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Next Value");
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
        final TextView tbID = (TextView) findViewById(R.id.tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
        final TextView tbDate = (TextView) findViewById(R.id.tbDate);
        final TextView tbDistance = (TextView) findViewById(R.id.tbDistance);
        final TextView tbTemperature = (TextView) findViewById(R.id.tbTemperature);
        final TextView tbHumidity = (TextView) findViewById(R.id.tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        String strID = String.valueOf(tbID.getText());
        int LastEntry =  mDbHelper.getNextEntryIDTerminalbutHigherThan(spinTerminal.getSelectedItem().toString(), Integer.parseInt(strID));
        if(LastEntry == 0)
        {
            // There are is no previous record
            Snackbar.make(findViewById(R.id.SensorGraphLayout), R.string.no_next_record, Snackbar.LENGTH_SHORT).show();
            //Toast myToast = Toast.makeText(getApplicationContext(),  R.string.no_next_record, Toast.LENGTH_SHORT);
            //myToast.show();
        }
        else {
            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
            while (cursorEntry.moveToNext()) {
                String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
                int intDistance= Integer.parseInt(strDistance);
                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
                tbDistance.setText(strDistance + " cm");
                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
                waterlevel.setProgress(240-intDistance);
            }
        }
    }
    private void UpdateDataSensorPreviousValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Previous Value");
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
        final TextView tbID = (TextView) findViewById(R.id.tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
        final TextView tbDate = (TextView) findViewById(R.id.tbDate);
        final TextView tbDistance = (TextView) findViewById(R.id.tbDistance);
        final TextView tbTemperature = (TextView) findViewById(R.id.tbTemperature);
        final TextView tbHumidity = (TextView) findViewById(R.id.tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        String strID = String.valueOf(tbID.getText());
        int LastEntry =  mDbHelper.getLastEntryIDTerminalbutLowerThan(spinTerminal.getSelectedItem().toString(), Integer.parseInt(strID));
        if(LastEntry == 0)
        {
            // There are is no previous record
            Snackbar.make(findViewById(R.id.SensorGraphLayout), R.string.no_previous_record, Snackbar.LENGTH_SHORT).show();
//            Toast myToast = Toast.makeText(getApplicationContext(), "There is no previous record.", Toast.LENGTH_SHORT);
//            myToast.show();
        }
        else {
            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
            while (cursorEntry.moveToNext()) {
                String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
                int intDistance= Integer.parseInt(strDistance);
                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
                tbDistance.setText(strDistance + " cm");
                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
                waterlevel.setProgress(240-intDistance);
            }
        }
    }
    private void ShowLast10Readings() {
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

        Intent intent = new Intent(this, ShowLast10Readings.class);
        String message = spinTerminal.getSelectedItem().toString();
        intent.putExtra(INTENT_NEW_READING, message);
        startActivity(intent);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId() /*to get clicked view id**/) {
            case R.id.btnUpdate:
              //  updateSensorFilter();
                updateData();
                UpdateGraph();
                break;
             case R.id.btnShowSensorPreviousValue:
           //     updateSensorFilter();
                UpdateDataSensorPreviousValue();
                break;
            case R.id.btnShowSensorNextValue:
                UpdateDataSensorNextValue();
                break;
            case R.id.btnLast10Readings:
                ShowLast10Readings();
                break;
            case R.id.btnStopAlarm:
                Stop_Alarm();
                break;
            default:
                break;
        }
    }

    private void Stop_Alarm() {
//        if(ringtone != null) {
//            if(ringtone.isPlaying())
//                ringtone.stop();
//        }

        if(mediaplayer != null) {
            if(mediaplayer.isPlaying())
                mediaplayer.stop();
        }
        // Hide Alarm Button Again
        final ImageButton btnStopAlarm = (ImageButton) findViewById(R.id.btnStopAlarm);
        btnStopAlarm.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case spinTerminal:
                updateData();
                UpdateGraph();
                break;
            default:
                break;
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    void NewReadingHasbeenReceived()
    {
        updateData();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId())
        {
            case R.id.AlarmValue:
                TextView AlarmValueText = (TextView) findViewById(R.id.tbAlarmValueText);
                String strAlarmValue = Integer.toString(i);
                AlarmValueText.setText(strAlarmValue);

                // Get selected Terminal
                String terminal = "";
                final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
                // Check if spinner terminal has a valid value selected. If not then do nothing.
                // If there is a valid value then update the database and draw new lines on Alarm Level
                // Finally update the graph
                if (spinTerminal != null && spinTerminal.getSelectedItem() != null) {
                    terminal = spinTerminal.getSelectedItem().toString();

                    // Update the new alarm value in the database
                    mDbHelper.insertSensorAlarm(terminal, i);

                    // Set Dashed Red line on drawing for Alarm level
                    final TextView AlarmLine = (TextView) findViewById(R.id.AlarmLine);
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) AlarmLine.getLayoutParams();
                    ImageView MonitorImage = (ImageView) findViewById(R.id.MonitorImage);
                    int MonitorHeight = MonitorImage.getMeasuredHeight();
                    int newTopMargin = (int) ((MonitorHeight / 240.0) * i);
                    mlp.setMargins(0, newTopMargin, 0, 0);

                    // Update the graph as alert value has changed
                    UpdateGraph();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

//    @Override
//    public boolean onDown(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public void onShowPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        return false;
//    }
//
//    @Override
//    public void onLongPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                if (e1.getX() < e2.getX()) {
//            Log.d(DEBUG_TAG, "Left to Right swipe performed");
//        }
//
//        if (e1.getX() > e2.getX()) {
//            Log.d(DEBUG_TAG, "Right to Left swipe performed");
//        }
//
//        if (e1.getY() < e2.getY()) {
//            Log.d(DEBUG_TAG, "Up to Down swipe performed");
//        }
//
//        if (e1.getY() > e2.getY()) {
//            Log.d(DEBUG_TAG, "Down to Up swipe performed");
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean onSingleTapConfirmed(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onDoubleTap(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onDoubleTapEvent(MotionEvent e) {
//        return false;
//    }

//    @Override
//    public boolean onDown(MotionEvent e) {
//        Log.d(DEBUG_TAG, "Down xxxxx");
//        return false;
//    }
//
//    @Override
//    public void onShowPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        return false;
//    }
//
//    @Override
//    public void onLongPress(MotionEvent e) {
//
//    }
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

//        return true;
//    }

//    @Override
//    public void onStartTrackingTouch(SeekBar seekBar) {
//
//    }
//
//    @Override
//    public void onStopTrackingTouch(SeekBar seekBar) {
//
//    }
//
//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        return gestureDetector.onTouchEvent(motionEvent);
//    }
//
//    @Override
//    public boolean onDown(MotionEvent e) {
//        return true;
//    }

    class CustomGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
        private String DEBUG_TAG = "MySMSMonitor";

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(DEBUG_TAG, "onSingleTapConfirmed");
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(DEBUG_TAG, "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {

            Log.d(DEBUG_TAG, "onDoubleTapEvent");
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(DEBUG_TAG, "onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(DEBUG_TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(DEBUG_TAG, "onSingleTapUp");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG, "onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(DEBUG_TAG, "onLongPress");

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling");
                if (e1.getX() < e2.getX()) {
            Log.d(DEBUG_TAG, "Left to Right swipe performed");

        }

        if (e1.getX() > e2.getX()) {
            Log.d(DEBUG_TAG, "Right to Left swipe performed");
            ShowGraphLayout();
        }

        if (e1.getY() < e2.getY()) {
            Log.d(DEBUG_TAG, "Up to Down swipe performed");
        }

        if (e1.getY() > e2.getY()) {
            Log.d(DEBUG_TAG, "Down to Up swipe performed");
        }

            return true;
        }

        private void ShowGraphLayout() {
            final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

            Intent intent = new Intent(SensorDrawingLayout.this, SensorGraphLayout.class);
            String message = spinTerminal.getSelectedItem().toString();
            intent.putExtra(INTENT_NEW_READING, message);
            startActivity(intent);
        }


    }
}



