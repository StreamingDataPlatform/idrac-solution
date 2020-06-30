package io.pravega.example.idracdemo.flinkprocessor.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlatMetricReport implements Serializable {
    public String Id;
    public String Name;
    public String RemoteAddr;
    public String Timestamp;
    public String MetricId;
    public Double MetricValue;
    public String NonNumericValue;
    public String ContextID;
    public String Label;
    public String RackLabel;
    public Oem Oem;

    @Override
    public String toString() {
        return "FlatMetricReport{" +
                "Id='" + Id + '\'' +
                ", Name='" + Name + '\'' +
                ", RemoteAddr='" + RemoteAddr + '\'' +
                ", Timestamp=" + Timestamp +
                ", MetricId='" + MetricId + '\'' +
                ", MetricValue='" + MetricValue + '\'' +
                ", NonNumericValue='" + NonNumericValue + '\'' +
                ", ContextID='" + ContextID + '\'' +
                ", Label='" + Label + '\'' +
		        ", RackLabel='" + RackLabel + '\'' +
                ", oem=" + Oem +
                '}';
    }
}
