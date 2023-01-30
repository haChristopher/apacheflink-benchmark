package benchmark.client

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

data class Message(
        val id: Int,
        val sendTimestamp: Long,
        val value: Int,
        val threadId: Long,
        val benchmarkClientID: String,
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResultMessage(
        @JsonProperty("id") val id: Int?,
        @JsonProperty("timestamp") val timestamp: Long?,
        @JsonProperty("sendTimestamp") val sendTimestamp: Long?,
        @JsonProperty("value") var value: Int?,
        @JsonProperty("windowEnd") var windowEnd: Long?,
        @JsonProperty("windowStart") var windowStart: Long?,
        @JsonProperty("numRecords") var numRecords: Int?,

        // These are needed to calculate Latency
        @JsonProperty("writeInTimestamp") val writeInTimestamp: Long?,
        @JsonProperty("writeInType") var writeInType: String?,
        @JsonProperty("writeOutTimestamp") var writeOutTimestamp: Long?,
        @JsonProperty("writeOutType") var writeOutType: String?,
        
        @JsonProperty("composed") var composed: IntArray,
        @JsonProperty("debug") var debug: String?
) {
        override fun toString(): String {
                var result = listOf(id, sendTimestamp, value, windowEnd, windowStart, numRecords, writeOutTimestamp, writeOutType).joinToString(separator=";")
                result += ";" + composed.joinToString(prefix = "[", postfix = "]", separator = ",")
                return result
        }

        companion object Helper {
                fun toPropertyCSVString(): String {
                        return "id;sendTimestamp;value;windowEnd;windowStart;numRecords;writeOutTimestamp;writeOutType;composed"
                }
        }
}
