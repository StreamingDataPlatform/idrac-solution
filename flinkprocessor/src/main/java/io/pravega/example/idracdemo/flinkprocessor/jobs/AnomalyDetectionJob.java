package io.pravega.example.idracdemo.flinkprocessor.jobs;

import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaEventRouter;
import io.pravega.connectors.flink.serialization.JsonDeserializationSchema;
import io.pravega.connectors.flink.serialization.JsonSerializationSchema;
import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
import io.pravega.example.idracdemo.flinkprocessor.MetricReportFlattener;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.Alert;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.MetricReport;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnomalyDetectionJob extends AbstractJob {
    private static Logger log = LoggerFactory.getLogger(AnomalyDetectionJob.class);

    public AnomalyDetectionJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            StreamExecutionEnvironment env = initializeFlinkStreaming();
            createStream(appConfiguration.getInputStreamConfig());
            createStream(appConfiguration.getOutputStreamConfig());

            FlinkPravegaReader<FlatMetricReport> flinkPravegaReader = FlinkPravegaReader.builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getInputStreamConfig().getStream())
                    .withDeserializationSchema(new JsonDeserializationSchema(FlatMetricReport.class))
                    .build();

            DataStream<FlatMetricReport> events = env
                    .addSource(flinkPravegaReader)
                    .name("events");

            //DataStream<FlatMetricReport> flatEvents = events.flatMap(new MetricReportFlattener());

            DataStream<Alert> alerts = events.flatMap(new AnomalyDetector());
            alerts.printToErr();

            FlinkPravegaWriter<Alert> writer = FlinkPravegaWriter.<Alert>builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getOutputStreamConfig().getStream())
                    .withSerializationSchema(new JsonSerializationSchema())
                    .withEventRouter(new EventRouter())
                    .build();
            alerts.addSink(writer);

            env.execute(AnomalyDetectionJob.class.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A simple rule-based anomaly detector.
     * It considers a temperature of >100 deg to be an anomaly and outputs an Alert event.
     */
    public static class AnomalyDetector implements FlatMapFunction<FlatMetricReport, Alert> {
        @Override
        public void flatMap(FlatMetricReport in, Collector<Alert> out) {
            if (in.Id.equals("ThermalSensor") && in.MetricId.equals("CurrentReadingSynthetic") && in.MetricValue > 100.0) {
                Alert alert = new Alert();
                alert.AlertDescription = "high temperature";
                alert.Id = in.Id;
                alert.Name = in.Name;
                alert.RemoteAddr = in.RemoteAddr;
                alert.Timestamp = in.Timestamp;
                alert.MetricId = in.MetricId;
                alert.MetricValue = in.MetricValue;
                out.collect(alert);
            }
        }
    }

    public static class EventRouter implements PravegaEventRouter<Alert> {
        @Override
        public String getRoutingKey(Alert event) {
            return event.RemoteAddr;
        }
    }
}
