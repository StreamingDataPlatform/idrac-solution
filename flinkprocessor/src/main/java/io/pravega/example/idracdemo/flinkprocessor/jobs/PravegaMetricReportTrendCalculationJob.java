package io.pravega.example.idracdemo.flinkprocessor.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.StreamCut;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.serialization.JsonDeserializationSchema;
import io.pravega.example.idracdemo.flinkprocessor.AbstractJob;
import io.pravega.example.idracdemo.flinkprocessor.AppConfiguration;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.FlatMetricReport;
import io.pravega.example.idracdemo.flinkprocessor.datatypes.TrendMetric;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class PravegaMetricReportTrendCalculationJob extends AbstractJob {


    public PravegaMetricReportTrendCalculationJob(AppConfiguration appConfiguration) {
        super(appConfiguration);
    }

    @Override
    protected ElasticsearchSinkFunction<TrendMetric> getResultSinkFunction() {
        return new ElasticSearchUpsertSink(appConfiguration.getElasticSearch().getIndex());
    }

    @Override
    public void run() {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            createStream(appConfiguration.getInputStreamConfig());
            createStream(appConfiguration.getOutputStreamConfig());
            StreamManager streamManager = StreamManager.create(appConfiguration.getPravegaConfig().getClientConfig());
            StreamCut tailStreamCut = streamManager.getStreamInfo(appConfiguration.getInputStreamConfig().getStream().getScope(),
                    appConfiguration.getInputStreamConfig().getStream().getStreamName()).getTailStreamCut();
            FlinkPravegaReader<FlatMetricReport> flinkPravegaReader = FlinkPravegaReader.builder()
                    .withPravegaConfig(appConfiguration.getPravegaConfig())
                    .forStream(appConfiguration.getInputStreamConfig().getStream(), tailStreamCut)
                    .withDeserializationSchema(new JsonDeserializationSchema(FlatMetricReport.class))
                    .build();
            DataStream source = env
                    .addSource(flinkPravegaReader)
                    .name("PravegaMetricReportTrendCalculationJob").assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks<FlatMetricReport>() {
                        private final long maxOutOfOrderness = 3500; // 3.5 seconds

                        private long currentMaxTimestamp;

                        @Override
                        public long extractTimestamp(FlatMetricReport element, long previousElementTimestamp) {
                            DateFormat desiredFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            desiredFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            Date date;
                            try {
                                date = desiredFormat.parse(element.Timestamp);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            currentMaxTimestamp = Math.max(date.getTime(), currentMaxTimestamp);
                            return date.getTime();
                        }

                        @Nullable
                        @Override
                        public Watermark getCurrentWatermark() {
                            // return the watermark as current highest timestamp minus the out-of-orderness bound
                            return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
                        }
                    });
            KeyedStream keyedStream = source.keyBy(new KeySelector<FlatMetricReport, Tuple2<String, String>>() {
                @Override
                public Tuple2<String, String> getKey(FlatMetricReport flatMetricReport) throws Exception {
                    return new Tuple2<String, String>(flatMetricReport.MetricId, flatMetricReport.RemoteAddr);
                }
            });
            ElasticsearchSink<FlatMetricReport> elasticSinkByMinute = newElasticSearchSink();
            ElasticsearchSink<FlatMetricReport> elasticSinkByHour = newElasticSearchSink();
            ElasticsearchSink<FlatMetricReport> elasticSinkByDay = newElasticSearchSink();
            keyedStream.timeWindow(Time.minutes(1)).aggregate(new LinearRegressionAggregateMetricByTime(TimeUnit.MINUTES)).addSink(elasticSinkByMinute);
            keyedStream.timeWindow(Time.hours(1)).aggregate(new LinearRegressionAggregateMetricByTime(TimeUnit.HOURS)).addSink(elasticSinkByHour);
            keyedStream.timeWindow(Time.days(1)).aggregate(new LinearRegressionAggregateMetricByTime(TimeUnit.DAYS)).addSink(elasticSinkByDay);
            env.execute(PravegaMetricReportTrendCalculationJob.class.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static class LinearRegressionAverageAccumulator {
        public double sum_x;
        public double sum_y;
        public double sum_xy;
        public double sum_xx;
        public long firstTimeStamp = Long.MIN_VALUE;
        public long lastTimeStamp;
        public int count;
        public double firstY = Double.MIN_VALUE;
        public TrendMetric trendMetric;
    }

    public static class ElasticSearchUpsertSink implements ElasticsearchSinkFunction<TrendMetric> {
        private final String index;
        private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        public ElasticSearchUpsertSink(String index) {
            this.index = index;
        }

        @Override
        public void process(TrendMetric element, RuntimeContext ctx, RequestIndexer indexer) {
            try {
                indexer.add(createUpsertRequest(element));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private UpdateRequest createUpsertRequest(TrendMetric element) throws Exception {
            String docId = element.MetricId + "-" + element.RemoteAddr;
            byte[] bytes = objectMapper.writeValueAsBytes(element);
            return new UpdateRequest(index, "update", docId).doc(bytes, XContentType.JSON).upsert(bytes, XContentType.JSON).docAsUpsert(true);
        }
    }

    public static class LinearRegressionAggregateMetricByTime implements AggregateFunction<FlatMetricReport, LinearRegressionAverageAccumulator, TrendMetric> {

        private TimeUnit timeUnit;

        public LinearRegressionAggregateMetricByTime(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        @Override
        public LinearRegressionAverageAccumulator createAccumulator() {
            return new LinearRegressionAverageAccumulator();
        }

        @Override
        public LinearRegressionAverageAccumulator add(FlatMetricReport flatMetricReport, LinearRegressionAverageAccumulator accumulator) {
            DateFormat desiredFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            desiredFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date;
            try {
                date = desiredFormat.parse(flatMetricReport.Timestamp);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            if (accumulator.firstTimeStamp == Long.MIN_VALUE) {
                accumulator.firstTimeStamp = date.getTime();
                accumulator.firstY = flatMetricReport.MetricValue;
                accumulator.trendMetric = new TrendMetric();
                accumulator.trendMetric.MetricId = flatMetricReport.MetricId;
                accumulator.trendMetric.RemoteAddr = flatMetricReport.RemoteAddr;
            }
            double currentX = (date.getTime() - accumulator.firstTimeStamp) / 1000.0;
            accumulator.lastTimeStamp = Math.max(accumulator.lastTimeStamp, date.getTime());
            accumulator.count++;
            accumulator.sum_x += currentX;
            accumulator.sum_y += flatMetricReport.MetricValue;
            accumulator.sum_xx += currentX * currentX;
            accumulator.sum_xy += (currentX * flatMetricReport.MetricValue);
            return accumulator;
        }

        @Override
        public TrendMetric getResult(LinearRegressionAverageAccumulator accumulator) {
            if (accumulator.count == 0) {
                return accumulator.trendMetric;
            }
            if (accumulator.firstY == 0) {
                accumulator.firstY = 1;
                accumulator.firstY = 1;
            }
            double arg_x = accumulator.sum_x / accumulator.count;
            double arg_y = accumulator.sum_y / accumulator.count;
            double arg_xx = accumulator.sum_xx / accumulator.count;
            double arg_xy = accumulator.sum_xy / accumulator.count;
            double a = arg_xy - arg_x * arg_y;
            double b = arg_xx - (arg_x * arg_x);
            if (b == 0) {
                b = 1;
            }
            double slop = (((a / b) * (accumulator.lastTimeStamp - accumulator.firstTimeStamp) / 1000) / accumulator.firstY);
            String slopDescription = getSlopDescription(slop);
            switch (timeUnit) {
                case MINUTES: {
                    accumulator.trendMetric.CurrentValue = arg_y;
                    accumulator.trendMetric.MinuteTrend = slop;
                    accumulator.trendMetric.MinuteDescription = slopDescription;
                    break;
                }
                case HOURS: {
                    accumulator.trendMetric.HourTrend = slop;
                    accumulator.trendMetric.HourDescription = slopDescription;
                    break;
                }
                case DAYS: {
                    accumulator.trendMetric.DayTrend = slop;
                    accumulator.trendMetric.DayDescription = slopDescription;
                    break;
                }
                default:
                    new RuntimeException("not supported time unit");
            }
            return accumulator.trendMetric;
        }

        @Override
        public LinearRegressionAverageAccumulator merge(LinearRegressionAverageAccumulator a, LinearRegressionAverageAccumulator b) {
            return a.count > b.count ? a : b;
        }


        private String getSlopDescription(double slop) {
            double atan = Math.atan(slop) * (180 / Math.PI);
            if (atan >= 0 && atan < 10.5) {
                return "Flat";
            } else if (atan >= 10.5 && atan < 22.5) {
                return "22.5 UP";
            } else if (atan >= 22.5 && atan < 45) {
                return "45 UP";
            } else if (atan >= 45 && atan < 75) {
                return "75 UP";
            } else if (atan >= 75 && atan < 90) {
                return "Vertical UP";
            }
            if (-atan >= 0 && -atan < 10.5) {
                return "Flat";
            } else if (-atan >= 10.5 && -atan < 22.5) {
                return "22.5 Down";
            } else if (-atan >= 22.5 && -atan < 45) {
                return "45 Down";
            } else if (-atan >= 45 && -atan < 75) {
                return "75 Down";
            } else if (-atan >= 75 && -atan < 90) {
                return "Vertical Down";
            }
            return "";
        }

    }
}


