package org.chris.csb.flink.serializers;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.ProducerRecord;


public class RecordSerializationSchema implements KafkaRecordSerializationSchema<ObjectNode>{

    private String topic;   

    public RecordSerializationSchema(String topic) {
        super();
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(ObjectNode element, KafkaSinkContext context, Long timestamp) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<byte[], byte[]>(topic, new byte[0]);
        try {
            record = new ProducerRecord<byte[], byte[]>(topic, element.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return record;
    }

}