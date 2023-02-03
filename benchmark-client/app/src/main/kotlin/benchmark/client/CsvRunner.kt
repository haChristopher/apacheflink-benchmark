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
        batchSize: Int = 50,
        batchTimeOut: Long = 10000,
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

        while (!Thread.currentThread().isInterrupted()) {
            val time = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            currentFileName = time.toString() + "-" + currentThreadId + "-" + currentFileIndex + ".csv"
            var list: MutableList<CSVWriteable> = mutableListOf()

            var currentFile = File(basePath + currentFileName)
            currentFile.getParentFile().mkdirs();
            currentFile.createNewFile();

            var headerWritten = false

            while (currentNumRecords < this.recordsPerFile) {

                try {
                    val timeoutAt: Long = System.currentTimeMillis() + batchTimeOut;
                    while(list.size < this.batchSize || timeoutAt > System.currentTimeMillis()) {
                        list.add(queue.poll(3, TimeUnit.SECONDS));
                    }
                } catch (e: Exception) {
                    Thread.currentThread().interrupt();
                }

                if (!headerWritten) {
                    currentFile.writeText(list.get(0).toPropertyCSVString())
                    currentFile.appendText("\n")
                    headerWritten = true
                }

                list.toList().forEach {
                    currentFile.appendText(it.toCSVString())
                    currentFile.appendText("\n")
                }

                list.clear()
            }
        }
    }

}