package benchmark.client

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.SynchronousQueue
import java.util.Properties
import java.util.UUID
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import java.io.File
import java.io.FileInputStream
import benchmark.client.KafkaProducerRunner
import benchmark.client.KafkaConsumerRunner

class App {
    var corePoolSize: Int = 2
    var maximumPoolSize: Int = 3
    var keepAliveTime = 100L
    var workQueue = SynchronousQueue<Runnable>()
    lateinit var workerPool: ExecutorService
    lateinit var properties: Properties
    lateinit var queue: BlockingQueue<ResultMessage>
    lateinit var mode: String

    fun run(mode: String ){
        this.mode = mode
        this.setup();
    }

    fun setup() {

        properties = loadProperties()
        var clientId: String? = System.getenv("CLIENT_ID")

        if (this.mode == ""){
            this.mode = properties.getProperty("Mode")
        }

        if ( clientId == null) {
            clientId = UUID.randomUUID().toString()
        }

        val workerPool: ExecutorService = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            workQueue,
        )

        queue = LinkedBlockingDeque<ResultMessage>();

        if (mode == "consume") {
            println("Consumer mode selected");

            val kafkaConsumer: KafkaConsumerRunner = KafkaConsumerRunner(
                properties.getProperty("KafkaBroker"),
                properties.getProperty("KafkaOutTopic"),
                properties.getProperty("ConsumerGroup"),
                queue,
                clientId.toString()
            );

            val csvWriter: CsvRunner = CsvRunner(
                "/",
                queue,
                clientId.toString()
            );
            
            workerPool.execute(kafkaConsumer);
            workerPool.execute(kafkaConsumer);
            workerPool.execute(csvWriter);
        } else {
            print("Producer mode selected");

            val kafkaProducer: KafkaProducerRunner = KafkaProducerRunner(
                properties.getProperty("KafkaBroker"),
                properties.getProperty("KafkaInTopic"),
                clientId.toString()
            );

            workerPool.execute(kafkaProducer);
            workerPool.execute(kafkaProducer);
            workerPool.execute(kafkaProducer);
        }

        Thread.sleep(10000)
        workerPool.shutdown()

    }

    fun loadProperties(): Properties {
        val configFilePath: String = System.getenv("CONFIG_FILE") ?: "config.properties"

        val file = File(configFilePath)

        val prop = Properties()
        FileInputStream(file).use { prop.load(it) }

        // Print all properties
        prop.stringPropertyNames()
            .associateWith {prop.getProperty(it)}
            .forEach { println(it) }

        return prop
    }
}

fun main(args: Array<String>) {
    var mode = "";
    if (args.size > 0){
        mode = args[0]
    }
    App().run(mode=mode)
}
