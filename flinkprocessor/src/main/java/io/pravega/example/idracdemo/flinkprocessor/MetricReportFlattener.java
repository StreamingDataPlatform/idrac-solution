package io.pravega.example.idracdemo.flinkprocessor;

import io.pravega.example.idracdemo.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.MetricReport;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.table.api.scala.dateFormat;
import org.apache.flink.util.Collector;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class MetricReportFlattener implements FlatMapFunction<MetricReport, FlatMetricReport> {
    @Override
    public void flatMap(MetricReport in, Collector<FlatMetricReport> out) {
        if (in.MetricValues != null) {
            for (MetricReport.MetricValue metricValue : in.MetricValues) {
                FlatMetricReport rpt = new FlatMetricReport();
                rpt.NonNumericValue = "";
                rpt.Id = in.Id;
                rpt.Name = in.Name;
                rpt.RemoteAddr = in.RemoteAddr;
                rpt.Oem = metricValue.Oem;
                rpt.Label = metricValue.Oem.Dell.Label;
                rpt.ContextID = metricValue.Oem.Dell.ContextID;
                try {
                    SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                    DateFormat desiredFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    desiredFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date result = originalFormat.parse(metricValue.Timestamp);
                    rpt.Timestamp = desiredFormat.format(result);
                } catch (ParseException e) {
                    e.printStackTrace();
                    rpt.Timestamp = metricValue.Timestamp;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    continue;
                }
                rpt.MetricId = metricValue.MetricId;
                try {
                    rpt.MetricValue = Double.parseDouble(metricValue.MetricValue);

                }
                catch (NullPointerException e) {
                    rpt.MetricValue = 0.0;
                    rpt.NonNumericValue = metricValue.MetricValue;
                }
                catch (NumberFormatException e) {
                    rpt.MetricValue = 0.0;
                    rpt.NonNumericValue = metricValue.MetricValue;
                }
                out.collect(rpt);
            }
        }
    }
}
