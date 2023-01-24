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
    var queue: BlockingQueue<ResultMessage>
    var basePath: String
    var currentNumRecords = 0
    var seperator = ";"
    var currentFileName = ""
    var currentFileIndex = 0
    var currentThreadId: Long

    constructor(basePath: String, queue: BlockingQueue<ResultMessage>, clientId: String) {
        this.basePath = basePath
        this.queue = queue
        this.clientId = clientId
        this.currentThreadId = Thread.currentThread()!!.getId()
    }

    public override fun run() {

        while (!Thread.currentThread().isInterrupted()) {
            // create new file name
            val time = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            currentFileName = time.toString() + "-" + currentThreadId + "-" + currentFileIndex + ".csv"
            var list: MutableList<ResultMessage> = mutableListOf()

            var currentFile = File(currentFileName)

            currentFile.writeText(ResultMessage.Helper.toPropertyCSVString())
            currentFile.appendText("\n")

            // currentFile.bufferedWriter().use { out ->
            //     out.write("id;timestamp;value;windowEnd;windowStart;numRecords;composed")
            //     out.newLine()
            // }
        
            while (currentNumRecords < 1000) {

                try {
                    while(list.size < 5) {
                        list.add(queue.take());
                    }
                    // println(list[0].toString())
                } catch (e: Exception) {
                    Thread.currentThread().interrupt();
                }

                // currentFile.bufferedWriter().use{ out ->
                //     list.toList().forEach {
                //         //${it.id}, ${it.timestamp
                //         out.append(";;${it.value};${it.windowEnd};${it.windowStart};${it.numRecords};")
                //         out.append(it.composed.joinToString(prefix = "[", postfix = "]", separator = ","))
                //         out.newLine()
                //     }
                // }

                list.toList().forEach {
                    currentFile.appendText(it.toString())
                    //currentFile.appendText(";;${it.value};${it.windowEnd};${it.windowStart};${it.numRecords};")
                    //currentFile.appendText(it.composed.joinToString(prefix = "[", postfix = "]", separator = ","))
                    currentFile.appendText("\n")
                }

                list.clear()
            }
        }
    }

}