package com.amazonaws.services.msf;

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink;
import org.apache.flink.connector.kinesis.source.KinesisStreamsSource;
import org.apache.flink.connector.kinesis.source.config.KinesisSourceConfigOptions;
import org.apache.flink.connector.kinesis.source.enumerator.assigner.ShardAssignerFactory;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * A basic Flink Java application to run on Amazon Managed Service for Apache Flink,
 * with Kinesis Data Streams as source and sink.
 */
public class BasicStreamingJob {

    private static final Logger LOGGER = LogManager.getLogger(BasicStreamingJob.class);

    // Name of the local JSON resource with the application properties in the same format as they are received from the Amazon Managed Service for Apache Flink runtime
    private static final String LOCAL_APPLICATION_PROPERTIES_RESOURCE = "flink-application-properties-dev.json";

    /**
     * Load application properties from Amazon Managed Service for Apache Flink runtime or from a local resource, when the environment is local
     */
    private static Map<String, Properties> loadApplicationProperties(StreamExecutionEnvironment env) throws IOException {
        if (env instanceof LocalStreamEnvironment) {
            LOGGER.info("Loading application properties from '{}'", LOCAL_APPLICATION_PROPERTIES_RESOURCE);
            return KinesisAnalyticsRuntime.getApplicationProperties(
                    BasicStreamingJob.class.getClassLoader()
                            .getResource(LOCAL_APPLICATION_PROPERTIES_RESOURCE).getPath());
        } else {
            LOGGER.info("Loading application properties from Amazon Managed Service for Apache Flink");
            return KinesisAnalyticsRuntime.getApplicationProperties();
        }
    }

    private static FlinkKinesisConsumer<String> createSource(Properties inputProperties) {
        String inputStreamName = inputProperties.getProperty("stream.name");

        return new FlinkKinesisConsumer<>(inputStreamName, new SimpleStringSchema(), inputProperties);
    }

    private static KinesisStreamsSource<String> kinesisSource(Properties inputProperties) {
        Configuration sourceConfig = new Configuration();
        sourceConfig.set(KinesisSourceConfigOptions.STREAM_INITIAL_POSITION,
                KinesisSourceConfigOptions.InitialPosition.TRIM_HORIZON);


// Create a new KinesisStreamsSource to read from specified Kinesis Stream.
        KinesisStreamsSource<String> kdsSource =
                KinesisStreamsSource.<String>builder()
                        .setStreamArn("arn:aws:kinesis:us-east-1:000000000000:stream/ExampleInputStream")
                        .setSourceConfig(sourceConfig)

                        .setDeserializationSchema(new SimpleStringSchema())
                        .setKinesisShardAssigner(ShardAssignerFactory.uniformShardAssigner()) // This is optional, by default uniformShardAssigner will be used.
                        .build();
        return kdsSource;

    }

    private static KinesisStreamsSink<String> createSink(Properties outputProperties) {
        String outputStreamName = outputProperties.getProperty("stream.name");
        return KinesisStreamsSink.<String>builder()
                .setKinesisClientProperties(outputProperties)
                .setSerializationSchema(new SimpleStringSchema())
                .setStreamName(outputStreamName)
                .setPartitionKeyGenerator(element -> String.valueOf(element.hashCode()))
                .build();
    }

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
     //   Configuration props = new Configuration();

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Load application parameters
        final Map<String, Properties> applicationParameters = loadApplicationProperties(env);

        SourceFunction<String> source = createSource(applicationParameters.get("InputStream0"));
        DataStream<String> input = env.addSource(source, "Kinesis Source");
        input.print();
        Sink<String> sink = createSink(applicationParameters.get("OutputStream0"));
        input.sinkTo(sink);

        env.execute("Flink streaming Java API skeleton");
    }

}
