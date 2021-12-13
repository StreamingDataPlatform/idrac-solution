package io.pravega.idracsolution.flinkprocessor;

import io.pravega.client.stream.StreamCut;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.idracsolution.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.idracsolution.flinkprocessor.util.JsonDeserializationSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class IdracMetricsToTimescaleDB extends AbstractJob {

    public IdracMetricsToTimescaleDB(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    private static Logger log = LoggerFactory.getLogger(IdracMetricsToTimescaleDB.class);

    /**
     * The entry point for Flink applications.
     */
    public static void main(String... args) throws Exception {
        AppConfiguration config = new AppConfiguration(args);
        log.info("config: {}", config);
        IdracMetricsToTimescaleDB job = new IdracMetricsToTimescaleDB(config);
        job.run();
    }

    public void run() {
        try {
            final AppConfiguration.StreamConfig inputStreamConfig = getConfig().getStreamConfig("input");
            log.info("input stream: {}", inputStreamConfig);
            createStream(inputStreamConfig);
            createTimescaleDB();
            String tableSchema = "idrac(time TIMESTAMP,RemoteAddr TEXT," +
                    "value NUMERIC, NonNumericValue TEXT, ContextID TEXT, MetricID TEXT," +
                    "RackLabel TEXT)";
            String tableName = "idrac";
            createTimescaleDBTable(tableSchema, tableName);
            final StreamCut startStreamCut = resolveStartStreamCut(inputStreamConfig);
            final StreamCut endStreamCut = resolveEndStreamCut(inputStreamConfig);
            final String fixedRoutingKey = getConfig().getParams().get("fixedRoutingKey", "");
            log.info("fixedRoutingKey: {}", fixedRoutingKey);
            final StreamExecutionEnvironment env = initializeFlinkStreaming();

            final FlinkPravegaReader<FlatMetricReport> flinkPravegaReader = FlinkPravegaReader.<FlatMetricReport>builder()
                    .withPravegaConfig(inputStreamConfig.getPravegaConfig())
                    .forStream(inputStreamConfig.getStream(), startStreamCut, endStreamCut)
                    .withDeserializationSchema(new JsonDeserializationSchema(FlatMetricReport.class))
                    .build();

            DataStream<FlatMetricReport> events = env
                    .addSource(flinkPravegaReader)
                    .name("read-flatten-events");
            events.addSink(
                    JdbcSink.sink(
                            "INSERT INTO idrac(time, RemoteAddr, value, NonNumericValue, " +
                                    "ContextID, MetricId, RackLabel) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            (statement, metricValue) -> {
                                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.ofEpochSecond(metricValue.Timestamp/1000, 0 , ZoneOffset.UTC)));
                                statement.setString(2, metricValue.RemoteAddr);
                                statement.setDouble(3, metricValue.MetricValue);
                                statement.setString(4, metricValue.NonNumericValue);
                                statement.setString(5, metricValue.ContextID);
                                statement.setString(6, metricValue.MetricId);
                                statement.setString(7, metricValue.RackLabel);
                            },
                            JdbcExecutionOptions.builder()
                                    .withBatchSize(getConfig().getTimescaledbBatchSize())
                                    .withBatchIntervalMs(getConfig().getTimescaledbFlushDuration())
                                    .withMaxRetries(5)
                                    .build(),
                            new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                    .withUrl(String.format("jdbc:%s/%s",
                                            getConfig().getTimescaledbUrl(),
                                            getConfig().getTimescaledbDatabase()
                                    ))
                                    .withDriverName("org.postgresql.Driver")
                                    .withUsername(getConfig().getTimescaledbUsername())
                                    .withPassword(getConfig().getTimescaledbPassword())
                                    .build()
                    ));

            env.execute(IdracMetricsToTimescaleDB.class.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
