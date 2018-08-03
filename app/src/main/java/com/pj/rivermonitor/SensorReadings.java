package com.pj.rivermonitor;

import android.provider.BaseColumns;

/**
 * Created by Patrik on 23/08/2016.
 */
public final class SensorReadings {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public SensorReadings() {}

    /* Inner class that defines the table contents */
    public static abstract class SensorEntry implements BaseColumns {
        public static final String TABLE_NAME = "SensorEntry";
        public static final String ENTRY_ID = "entryid";
        public static final String SENSOR_NAME = "sensor";
        public static final String SENSOR_DATE = "sensor_date";
        public static final String SENSOR_DISTANCE = "sensor_distance";
        public static final String SENSOR_HUMIDITY = "sensor_humidity";
        public static final String SENSOR_TEMPERATURE = "sensor_temperature";
        public static final String TABLE_NAME_SENSOR_ALARM = "Sensor_Alarm";
        public static final String SENSOR_ALARM_ID = "_id";
        public static final String SENSOR_ALARM_NAME = "sensor";
        public static final String SENSOR_ALARM_ALERT_VALUE = "sensor_alert_value";
    }


}