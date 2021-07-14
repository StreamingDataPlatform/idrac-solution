package io.pravega.idracsolution.flinkprocessor.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricReport implements Serializable {
    public String Id;
    public MetricValue[] MetricValues;
    public String Name;
    public String RemoteAddr;
    public String RackLabel;
    public String Timestamp;
    @JsonProperty("@odata.type")
    public String dataType;
    @JsonProperty("@odata.context")
    public String dataContext;
    @JsonProperty("@odata.id")
    public String dataId;
    public String ReportSequence;
    @JsonProperty("MetricValues@odata.count")
    public long metricCount;
    public MetricReportDefinition MetricReportDefinition;

    @Override
    public String toString() {
        return "MetricReport{" +
                "Id='" + Id + '\'' +
                ", MetricValues=" + Arrays.toString(MetricValues) +
                ", Name='" + Name + '\'' +
                ", RemoteAddr='" + RemoteAddr + '\'' +
                ", RemoteAddr='" + RackLabel + '\'' +
                ", Timestamp='" + Timestamp + '\'' +
                '}';
    }

    public static class MetricReportDefinition implements Serializable {
        @JsonProperty("@odata.id")
        public String dataId;
    }

    public static class MetricValue implements Serializable {
        public String MetricId;
        public String MetricValue;
        public String Timestamp;
        public Oem Oem;

        public Tuple2<String, String> getMetricId() {
            return new Tuple2<>(this.MetricId, this.Oem.Dell.ContextID);
        }

        @Override
        public String toString() {
            return "MetricValue{" +
                    "MetricId='" + MetricId + '\'' +
                    ", MetricValue='" + MetricValue + '\'' +
                    ", Timestamp='" + Timestamp + '\'' +
                    ", oem=" + Oem +
                    '}';
        }
    }
}