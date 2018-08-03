package com.pj.rivermonitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import static com.pj.rivermonitor.R.id.AlarmLine;
import static com.pj.rivermonitor.R.id.btnStopAlarm;
import static com.pj.rivermonitor.R.id.tbAlarmValueText;
import static com.pj.rivermonitor.R.id.tbDate;
import static com.pj.rivermonitor.R.id.tbDistance;
import static com.pj.rivermonitor.R.id.tbHumidity;
import static com.pj.rivermonitor.R.id.tbID;
import static com.pj.rivermonitor.R.id.tbTemperature;

public class SensorGraphLayout extends AppCompatActivity implements View.OnClickListener, Spinner.OnItemSelectedListener {

    private String DEBUG_TAG = "MySMSMonitor";
    private GestureDetectorCompat mDetector1;

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
    private GestureDetector mGestureDetector1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_graph_layout);

        // Get the selected Terminal from previous screen
        Bundle extras = getIntent().getExtras();
        SensorReported = "";
        if(getIntent().hasExtra(INTENT_NEW_READING)) {
            SensorReported = extras.getString(INTENT_NEW_READING);
        }


        Log.d("MySMSMonitor", "River Started with Graphical View.");

        // Start Database Helper
        mDbHelper = new SensorEntryDbHelper(this);
        db = mDbHelper.getWritableDatabase();
        Log.d("MySMSMonitor", "Database activated.");



        // Check if the application has the necessary permissions else ask for them to the user

        // Create an object of our Custom Gesture Detector Class
        final CustomGestureDetector1 customGestureDetector = new CustomGestureDetector1();
        // Create a GestureDetector
        mGestureDetector1 = new GestureDetector(this, customGestureDetector);
        // Attach listeners that'll be called for double-tap and related gestures
        mGestureDetector1.setOnDoubleTapListener(customGestureDetector);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector1.onTouchEvent(event);
                return false;
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector1.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get Screen Objects
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        spinTerminal.setOnItemSelectedListener(this);

         // Update reading if exists
        if(mDbHelper.getLastEntryID()==0) {
            // There is no reading so set fields to no reading

            // Upate spinner Terminal list with no data
            ArrayList sensorFilterList = new ArrayList();
            sensorFilterList.add(NO_DATA);
            ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
            spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinTerminal.setAdapter(spinSensorFilterAdapter);

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
        final TextView AlarmValueText = (TextView) findViewById(R.id.tbAlarmValue);
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

        if(cursorEntry.getCount() > 0) { // Check if we have values
            do {
                series.appendData(new DataPoint(x, -cursorEntry.getInt(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE))), true, AmountofReadings);
                Alertseries.appendData(new DataPoint(x, -Integer.parseInt(AlarmValueText.getText().toString())), true, AmountofReadings);
                x++;
            }
            while (cursorEntry.moveToPrevious());
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
    private void updateData()
    {
        Log.d("MySMSMonitor", "Updating Data");
  //      final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
//        final TextView tbID = (TextView) findViewById(tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
//        final TextView tbDate = (TextView) findViewById(tbDate);
//        final TextView tbDistance = (TextView) findViewById(tbDistance);
//        final TextView tbTemperature = (TextView) findViewById(tbTemperature);
//        final TextView tbHumidity = (TextView) findViewById(tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        final TextView tbAlarmValueText = (TextView) findViewById(R.id.tbAlarmValue);
       // final SeekBar seekbarAlarmValue = (SeekBar) findViewById(R.id.AlarmValue);
     //   final TextView AlarmLine = (TextView) findViewById(R.id.AlarmLine);

        String selectedTerminal = spinTerminal.getSelectedItem().toString();
 //       int LastEntry =  mDbHelper.getLastEntryIDTerminal(selectedTerminal);
//        Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
//        while(cursorEntry.moveToNext())
//        {
//            String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
//            int intDistance= Integer.parseInt(strDistance);
////            tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
//            //tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
////            tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
////            tbDistance.setText(strDistance +" cm");
////            tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE))+ " ºC");
////            tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY))+" %");
//            waterlevel.setProgress(240-intDistance);
//        }

        // Get the Alarm Value from the database
        int AlarmValue = mDbHelper.getSensorAlarm(selectedTerminal);
        tbAlarmValueText.setText(Integer.toString(AlarmValue));
   //     seekbarAlarmValue.setProgress(AlarmValue);

        // Draw the Alarm Line on the correct position
//
    }
    private void UpdateDataSensorNextValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Next Value");
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
//        final TextView tbID = (TextView) findViewById(tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
//        final TextView tbDate = (TextView) findViewById(tbDate);
//        final TextView tbDistance = (TextView) findViewById(tbDistance);
//        final TextView tbTemperature = (TextView) findViewById(tbTemperature);
//        final TextView tbHumidity = (TextView) findViewById(tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
//        String strID = String.valueOf(tbID.getText());
//        int LastEntry =  mDbHelper.getNextEntryIDTerminalbutHigherThan(spinTerminal.getSelectedItem().toString(), Integer.parseInt(strID));
//        if(LastEntry == 0)
//        {
//            // There are is no previous record
//            Snackbar.make(findViewById(R.id.SensorGraphLayout), R.string.no_next_record, Snackbar.LENGTH_SHORT).show();
//            //Toast myToast = Toast.makeText(getApplicationContext(),  R.string.no_next_record, Toast.LENGTH_SHORT);
//            //myToast.show();
//        }
//        else {
//            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
//            while (cursorEntry.moveToNext()) {
//                String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
//                int intDistance= Integer.parseInt(strDistance);
////                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
////                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
////                tbDistance.setText(strDistance + " cm");
////                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
////                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
//                waterlevel.setProgress(240-intDistance);
//            }
//        }
    }
    private void UpdateDataSensorPreviousValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Previous Value");
        final ProgressBar waterlevel = (ProgressBar) findViewById(R.id.WaterLevel);
