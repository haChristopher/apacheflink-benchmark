package benchmark.client

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.SynchronousQueue
import java.util.Properties
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

import java.io.File
import java.io.FileInputStream
import benchmark.client.KafkaProducerRunner
import benchmark.client.KafkaDelayProducerRunner
import benchmark.client.KafkaConsumerRunner

class App {
    var corePoolSize: Int = 2
    var maximumPoolSize: Int = 3
    var keepAliveTime = 100L
    var workQueue = SynchronousQueue<Runnable>()
    var csvPath = "/"
    lateinit var workerPool: ExecutorService
    lateinit var properties: Properties
    lateinit var queue: BlockingQueue<CSVWriteable>
    lateinit var mode: String
    var msgsPerSecond: Int = 0
    var percentLate: Int = 0
    var latenessInSecond: Int = 0

    fun run(mode: String ){
        this.mode = mode
        this.setup();
    }

    fun setup() {
        var clientId: String? = System.getenv("CLIENT_ID")
        
        // Get Configuration
        this.properties = loadProperties()
        this.msgsPerSecond = Integer.parseInt(properties.getProperty("MessagesPerSecond"), 10);
        this.percentLate = Integer.parseInt(properties.getProperty("PercentageOfLateMsgs"), 10);
        this.latenessInSecond = Integer.parseInt(properties.getProperty("LatenessInSecond"), 10);

        if (this.mode == "") {
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

        queue = LinkedBlockingDeque<CSVWriteable>();

        if (mode == "consume") {
            println("Consumer mode selected");
            csvPath = "results/"

            val kafkaConsumer: KafkaConsumerRunner = KafkaConsumerRunner(
                properties.getProperty("KafkaBroker"),
                properties.getProperty("KafkaOutTopic"),
                properties.getProperty("ConsumerGroup"),
                queue,
                clientId.toString()
            );
            
            workerPool.execute(kafkaConsumer);
            workerPool.execute(kafkaConsumer);
        } else {
            print("Producer mode selected");
            csvPath = "messages/"

            val kafkaProducer: KafkaDelayProducerRunner = KafkaDelayProducerRunner(
                broker = properties.getProperty("KafkaBroker"),
                topic = properties.getProperty("KafkaInTopic"),
                clientId = clientId.toString(),
                messagesPerSecond = this.msgsPerSecond,
                percentLate = this.percentLate,
                latenessInSecond = this.latenessInSecond,
                queue
            );

            workerPool.execute(kafkaProducer);
            // workerPool.execute(kafkaProducer);
            // workerPool.execute(kafkaProducer);
        }

        // Setup to csv writer
        val csvWriter: CsvRunner = CsvRunner(
            csvPath,
            queue,
            clientId.toString()
        );

        workerPool.execute(csvWriter);

        Thread.sleep(20000)
        workerPool.shutdown()

    }

    fun loadProperties(): Properties {
        val configFilePath: String = System.getenv("CONFIG_FILE") ?: "config.properties"

        val file = File(configFilePath)

        val prop = Properties()
        FileInputStream(file).use { prop.load(it) }

        // Print found properties
        println("Found following settings:")
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
