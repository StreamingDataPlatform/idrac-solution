package io.pravega.example.idracdemo.flinkprocessor.jobs;

import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
//import org.apache.flink.table.descriptors.Elasticsearch;
//import org.apache.flink.table.descriptors.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Read JSON events from Pravega using the Flink Table API.
 *  The events are then written to ElasticSearch.
 *  TODO: Disabled because this requires Flink 1.7+.
 */
public class PravegaJsonTableToElasticSearchJob extends AbstractJob {
    private static Logger log = LoggerFactory.getLogger(PravegaJsonTableToElasticSearchJob.class);

    public PravegaJsonTableToElasticSearchJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
//        StreamExecutionEnvironment env = initialiseEnv();
//        StreamTableEnvironment tableEnv = TableEnvironment.getTableEnvironment(env);
//        createInputStreamIfRequired();
//
//        TableSchema inputSchema = TableSchema.builder()
//                .field("@odata.type", Types.STRING())
//                .field("Id", Types.STRING())
//                .field("remote_addr", Types.STRING())
//                .build();
//        FlinkPravegaJsonTableSource source = FlinkPravegaJsonTableSource.builder()
//                .forStream(pravegaArgs.inputStream, StreamCut.UNBOUNDED, StreamCut.UNBOUNDED)
//                .withPravegaConfig(appConfiguration.getPravegaConfig())
//                .failOnMissingField(true)
//                .withSchema(inputSchema)
//                .build();
//        tableEnv.registerTableSource("idracdata", source);
//        Table t1 = tableEnv.scan("idracdata");
//        t1.printSchema();
//
//        tableEnv.toAppendStream(t1, Row.class).printToErr();
//
//        // TODO: Below throws exception "Could not find the required schema in property 'schema'."
//        tableEnv.connect(
//                new Elasticsearch()
//                        .version("6")                      // required: valid connector versions are "6"
//                        .host("localhost", 9200, "http")   // required: one or more Elasticsearch hosts to connect to
//                        .index("MyUsers")                  // required: Elasticsearch index
//                        .documentType("user")              // required: Elasticsearch document type
//            )
//            .inAppendMode()
//            .withFormat(new Json()
//                .deriveSchema()
//                )
//            .registerTableSink("estable");
//        t1.insertInto("estable");
//
//        log.info("Executing {} job", JOB_NAME);
//        env.execute(JOB_NAME);
    }
}