//        final TextView tbID = (TextView) findViewById(tbID);
        //final TextView tbTerminal = (TextView) findViewById(R.id.tbTerminal);
//        final TextView tbDate = (TextView) findViewById(tbDate);
//        final TextView tbDistance = (TextView) findViewById(tbDistance);
//        final TextView tbTemperature = (TextView) findViewById(tbTemperature);
//        final TextView tbHumidity = (TextView) findViewById(tbHumidity);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
//        String strID = String.valueOf(tbID.getText());
//        int LastEntry =  mDbHelper.getLastEntryIDTerminalbutLowerThan(spinTerminal.getSelectedItem().toString(), Integer.parseInt(strID));
//        if(LastEntry == 0)
//        {
//            // There are is no previous record
//            Snackbar.make(findViewById(R.id.SensorGraphLayout), R.string.no_previous_record, Snackbar.LENGTH_SHORT).show();
////            Toast myToast = Toast.makeText(getApplicationContext(), "There is no previous record.", Toast.LENGTH_SHORT);
////            myToast.show();
//        }
//        else {
//            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
//            while (cursorEntry.moveToNext()) {
//                String strDistance = cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE));
//                int intDistance= Integer.parseInt(strDistance);
////                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
////                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
////                tbDistance.setText(strDistance + " cm");
////                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
////                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
//                waterlevel.setProgress(240-intDistance);
//            }
//        }
    }
    private void ShowLast10Readings() {
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

        Intent intent = new Intent(this, ShowLast10Readings.class);
        String message = spinTerminal.getSelectedItem().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
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
            default:
                break;
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.spinTerminal:
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

    class CustomGestureDetector1 implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
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
                    ShowDrawingReadings();

        }

        if (e1.getX() > e2.getX()) {
            Log.d(DEBUG_TAG, "Right to Left swipe performed");
            ShowLast10Readings();
        }

        if (e1.getY() < e2.getY()) {
            Log.d(DEBUG_TAG, "Up to Down swipe performed");
        }

        if (e1.getY() > e2.getY()) {
            Log.d(DEBUG_TAG, "Down to Up swipe performed");
        }

            return true;
        }


        private void ShowDrawingReadings() {
            final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

            Intent intent = new Intent(SensorGraphLayout.this, SensorDrawingLayout.class);
            String message = spinTerminal.getSelectedItem().toString();
            intent.putExtra(INTENT_NEW_READING, message);
            startActivity(intent);
        }

        private void ShowLast10Readings() {
            final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

            Intent intent = new Intent(SensorGraphLayout.this, ShowLast10Readings.class);
            String message = spinTerminal.getSelectedItem().toString();
            intent.putExtra(INTENT_NEW_READING, message);
            startActivity(intent);
        }


    }
}



