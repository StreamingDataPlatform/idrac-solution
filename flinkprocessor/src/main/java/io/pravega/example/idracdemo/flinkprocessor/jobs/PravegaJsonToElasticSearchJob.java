package io.pravega.example.idracdemo.flinkprocessor.jobs;

import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.serialization.UTF8StringDeserializationSchema;
import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a general-purpose class that reads UTF-8 JSON events from Pravega and writes them to ElasticSearch.
 * The JSON is passed without any conversion.
 */
public class PravegaJsonToElasticSearchJob extends AbstractJob {
    private static Logger log = LoggerFactory.getLogger(PravegaJsonToElasticSearchJob.class);

    public PravegaJsonToElasticSearchJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            StreamExecutionEnvironment env = initializeFlinkStreaming();
            setupElasticSearch();
            createStream(appConfiguration.getInputStreamConfig());

            FlinkPravegaReader<String> flinkPravegaReader = FlinkPravegaReader.<String>builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getInputStreamConfig().getStream())
                    .withDeserializationSchema(new UTF8StringDeserializationSchema())
                    .build();

            DataStream<String> events = env
                    .setParallelism(appConfiguration.getReaderParallelism())
                    .addSource(flinkPravegaReader)
                    .name("events");
            events.printToErr();

            ElasticsearchSink<String> elasticSink = newElasticSearchSink();
            events.addSink(elasticSink).name("Write to ElasticSearch").setParallelism(appConfiguration.getWriterParallelism());

            env.execute(PravegaJsonToElasticSearchJob.class.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ElasticsearchSinkFunction getResultSinkFunction() {
        String index = appConfiguration.getElasticSearch().getIndex();
        String type = appConfiguration.getElasticSearch().getType();

        return new ResultSinkFunction(index, type);
    }

    public static class ResultSinkFunction implements ElasticsearchSinkFunction<String> {
        private final String index;
        private final String type;

        public ResultSinkFunction(String index, String type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public void process(String event, RuntimeContext ctx, RequestIndexer indexer) {
            log.error(event);
            indexer.add(createIndexRequest(event));
        }

        private IndexRequest createIndexRequest(String event) {
            try {
                return Requests.indexRequest()
                        .index(index)
                        .type(type)
                        .source(event, XContentType.JSON);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
