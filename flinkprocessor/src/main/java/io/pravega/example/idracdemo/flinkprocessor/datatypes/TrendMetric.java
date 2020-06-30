package io.pravega.example.idracdemo.flinkprocessor.datatypes;

public class TrendMetric {
    public String RemoteAddr;
    public String MetricId;
    public Double CurrentValue;
    public Double MinuteTrend;
    public String MinuteDescription;
    public Double HourTrend;
    public String HourDescription;
    public Double DayTrend;
    public String DayDescription;
    public String ContextId;
}
