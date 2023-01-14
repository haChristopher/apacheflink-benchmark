package org.chris.csb.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Example data streaming job using window operators
 */
public class SimpleDownSampling {

	@JsonSerialize
	public class InputMessage {
		String sender;
		String recipient;
		LocalDateTime sentAt;
		String message;
	}

	public static void main(String[] args) throws Exception {
		
		String jobName= "FlinkWindowSample";
		String inputTopic = "flink-input";
		String outputTopic = "flink-output";
		String consumerGroup = "benchmark";
		String broker = "192.168.2.133:9092";
		int allowedLatenessInSeconds = 5;

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		KafkaSource<String> source = KafkaSource.<String>builder()
			.setBootstrapServers(broker)
			.setTopics(inputTopic)
			.setGroupId(consumerGroup)
			.setStartingOffsets(OffsetsInitializer.latest())
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


		WatermarkStrategy<Tuple2<Long, String>>  basicStrategy = WatermarkStrategy
			.<Tuple2<Long, String>>forBoundedOutOfOrderness(Duration.ofSeconds(6))
			.withTimestampAssigner((event, timestamp) -> event.f0);

		
		DataStream<String> stream = env.fromSource(source, basicStrategy, "Kafka Source");	
		

		DataStream<String> downsampled = stream
			.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(5)))
			.allowedLateness( Time.seconds( allowedLatenessInSeconds ) )
			.sum(2);
			// .process( new ProcessAllWindowFunction<Element, Integer ,TimeWindow>()
			// {
			// 	@Override
			// 	public void process( Context arg0, Iterable<Element> input, Collector<Integer> output ) throws Exception
			// 	{
			// 		output.collect( 1 );
			// 	}
			// });

		downsampled.sinkTo(sink);
		
		env.execute(jobName);
	}
}
