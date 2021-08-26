package io.pravega.idracsolution.flinkprocessor;

import io.pravega.client.admin.StreamInfo;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.StreamCut;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

/**
 * An abstract job class for Flink Pravega applications.
 */
public abstract class AbstractJob implements Runnable {
    final private static Logger log = LoggerFactory.getLogger(AbstractJob.class);

    private final AppConfiguration config;

    public AbstractJob(AppConfiguration config) {
        this.config = config;
    }

    public AppConfiguration getConfig() {
        return config;
    }

    /**
     * If the Pravega stream does not exist, creates a new stream with the specified stream configuration.
     * If the stream exists, it is unchanged.
     */
    public void createStream(AppConfiguration.StreamConfig streamConfig) {
        try (StreamManager streamManager = StreamManager.create(streamConfig.getPravegaConfig().getClientConfig())) {
            StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                    .scalingPolicy(streamConfig.getScalingPolicy())
                    .build();
            streamManager.createStream(
                    streamConfig.getStream().getScope(),
                    streamConfig.getStream().getStreamName(),
                    streamConfiguration);
        }
    }

    /**
     * Get head and tail stream cuts for a Pravega stream.
     */
    public StreamInfo getStreamInfo(AppConfiguration.StreamConfig streamConfig) {
        try (StreamManager streamManager = StreamManager.create(streamConfig.getPravegaConfig().getClientConfig())) {
            return streamManager.getStreamInfo(streamConfig.getStream().getScope(), streamConfig.getStream().getStreamName());
        }
    }

    /**
     * Convert UNBOUNDED start StreamCut to a concrete StreamCut, pointing to the current head or tail of the stream
     * (depending on isStartAtTail).
     */
    public StreamCut resolveStartStreamCut(AppConfiguration.StreamConfig streamConfig) {
        if (streamConfig.isStartAtTail()) {
            return getStreamInfo(streamConfig).getTailStreamCut();
        } else if (streamConfig.getStartStreamCut() == StreamCut.UNBOUNDED) {
            return getStreamInfo(streamConfig).getHeadStreamCut();
        } else {
            return streamConfig.getStartStreamCut();
        }
    }

    /**
     * For bounded reads (indicated by isEndAtTail), convert UNBOUNDED end StreamCut to a concrete StreamCut,
     * pointing to the current tail of the stream.
     * For unbounded reads, returns UNBOUNDED.
     */
    public StreamCut resolveEndStreamCut(AppConfiguration.StreamConfig streamConfig) {
        if (streamConfig.isEndAtTail()) {
            return getStreamInfo(streamConfig).getTailStreamCut();
        } else {
            return streamConfig.getEndStreamCut();
        }
    }

    public StreamExecutionEnvironment initializeFlinkStreaming() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Make parameters show in Flink UI.
        env.getConfig().setGlobalJobParameters(getConfig().getParams());

        env.setParallelism(getConfig().getParallelism());
        log.info("Parallelism={}, MaxParallelism={}", env.getParallelism(), env.getMaxParallelism());

        if (!getConfig().isEnableOperatorChaining()) {
            env.disableOperatorChaining();
        }
        if (getConfig().isEnableCheckpoint()) {
            env.enableCheckpointing(getConfig().getCheckpointIntervalMs(), CheckpointingMode.EXACTLY_ONCE);
            env.getCheckpointConfig().setMinPauseBetweenCheckpoints(getConfig().getCheckpointIntervalMs() / 2);
            env.getCheckpointConfig().setCheckpointTimeout(getConfig().getCheckpointTimeoutMs());
            // A checkpoint failure will cause the job to fail.
            env.getCheckpointConfig().setTolerableCheckpointFailureNumber(0);
            // If the job is cancelled manually by the user, do not delete the checkpoint.
            // This retained checkpoint can be used manually when restarting the job.
            // In SDP, a retained checkpoint can be used by creating a FlinkSavepoint object.
            env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        }

        // Configure environment for running in a local environment (e.g. in IntelliJ).
        if (env instanceof LocalStreamEnvironment) {
            // We can't use MemoryStateBackend because it can't store large state.
            if (env.getStateBackend() == null || env.getStateBackend() instanceof MemoryStateBackend) {
                log.warn("Using FsStateBackend instead of MemoryStateBackend");
                env.setStateBackend(new FsStateBackend("file:///tmp/flink-state", true));
            }
            // Stop immediately on any errors.
            log.warn("Using noRestart restart strategy");
            env.setRestartStrategy(RestartStrategies.noRestart());
            // Initialize Hadoop file system.
            FileSystem.initialize(getConfig().getParams().getConfiguration());
        }
        return env;
    }



    public ExecutionEnvironment initializeFlinkBatch() {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // Make parameters show in Flink UI.
        env.getConfig().setGlobalJobParameters(getConfig().getParams());

        int parallelism = getConfig().getParallelism();
        if (parallelism > 0) {
            env.setParallelism(parallelism);
        }
        log.info("Parallelism={}", env.getParallelism());

        // Configure environment for running in a local environment (e.g. in IntelliJ).
        if (env instanceof LocalEnvironment) {
            // Initialize Hadoop file system.
            FileSystem.initialize(getConfig().getParams().getConfiguration());
        }
        return env;
    }
}