package io.pravega.idracsolution.flinkprocessor.util;

import io.pravega.idracsolution.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.idracsolution.flinkprocessor.datatypes.MetricReport;
import org.apache.flink.api.common.functions.FlatMapFunction;
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
                rpt.RackLabel = in.RackLabel;
                rpt.Oem = metricValue.Oem;
                rpt.Label = metricValue.Oem.Dell.Label;
                rpt.ContextID = metricValue.Oem.Dell.ContextID;
                rpt.Timestamp = in.Timestamp;
                rpt.MetricId = metricValue.MetricId;
                try {
                    rpt.MetricValue = Double.parseDouble(metricValue.MetricValue);
                }
                catch (NullPointerException e) {
                    continue;
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
