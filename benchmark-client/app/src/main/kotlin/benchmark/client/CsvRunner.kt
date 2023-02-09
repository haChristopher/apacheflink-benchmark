package benchmark.client

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class CsvRunner: Runnable {
    var clientId: String
    var queue: BlockingQueue<CSVWriteable>
    var basePath: String
    var currentNumRecords = 0
    var seperator = ";"
    var currentFileName = ""
    var currentFileIndex = 0
    var currentThreadId: Long
    var batchSize: Int
    var batchTimeOut: Long
    var recordsPerFile: Int

    @JvmOverloads constructor(
        basePath: String,
        queue: BlockingQueue<CSVWriteable>,
        clientId: String,
        batchSize: Int = 5,
        batchTimeOut: Long = 100000,
        recordsPerFile: Int = 10000
    ) {
        this.basePath = basePath
        this.queue = queue
        this.clientId = clientId
        this.currentThreadId = Thread.currentThread()!!.getId()
        this.batchSize = batchSize
        this.batchTimeOut = batchTimeOut
        this.recordsPerFile = recordsPerFile
    }

    public override fun run() {

        // !Thread.currentThread().isInterrupted()
        while (true) {
            val time = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            currentFileName = time.toString() + "-" + currentThreadId + "-" + currentFileIndex + ".csv"
            println("Writing to file: " + currentFileName)

            var list: MutableList<CSVWriteable> = mutableListOf()
            var currentFile = File(basePath + currentFileName)
            currentFile.getParentFile().mkdirs();
            currentFile.createNewFile();

            currentNumRecords = 0
            var headerWritten = false

            while (currentNumRecords < this.recordsPerFile) {

                try {
                    val timeoutAt: Long = System.currentTimeMillis() + batchTimeOut;
                    println("message")
                    while(list.size < this.batchSize && timeoutAt > System.currentTimeMillis()) {
                        list.add(queue.poll(10, TimeUnit.SECONDS));
                        println("Polling")
                        println(list.size)
                    }
                } catch (e: Exception) {
                    println("Error while polling" + e.stackTraceToString())
                    Thread.currentThread().interrupt();
                }

                if (!headerWritten && list.size > 0) {
                    currentFile.writeText(list.get(0).toPropertyCSVString())
                    currentFile.appendText("\n")
                    headerWritten = true
                }

                list.toList().forEach {
                    currentFile.appendText(it.toCSVString())
                    currentFile.appendText("\n")
                }

                currentNumRecords += list.size
                list.clear()
            }
        }
    }

}