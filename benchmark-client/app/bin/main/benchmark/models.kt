package benchmark.client

data class Message(
        val timestampSend: String,
        val value: Int,
        val threadId: Long,
        val benchmarkClientID: String,
)
