package io.pravega.idracsolution.flinkprocessor;


import io.pravega.client.stream.StreamCut;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaEventRouter;
import io.pravega.idracsolution.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.idracsolution.flinkprocessor.datatypes.MetricReport;
import io.pravega.idracsolution.flinkprocessor.util.JsonDeserializationSchema;
import io.pravega.idracsolution.flinkprocessor.util.JsonSerializationSchema;
import io.pravega.idracsolution.flinkprocessor.util.MetricReportFlattener;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MetricReportToSingleMetricValue extends AbstractJob {
    private static Logger log = LoggerFactory.getLogger(MetricReportToSingleMetricValue.class);

    /**
     * The entry point for Flink applications.
     *
     */
    public static void main(String... args) throws Exception {
        AppConfiguration config = new AppConfiguration(args);
        log.info("config: {}", config);
        MetricReportToSingleMetricValue job = new MetricReportToSingleMetricValue(config);
        job.run();
    }
    public MetricReportToSingleMetricValue(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            final AppConfiguration.StreamConfig inputStreamConfig = getConfig().getStreamConfig("input");
            log.info("input stream: {}", inputStreamConfig);
            createStream(inputStreamConfig);
            final StreamCut startStreamCut = resolveStartStreamCut(inputStreamConfig);
            final StreamCut endStreamCut = resolveEndStreamCut(inputStreamConfig);
            final AppConfiguration.StreamConfig outputStreamConfig = getConfig().getStreamConfig("output");
            log.info("output stream: {}", outputStreamConfig);
            createStream(outputStreamConfig);
            final String fixedRoutingKey = getConfig().getParams().get("fixedRoutingKey", "");
            log.info("fixedRoutingKey: {}", fixedRoutingKey);

            final StreamExecutionEnvironment env = initializeFlinkStreaming();

            final FlinkPravegaReader<MetricReport> flinkPravegaReader = FlinkPravegaReader.builder()
                    .withPravegaConfig(inputStreamConfig.getPravegaConfig())
                    .forStream(inputStreamConfig.getStream(), startStreamCut, endStreamCut)
                    .withDeserializationSchema(new JsonDeserializationSchema(MetricReport.class))
                    .build();

            final DataStream<MetricReport> events = env
                    .addSource(flinkPravegaReader)
                    .name("metric-events")
                    .uid("metric-events");

            final DataStream<FlatMetricReport> metricReportEvents = events
                    .flatMap(new MetricReportFlattener()).name("produce-flatMetricReport")
                    .uid("produce-flatMetricReport");

            final FlinkPravegaWriter<FlatMetricReport> writer = FlinkPravegaWriter.<FlatMetricReport>builder()
                    .withPravegaConfig(outputStreamConfig.getPravegaConfig())
                    .forStream(outputStreamConfig.getStream())
                    .withSerializationSchema(new JsonSerializationSchema())
                    .withEventRouter(new EventRouter())
                    .build();
            metricReportEvents
                    .addSink(writer)
                    .uid("pravega-writer")
                    .name("Pravega writer to " + outputStreamConfig.getStream().getScopedName());


            env.execute(MetricReportToSingleMetricValue.class.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class EventRouter implements PravegaEventRouter<FlatMetricReport> {
        @Override
        public String getRoutingKey(FlatMetricReport event) {
            return event.RemoteAddr;
        }
    }
}
