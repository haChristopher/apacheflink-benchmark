package benchmark.client

data class Message(
        val id: Int,
        val timestamp: String,
        val value: Int,
        val threadId: Long,
        val benchmarkClientID: String,
)
