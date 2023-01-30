package benchmark.client

import java.util.Properties
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat

import kotlin.random.Random

class KafkaProducerRunner: Runnable {

    var broker: String
    var topic: String
    var clientId: String

    constructor(broker: String, topic: String, clientId: String) {
        this.broker = broker
        this.topic = topic
        this.clientId = clientId
    }

    public override fun run() {
        val producer: Producer<String, String> = createProducer(this.broker)

        val jsonMapper = ObjectMapper().apply {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            setDateFormat(StdDateFormat())
        }

        val currThreadId: Long = Thread.currentThread()!!.getId()
        
        for (i in 1..20) {
            println("${Thread.currentThread()} has run.")
            Thread.sleep(1_000)
            
            val timestamp = System.currentTimeMillis();

            var message = Message(
                id = Random.nextInt(0, 10000),
                sendTimestamp = timestamp,
                value = i,
                threadId = currThreadId,
                benchmarkClientID = this.clientId

            )
            val jsonMessage: String = jsonMapper.writeValueAsString(message);
            
            val futureResult = producer.send(ProducerRecord(this.topic, jsonMessage))
            futureResult.get()
        }
    }

    private fun createProducer(broker: String): Producer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = broker
        props["key.serializer"] = StringSerializer::class.java.canonicalName
        props["value.serializer"] = StringSerializer::class.java.canonicalName
        return KafkaProducer<String, String>(props)
    }
}