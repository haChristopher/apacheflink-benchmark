package org.chris.csb.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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

		// Load properties from config file
		Properties prop = new Properties();
		try (InputStream input = SimpleDownSampling.class.getClassLoader().getResourceAsStream("config.properties");) {
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		String jobName= "FlinkWindowSample";
		String inputTopic = prop.getProperty("InputTopic", "flink-input");
		String outputTopic =  prop.getProperty("OutputTopic", "flink-output");
		String consumerGroup = prop.getProperty("ConsumerGroup", "benchmark");
		String broker = prop.getProperty("KafkaBroker", "host.docker.internal:9092");
		int allowedLatenessInSeconds = Integer.parseInt(prop.getProperty("AllowedLatenessInSeconds", "50"));
		int consideredLateAfterSeconds = Integer.parseInt(prop.getProperty("ConsiderLaterAfterSeconds", "0"));
		int windowSizeInSeconds = Integer.parseInt(prop.getProperty("WindowSizeInSeconds", "10"));

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		/* SOURCE */
		KafkaSource<ObjectNode> source = KafkaSource.<ObjectNode>builder()
			.setBootstrapServers(broker)
			.setTopics(inputTopic)
			.setGroupId(consumerGroup)
			.setDeserializer(KafkaRecordDeserializationSchema.of(
				new JSONKeyValueDeserializationSchema(true)
			))
			.setStartingOffsets(OffsetsInitializer.latest())
			//.setStartingOffsets(OffsetsInitializer.earliest())
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
					public long extractTimestamp(ObjectNode element, long previousElementTimestamp) {
						Long timeStamp = 0L;
						try {
							// String value = element.get("value").get("content").get("timestamp").asText();
							// timeStamp = Instant.parse(value).getEpochSecond();
							timeStamp = element.get("value").get("sendTimestamp").asLong();
						} catch (Exception e) {
							e.printStackTrace();
						}

						// return timeStamp;
						element.put("writeInTimestamp", previousElementTimestamp);
						return timeStamp;
					}
				}
			);

		/** Event time processing is default setting since flink 1.12 */
		// env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		DataStream<ObjectNode> stream = env.fromSource(source, basicStrategy, "Kafka Source");	
		
		/** TRANSFORMATION */
		DataStream<ObjectNode> downsampled = stream
			.windowAll(TumblingEventTimeWindows.of(Time.seconds(windowSizeInSeconds)))
			.allowedLateness( Time.seconds( allowedLatenessInSeconds ) )
			.process( new ProcessAllWindowFunction<ObjectNode, ObjectNode, TimeWindow>()
			{
				@Override
				public void process( Context ctx, Iterable<ObjectNode> input, Collector<ObjectNode> output ) throws Exception
				{

					ObjectMapper mapper = new ObjectMapper();
					ObjectNode result = mapper.createObjectNode();

					result.put("windowStart", ctx.window().getStart());
					result.put("windowEnd", ctx.window().getEnd());

					List<String> list = new ArrayList<String>();
					Integer numRecords = 0;
					Integer sum = 0;
					Long earliestTimestamp = Long.MAX_VALUE;
					Long latestTimestamp = Long.MIN_VALUE;
					String latestId = "";
					String debug = "";

					try {
						for (ObjectNode element : input) {
							// list.add(element.get("value").get("id").asText());
							// -> This was causing to large messages on big windows
							sum += element.get("value").get("value").asInt();
							long sendTimestamp = element.get("value").get("sendTimestamp").asLong();
							numRecords ++;
							
							// Iterator<String>  it = element.get("value").fieldNames();
							// while(it.hasNext()) {
							// 	debug +=  it.next();
							// }

							if (sendTimestamp < earliestTimestamp) {
								earliestTimestamp = sendTimestamp;
							}
							if (sendTimestamp > latestTimestamp) {
								latestTimestamp = sendTimestamp;
								latestId = element.get("value").get("id").asText();
							}
						}
					} catch (NullPointerException e) {
						// e.printStackTrace();
					}
					// Only add newest record.
					list.add(latestId);

					result.put("debug", debug);
					result.put("sendTimestamp", earliestTimestamp);
					result.put("latestTimestamp", latestTimestamp);
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
