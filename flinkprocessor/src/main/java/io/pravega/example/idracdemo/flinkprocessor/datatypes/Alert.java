package io.pravega.example.idracdemo.flinkprocessor.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alert implements Serializable {
    public String AlertDescription;
    public String Id;
    public String Name;
    public String RemoteAddr;
    public String Timestamp;
    public String MetricId;
    public Double MetricValue;

    @Override
    public String toString() {
        return "Alert{" +
                "AlertDescription='" + AlertDescription + '\'' +
                ", Id='" + Id + '\'' +
                ", Name='" + Name + '\'' +
                ", RemoteAddr='" + RemoteAddr + '\'' +
                ", Timestamp=" + Timestamp +
                ", MetricId='" + MetricId + '\'' +
                ", MetricValue=" + MetricValue +
                '}';
    }
}
