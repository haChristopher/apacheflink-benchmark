package benchmark.client

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.SynchronousQueue
import java.util.Properties
import java.util.UUID
import java.io.File
import java.io.FileInputStream
import benchmark.client.KafkaRunner

class App {
    var corePoolSize: Int = 2
    var maximumPoolSize: Int = 3
    var keepAliveTime = 100L
    var workQueue = SynchronousQueue<Runnable>()

    fun run(){
        this.setup();
    }

    fun setup() {

        val properties: Properties = loadProperties()
        var clientId: String? = System.getenv("CLIENT_ID")

        if ( clientId == null ) {
            clientId = UUID.randomUUID().toString()
        }

        val workerPool: ExecutorService = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            workQueue,
        )

        val kafkaRunner: KafkaRunner = KafkaRunner(
            properties.getProperty("KafkaBroker"),
            properties.getProperty("KafkaTopic"),
            clientId
        );
        
        // workerPool.execute(kafkaRunner);
        workerPool.execute(kafkaRunner);
        workerPool.execute(kafkaRunner);

        Thread.sleep(5000)

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

fun main() {
    App().run()
}
