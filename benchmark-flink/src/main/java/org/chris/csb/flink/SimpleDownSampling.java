package org.chris.csb.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.chris.csb.flink.serializers.RecordSerializationSchema;

import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple Downsampling Windowing Job
 */
public class SimpleDownSampling {

	public static void main(String[] args) throws Exception {
		
		String jobName= "FlinkWindowSample";
		String inputTopic = "flink-input";
		String outputTopic = "flink-output";
		String consumerGroup = "benchmark";
		String broker = "host.docker.internal:9092";
		int allowedLatenessInSeconds = 5;
		int consideredLateAfterSeconds = 6;
		int windowSizeInSeconds = 5;

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		/* SOURCE */
		KafkaSource<ObjectNode> source = KafkaSource.<ObjectNode>builder()
			.setBootstrapServers(broker)
			.setTopics(inputTopic)
			.setGroupId(consumerGroup)
			.setDeserializer(KafkaRecordDeserializationSchema.of(
				new JSONKeyValueDeserializationSchema(false)
			))
			.setStartingOffsets(OffsetsInitializer.latest())
			.build();

		/* SINK */
		KafkaSink<ObjectNode> sink = KafkaSink.<ObjectNode>builder()
			.setBootstrapServers(broker)
			.setRecordSerializer(new RecordSerializationSchema(outputTopic))
			.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
			.build();


		/** Extracts timestamp from stream element and uses it as watermark. */
		WatermarkStrategy<ObjectNode>  basicStrategy = WatermarkStrategy
			.<ObjectNode>forBoundedOutOfOrderness(Duration.ofSeconds(consideredLateAfterSeconds))
			.withTimestampAssigner(
				new SerializableTimestampAssigner<ObjectNode>() {
					@Override
					public long extractTimestamp(ObjectNode element, long recordTimestamp) {
						Long timeStamp = 0L;
						try {
							String value = element.get("value").get("content").get("timestamp").asText();
							timeStamp = Instant.parse(value).getEpochSecond();
						} catch (Exception e) {
							e.printStackTrace();
						}

						return timeStamp;
					}
				}
			);

		
		DataStream<ObjectNode> stream = env.fromSource(source, basicStrategy, "Kafka Source");	
		

		/** TRANSFORMATION */
		DataStream<ObjectNode> downsampled = stream
			.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(windowSizeInSeconds)))
			.allowedLateness( Time.seconds( allowedLatenessInSeconds ) )
			.process( new ProcessAllWindowFunction<ObjectNode, ObjectNode, TimeWindow>()
			{
				@Override
				public void process( Context arg0, Iterable<ObjectNode> input, Collector<ObjectNode> output ) throws Exception
				{

					ObjectMapper mapper = new ObjectMapper();
					ObjectNode result = mapper.createObjectNode();

					result.put("windowStart", arg0.window().getStart());
					result.put("windowEnd", arg0.window().getEnd());

					List<Integer> list = new ArrayList<Integer>();
					Integer numRecords = 0;
					Integer sum = 0;

					try {
						for (ObjectNode element : input) {
							list.add(element.get("value").get("id").asInt());
							sum += element.get("value").get("value").asInt();
							numRecords ++;
						}
					} catch (NullPointerException e) {
						// Do nothing here, fix later
					}

					// get timestamp of earliest value

					result.put("value", sum);
					result.put("numRecords", numRecords);
					ArrayNode array = mapper.valueToTree(list);
					result.putArray("composed").addAll(array);

					output.collect( result );
				}
			});

		downsampled.sinkTo(sink);
		
		env.execute(jobName);
	}
}
