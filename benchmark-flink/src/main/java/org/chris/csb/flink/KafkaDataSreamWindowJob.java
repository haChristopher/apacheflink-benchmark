package org.chris.csb.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

/**
 * Example data streaming job using window operators
 */
public class KafkaDataSreamWindowJob {

	public static void main(String[] args) throws Exception {
		
		String jobName= "FlinkWindowSample";
		String inputTopic = "flink-input";
		String outputTopic = "flink-output";
		String consumerGroup = "benchmark";
		String broker = "192.168.178.110:9092";

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		KafkaSource<String> source = KafkaSource.<String>builder()
			.setBootstrapServers(broker)
			.setTopics(inputTopic)
			.setGroupId(consumerGroup)
			.setStartingOffsets(OffsetsInitializer.earliest())
			.setValueOnlyDeserializer(new SimpleStringSchema())
			.build();

		KafkaSink<String> sink = KafkaSink.<String>builder()
			.setBootstrapServers(broker)
			.setRecordSerializer(KafkaRecordSerializationSchema.builder()
				.setTopic(outputTopic)
				.setValueSerializationSchema(new SimpleStringSchema())
				.build()
			)
			.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
			.build();
	
		
		DataStream<String> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");	
		
		DataStream<String> mapped = stream.map(new MapFunction<String, String>() {
			@Override
			public String map(String value) throws Exception {
				return value + "went trough";
			}
		});
		
		mapped.sinkTo(sink);

		env.execute(jobName);
	}
}
