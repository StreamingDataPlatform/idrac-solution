package io.pravega.example.idracdemo.flinkprocessor;

import io.pravega.client.stream.Stream;
import io.pravega.connectors.flink.PravegaConfig;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Flink application parameters used by all job types.
 */
public class AppConfiguration {
    private static Logger log = LoggerFactory.getLogger(AppConfiguration.class);

    private final String jobClass;
    private final PravegaConfig pravegaConfig;
    private final StreamConfig inputStreamConfig;
    private final StreamConfig outputStreamConfig;
    private final ElasticSearch elasticSearch = new ElasticSearch();

    private int parallelism;
    private int writerParallelism;
    private int readerParallelism;
    private long checkpointInterval;
    private boolean enableCheckpoint;
    private boolean enableOperatorChaining;
    private boolean enableRebalance;

    // InfluxDB 
    private String metricsSink;
    private String influxDbUrl;
    private String influxdb_username;
    private String influxdb_password;
    private String influxdb_database;


    public AppConfiguration(String[] args) {
        ParameterTool params = ParameterTool.fromArgs(args);
        log.info("Parameter Tool: {}", params.toMap());

        pravegaConfig = PravegaConfig.fromParams(params).withDefaultScope("examples"); // TODO: make configurable
        inputStreamConfig = new StreamConfig(pravegaConfig, "input-", params);
        outputStreamConfig = new StreamConfig(pravegaConfig, "output-", params);

        jobClass = params.get("jobClass");
        parallelism = params.getInt("parallelism", 3);
        writerParallelism = params.getInt("writer-parallelism", 3);
        readerParallelism = params.getInt("reader-parallelism", 5);

        checkpointInterval = params.getLong("checkpointInterval", 10000);     // milliseconds
        enableCheckpoint = params.getBoolean("enableCheckpoint", true);
        enableOperatorChaining = params.getBoolean("enableOperatorChaining", true);
        enableRebalance = params.getBoolean("rebalance", false);

        // elastic-sink: Whether to sink the results to Elastic Search or not.
        elasticSearch.setSinkResults(params.getBoolean("elastic-sink", true));

        elasticSearch.setDeleteIndex(params.getBoolean("elastic-delete-index", false));

        // elastic-host: Host of the Elastic instance to sink to.
        elasticSearch.setHost(params.get("elastic-host", "elasticsearch-client.default.svc.cluster.local"));

        // elastic-port: Port of the Elastic instance to sink to.
        elasticSearch.setPort(params.getInt("elasticsearch-client", 9200));

        // elastic-cluster: The name of the Elastic cluster to sink to.
        elasticSearch.setCluster(params.get("elastic-cluster", "elastic"));

        // elastic-index: The name of the Elastic index to sink to.
        elasticSearch.setIndex(params.get("elastic-index", "idracdemo-events"));

        // elastic-type: The name of the type to sink.
        elasticSearch.setType(params.get("elastic-type", "event"));

        // InfluxDb
        metricsSink  = params.get("metricsSink", "InfluxDB");
        influxDbUrl  = params.get("influxDB.host", "http://idracdemo-influxdb.default.svc.cluster.local:8086");
        influxdb_username = params.get("influxDB.username", "admin");
        influxdb_password = params.get("influxDB.password", "password");
        influxdb_database = params.get("influxDB.database", "idracdemo");
    }
	
	 public enum MetricsSink {
        ES,
        InfluxDB
    }

    public MetricsSink getMetricsSink() {   return MetricsSink.valueOf(metricsSink);    }

    public String getInfluxdbUrl() { return influxDbUrl; }

    public String getInfluxdbUsername() {
        return influxdb_username;
    }

    public String getInfluxdbPassword() {
        return influxdb_password;
    }

    public String getInfluxdbDatabase() {
        return influxdb_database;
    }


    public String getJobClass() {
        return jobClass;
    }

    public PravegaConfig getPravegaConfig() {
        return pravegaConfig;
    }

    public StreamConfig getInputStreamConfig() {
        return inputStreamConfig;
    }

    public StreamConfig getOutputStreamConfig() {
        return outputStreamConfig;
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getWriterParallelism() {
        return writerParallelism;
    }

    public int getReaderParallelism() {
        return readerParallelism;
    }

    public long getCheckpointInterval() {
        return checkpointInterval;
    }

    public boolean isEnableCheckpoint() {
        return enableCheckpoint;
    }

    public boolean isEnableOperatorChaining() {
        return enableOperatorChaining;
    }

    public boolean isEnableRebalance() {
        return enableRebalance;
    }

   

   

    public static class StreamConfig {
        protected Stream stream;
        protected int targetRate;
        protected int scaleFactor;
        protected int minNumSegments;

        public StreamConfig(PravegaConfig pravegaConfig, String argPrefix, ParameterTool params) {
            stream = pravegaConfig.resolve(params.get(argPrefix + "stream", "default"));
            targetRate = params.getInt(argPrefix + "targetRate", 100000);  // Data rate in KB/sec
            scaleFactor = params.getInt(argPrefix + "scaleFactor", 2);
            minNumSegments = params.getInt(argPrefix + "minNumSegments", 3);
        }

        public Stream getStream() {
            return stream;
        }

        public int getTargetRate() {
            return targetRate;
        }

        public int getScaleFactor() {
            return scaleFactor;
        }

        public int getMinNumSegments() {
            return minNumSegments;
        }
    }

    public static class ElasticSearch implements Serializable {
        private boolean sinkResults;
        private boolean deleteIndex;
        private String host;
        private int port;
        private String cluster;
        private String index;
        private String type;

        public boolean isSinkResults() {
            return sinkResults;
        }

        public void setSinkResults(boolean sinkResults) {
            this.sinkResults = sinkResults;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getCluster() {
            return cluster;
        }

        public void setCluster(String cluster) {
            this.cluster = cluster;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isDeleteIndex() {
            return deleteIndex;
        }

        public void setDeleteIndex(boolean deleteIndex) {
            this.deleteIndex = deleteIndex;
        }
    }

    public ElasticSearch getElasticSearch() {
        return elasticSearch;
    }
}
