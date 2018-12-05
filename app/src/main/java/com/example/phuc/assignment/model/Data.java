package com.example.phuc.assignment.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Data {
    private float temperature;
    private float humidity;
    private Timestamp uploadTime;

    public Data(float temperature, float humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
        Date date = new Date();
        this.uploadTime = new Timestamp(date.getTime());
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("temperature", temperature);
        result.put("humidity", humidity);
        result.put("uploadTime", uploadTime);
        return result;

    }
}
