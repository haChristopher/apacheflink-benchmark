package benchmark.client

import java.util.Properties
import java.util.concurrent.BlockingQueue
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat

import kotlin.random.Random

class KafkaDelayProducerRunner: Runnable {

    var broker: String
    var topic: String
    var clientId: String
    var messagesPerSecond: Int = 0;
    var percentLate: Int = 0;
    var latenessInSecond: Int = 0;
    var queue: BlockingQueue<CSVWriteable>

    constructor(
        broker: String,
        topic: String,
        clientId: String,
        messagesPerSecond: Int,
        percentLate: Int = 0,
        latenessInSecond: Int = 0,
        queue: BlockingQueue<CSVWriteable>
    ) 
    {
        this.broker = broker
        this.topic = topic
        this.clientId = clientId
        this.messagesPerSecond = messagesPerSecond
        this.percentLate = percentLate
        this.latenessInSecond = latenessInSecond
        this.queue = queue
    }

    public override fun run() {
        println("${Thread.currentThread()} has run.")
        
        val producer: Producer<String, String> = createProducer(this.broker)

        val jsonMapper = ObjectMapper().apply {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            setDateFormat(StdDateFormat())
        }

        val currThreadId: Long = Thread.currentThread()!!.getId()

        var sleepTimeInMs: Long = (1.0 / messagesPerSecond * 1000).toLong()

        var messageCount = 0;
        
        while (messageCount < messagesPerSecond * 60) {
            Thread.sleep(sleepTimeInMs)
            var timestamp = System.currentTimeMillis();

            var message = Message(
                id = currThreadId.toString() + "-" + messageCount,
                sendTimestamp = timestamp,
                value = Random.nextInt(0, 10000),
                threadId = currThreadId,
                benchmarkClientID = this.clientId
            )
            
            /** Check if message should be late */
            if (Random.nextInt(0, 100) < this.percentLate) {
                message.sendTimestamp -= this.latenessInSecond * 1000;
                message.wasLate = true;
            }

            val jsonMessage: String = jsonMapper.writeValueAsString(message);
            
            val futureResult = producer.send(ProducerRecord(this.topic, jsonMessage))
            val meta: RecordMetadata = futureResult.get()
            message.logAppendTime = meta.timestamp()
            this.queue.add(message)

            messageCount ++;
        }
    }

    private fun createProducer(broker: String): Producer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = broker
        props["key.serializer"] = StringSerializer::class.java.canonicalName
        props["value.serializer"] = StringSerializer::class.java.canonicalName
        return KafkaProducer<String, String>(props)
    }

    private fun generateData(){

    }
}