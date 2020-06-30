package io.pravega.example.idracdemo.flinkprocessor.jobs;

import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamCut;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaEventRouter;
import io.pravega.connectors.flink.serialization.JsonDeserializationSchema;
import io.pravega.connectors.flink.serialization.JsonSerializationSchema;
import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.MetricReport;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class PravegaMetricReportToPRavegaMetricValue extends AbstractJob {
    private static final TimeCharacteristic characteristic = TimeCharacteristic.ProcessingTime;
    private static Logger log = LoggerFactory.getLogger(PravegaMetricReportToPRavegaMetricValue.class);

    public PravegaMetricReportToPRavegaMetricValue(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    public void run() {
        try {
            StreamExecutionEnvironment env = initializeFlinkStreaming();
            env.setStreamTimeCharacteristic(characteristic);
            createStream(appConfiguration.getInputStreamConfig());
            createStream(appConfiguration.getOutputStreamConfig());

            MapStateDescriptor<String, String> broadCastConfigDescriptor =
                    new MapStateDescriptor<>("broadCastConfig",
                            BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);

            BroadcastStream<Map<String, String>> broadcastStream = env.addSource(new RackLabelSource())
                    .name("RackLabels")
                    .uid("Racklebels")
                    .flatMap(new FlatMapFunction<RackLabel, Map<String, String>>() {
                                 @Override
                                 public void flatMap(RackLabel value, Collector<Map<String, String>> out) {
                                     try {
                                         HashMap<String, String> data = new HashMap<String, String>();
                                         data.put(value.getId(), value.geRackLabel());
                                         out.collect(data);
                                     } catch (Exception e) {
                                         throw new RuntimeException(e);
                                     }
                                 }
                             }
                    ).name("RackLabel-IP-map").uid("RackLabel-IP-map").setParallelism(1).broadcast(broadCastConfigDescriptor);

            StreamManager streamManager = StreamManager.create(appConfiguration.getPravegaConfig().getClientConfig());
            StreamCut tailStreamCut = streamManager.getStreamInfo(appConfiguration.getInputStreamConfig().getStream().getScope(),
                    appConfiguration.getInputStreamConfig().getStream().getStreamName()).getTailStreamCut();
            FlinkPravegaReader<MetricReport> flinkPravegaReader = FlinkPravegaReader.builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getInputStreamConfig().getStream(), tailStreamCut)
                    .withDeserializationSchema(new JsonDeserializationSchema(MetricReport.class))
                    .build();

            DataStream<MetricReport> events = env
                    .addSource(flinkPravegaReader)
                    .name("idrac-metric-events")
                    .uid("idrac-metric-events");

            DataStream<FlatMetricReport> metricReportEvents = events
                    .keyBy("Id")
                    .connect(broadcastStream)
                    .process(new EnrichMetricReportProcess()).name("produce-flatMetricReport")
                    .uid("produce-flatMetricReport");

            FlinkPravegaWriter<FlatMetricReport> writer = FlinkPravegaWriter.<FlatMetricReport>builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getOutputStreamConfig().getStream())
                    .withSerializationSchema(new JsonSerializationSchema())
                    .withEventRouter(new EventRouter())
                    .build();
            metricReportEvents.addSink(writer).setParallelism(appConfiguration.getWriterParallelism()).name("pravega-flatmetric-stream")
                    .uid("pravega-flatmetric-stream");

            env.execute(PravegaMetricReportToPRavegaMetricValue.class.getSimpleName());
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

    private static class RackLabelSource implements SourceFunction<RackLabel> {
        private boolean running = true;

        @Override
        public void run(SourceContext<RackLabel> ctx) throws Exception {
            InputStream graphFile = getClass().getResourceAsStream("/rack_labels.csv");       // rack labels
            String line = "";
            boolean notCompleted = true;
            while (running) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(graphFile))) {
                    if (br != null && notCompleted) {
                        while ((line = br.readLine()) != null) {
                            // use comma as separator
                            String[] labels = line.split(",");
                            ctx.collect(new RackLabel(labels[0], labels[1]));
                        }
                    }
                } catch (IOException e) {
                    log.error("Racklabel Error: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                notCompleted = false;
            }

        }

        @Override
        public void cancel() {
            running = false;
        }
    }

    /**
     * Simple POJO containing a IP and Rack Label.
     */
    private static class RackLabel implements Serializable {
        private static final long serialVersionUID = -7336372859203407522L;
        private String id;
        private String rackLabel;

        public RackLabel(String id, String label) {
            this.id = id;
            this.rackLabel = label;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String geRackLabel() {
            return rackLabel;
        }

        public void setRackLabel(String rackLabel) {
            this.rackLabel = rackLabel;
        }

        @Override
        public String toString() {
            return "RackLabel{" +
                    "Id='" + id + '\'' +
                    ", RackLabel=" + rackLabel +
                    '}';
        }
    }

    public class EnrichMetricReportProcess extends KeyedBroadcastProcessFunction<String, MetricReport, Map<String, String>, FlatMetricReport> {

        private final MapStateDescriptor<String, String> broadCastConfigDescriptor =
                new MapStateDescriptor<>("broadCastConfig",
                        BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);

        @Override
        public void processElement(MetricReport in, ReadOnlyContext ctx, Collector<FlatMetricReport> out) throws Exception {
            // Get the IP,label map from the broadcast state
            ReadOnlyBroadcastState<String, String> broadcastState = ctx.getBroadcastState(broadCastConfigDescriptor);
            if (in.MetricValues != null) {
                // Groupby metricid and contextID
                Map<Tuple2<String, String>, List<MetricReport.MetricValue>> result = Arrays.stream(in.MetricValues)
                        .collect(
                                Collectors.groupingBy(MetricReport.MetricValue::getMetricId
                                )
                        );
                Set<Tuple2<String, String>> KeySet = result.keySet();
                // convert metric value to double
                for (Tuple2<String, String> key : KeySet) {
                    ArrayList<Double> tempList = new ArrayList<>();
                    MetricReport.MetricValue saved_entry = new MetricReport.MetricValue();
                    for (MetricReport.MetricValue metric : result.get(key)) {
                        Double value;
                        try {
                            value = Double.parseDouble(metric.MetricValue);

                        } catch (NullPointerException e) {
                            continue;

                        } catch (NumberFormatException e) {
                            continue;
                        }
                        tempList.add(value);
                        saved_entry = metric;
                    }
                    // return the majority value in a report
                    Double majorityValue = tempList.stream()
                            .reduce(BinaryOperator.maxBy(Comparator.comparingDouble(o -> Collections.frequency(tempList, o)))).orElse(null);
                    if (majorityValue != null && saved_entry.MetricId != null && in.Id != null) {
                        String Label = "";
                        if (broadcastState.contains(in.RemoteAddr)) {
                            Label = broadcastState.get(in.RemoteAddr);
                        }



                        FlatMetricReport report = new FlatMetricReport();
                        report.Id = in.Id;
                        report.MetricId = saved_entry.MetricId;
                        report.MetricValue = majorityValue;
                        report.RackLabel = Label;
                        report.RemoteAddr = in.RemoteAddr;
                        report.Timestamp = in.Timestamp;
                        report.ContextID = saved_entry.Oem.Dell.ContextID;

                        out.collect(report);
                    }
                }
            }
        }

        @Override
        public void processBroadcastElement(Map<String, String> value, Context ctx, Collector<FlatMetricReport> out) throws Exception {

            BroadcastState<String, String> broadcastState = ctx.getBroadcastState(broadCastConfigDescriptor);
            value.forEach((k, v) -> {
                try {
                    if (!broadcastState.contains(k))
                        broadcastState.put(k, v);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }


}
