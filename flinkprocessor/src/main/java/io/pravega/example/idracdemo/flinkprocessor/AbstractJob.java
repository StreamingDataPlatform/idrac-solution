package io.pravega.example.idracdemo.flinkprocessor;

import io.pravega.client.admin.StreamInfo;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBConfig;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBSink;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractJob implements Runnable {

    private static Logger log = LoggerFactory.getLogger(AbstractJob.class);

    protected final AppConfiguration appConfiguration;

    public AbstractJob(AppConfiguration appConfiguration) {
        this.appConfiguration = appConfiguration;
    }

    public void createStream(AppConfiguration.StreamConfig streamConfig) {
        try (StreamManager streamManager = StreamManager.create(appConfiguration.getPravegaConfig().getClientConfig())) {
            // create the requested scope (if necessary)
//            streamManager.createScope(streamConfig.stream.getScope());
            // create the requested stream
            StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                    .scalingPolicy(ScalingPolicy.byDataRate(streamConfig.targetRate, streamConfig.scaleFactor, streamConfig.minNumSegments))
                    .build();
            streamManager.createStream(streamConfig.stream.getScope(), streamConfig.stream.getStreamName(), streamConfiguration);
        }
    }

    public StreamInfo getStreamInfo(Stream stream) {
        try (StreamManager streamManager = StreamManager.create(appConfiguration.getPravegaConfig().getClientConfig())) {
            return streamManager.getStreamInfo(stream.getScope(), stream.getStreamName());
        }
    }

    public StreamExecutionEnvironment initializeFlinkStreaming() throws Exception {
        // Configure the Flink job environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // Set parallelism, etc.
        int parallelism = appConfiguration.getParallelism();
        if (parallelism > 0) {
            env.setParallelism(parallelism);
        }
        if (!appConfiguration.isEnableOperatorChaining()) {
            env.disableOperatorChaining();
        }
        if (appConfiguration.isEnableCheckpoint()) {
            long checkpointInterval = appConfiguration.getCheckpointInterval();
            env.enableCheckpointing(checkpointInterval, CheckpointingMode.EXACTLY_ONCE);
        }
        log.info("Parallelism={}, MaxParallelism={}", env.getParallelism(), env.getMaxParallelism());
        // We can't use MemoryStateBackend because it can't store our large state.
//        if (env.getStateBackend() == null || env.getStateBackend() instanceof MemoryStateBackend) {
//            log.warn("Using FsStateBackend");
//            env.setStateBackend(new FsStateBackend("file:///tmp/flink-state", true));
//        }
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.getConfig().setAutoWatermarkInterval(200L);

        // Retry every 10 seconds for 24 hrs before failing
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                8640,
                Time.of(10, TimeUnit.SECONDS)
        ));

        return env;
    }

    public ExecutionEnvironment initializeFlinkBatch() throws Exception {
        // Configure the Flink job environment
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // Set parallelism, etc.
        int parallelism = appConfiguration.getParallelism();
        if (parallelism > 0) {
            env.setParallelism(parallelism);
        }
        log.info("Parallelism={}", env.getParallelism());
        return env;
    }

    protected void setupElasticSearch() throws Exception {
        if (appConfiguration.getElasticSearch().isSinkResults()) {
            new ElasticSetup(appConfiguration.getElasticSearch()).run();
        }
    }

    protected ElasticsearchSinkFunction getResultSinkFunction() {
        throw new UnsupportedOperationException();
    }

    protected ElasticsearchSink newElasticSearchSink() throws Exception {
        String host = appConfiguration.getElasticSearch().getHost();
        int port = appConfiguration.getElasticSearch().getPort();

        List<HttpHost> httpHosts = new ArrayList<>();
        httpHosts.add(new HttpHost(host, port, "http"));

        ElasticsearchSink.Builder builder = new ElasticsearchSink.Builder<>(httpHosts, getResultSinkFunction());
        builder.setBulkFlushInterval(5000);
        return builder.build();

    }

    protected void addMetricsSink(DataStream datastream, int parallelism, String uidSuffix) {
        switch (appConfiguration.getMetricsSink()) {
            case ES:
                break;
            case InfluxDB:
                datastream.addSink(createInfluxDBSink())
                        .name("influxdb-sink_" + uidSuffix)
                        .uid("influxdb-sink_" + uidSuffix)
                        .setParallelism(parallelism);
                break;

            default:
                throw new RuntimeException("Metric Sink type not supported");
        }
    }

    protected InfluxDBSink createInfluxDBSink() {
        InfluxDBConfig influxDBConfig = InfluxDBConfig.builder(appConfiguration.getInfluxdbUrl(),
                appConfiguration.getInfluxdbUsername(), appConfiguration.getInfluxdbPassword(), appConfiguration.getInfluxdbDatabase())
                .batchActions(1000)
                .flushDuration(100, TimeUnit.MILLISECONDS)
                .enableGzip(true)
                .build();
        return new InfluxDBSink(influxDBConfig);
    }
}
