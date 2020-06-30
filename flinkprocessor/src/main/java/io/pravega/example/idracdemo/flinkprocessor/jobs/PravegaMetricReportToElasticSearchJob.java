package io.pravega.example.idracdemo.flinkprocessor.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.serialization.JsonDeserializationSchema;
import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
import io.pravega.example.idracdemo.flinkprocessor.MetricReportFlattener;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.MetricReport;
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
 *  Read the stream as MetricReport objects encoded as JSON.
 *  The events are then displayed to stderr.
 */
public class PravegaMetricReportToElasticSearchJob extends AbstractJob {
    private static Logger log = LoggerFactory.getLogger(PravegaMetricReportToElasticSearchJob.class);

    public PravegaMetricReportToElasticSearchJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            StreamExecutionEnvironment env = initializeFlinkStreaming();
            setupElasticSearch();
            createStream(appConfiguration.getInputStreamConfig());

            FlinkPravegaReader<FlatMetricReport> flinkPravegaReader = FlinkPravegaReader.builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getInputStreamConfig().getStream())
                    .withDeserializationSchema(new JsonDeserializationSchema(FlatMetricReport.class))
                    .build();

            DataStream<FlatMetricReport> events = env
                    .addSource(flinkPravegaReader)
                    .name("events");
            events.printToErr();

          //  DataStream<FlatMetricReport> flatEvents = events.flatMap(new MetricReportFlattener());
          //  flatEvents.printToErr();

            ElasticsearchSink<FlatMetricReport> elasticSink = newElasticSearchSink();
            events.addSink(elasticSink).name("Write to ElasticSearch");

            env.execute(PravegaMetricReportToElasticSearchJob.class.getSimpleName());
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

    public static class ResultSinkFunction implements ElasticsearchSinkFunction<FlatMetricReport> {
        private final String index;
        private final String type;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public ResultSinkFunction(String index, String type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public void process(FlatMetricReport event, RuntimeContext ctx, RequestIndexer indexer) {
            indexer.add(createIndexRequest(event));
        }

        private IndexRequest createIndexRequest(FlatMetricReport event) {
            try {
                byte[] json = objectMapper.writeValueAsBytes(event);
                return Requests.indexRequest()
                        .index(index)
                        .type(type)
                        .source(json, XContentType.JSON);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
