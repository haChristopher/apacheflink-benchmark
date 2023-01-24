package benchmark.client

import java.util.Properties
import java.util.concurrent.BlockingQueue
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant
import java.time.Duration

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.InvalidFormatException

import kotlin.random.Random

class KafkaConsumerRunner: Runnable {

    var broker: String
    var topic: String
    var clientId: String
    var consumerGroup: String
    var queue: BlockingQueue<ResultMessage>

    constructor(broker: String, topic: String,  group: String, queue: BlockingQueue<ResultMessage>, clientId: String) {
        this.broker = broker
        this.topic = topic
        this.consumerGroup = group
        this.queue = queue
        this.clientId = clientId
    }

    public override fun run() {
        val consumer: Consumer<String, String> = createConsumer(this.broker, this.consumerGroup)

        val jsonMapper = ObjectMapper().apply {
            enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            setDateFormat(StdDateFormat())
        }

        consumer.subscribe(listOf(this.topic))

        while (true) {
            val records = consumer.poll(Duration.ofSeconds(20))
            records.iterator().forEach {
                if (it.value() != null) {
                    val messageJson = it.value()
                    try {
                        var message = jsonMapper.readValue(messageJson, ResultMessage::class.java)
                        message.writeOutTimestamp = it.timestamp()
                        message.writeOutType = it.timestampType().toString()
                        this.queue.add(message)
                    } catch (e: InvalidDefinitionException ) {
                        e.printStackTrace()
                        println("Ignoring incomplete message: {$messageJson}")
                    } catch (e: InvalidFormatException ) {
                        println("Ignoring invalid message: {$messageJson} has wrong datatype {$e.stackTrace}")
                    }
                }
            }
        }

    }

    private fun createConsumer(broker: String, group: String): Consumer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = broker
        props["group.id"] = group
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["auto.offset.reset"] = "earliest"

        return KafkaConsumer<String, String>(props)
    }
}