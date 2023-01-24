package benchmark.client

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

data class Message(
        val id: Int,
        val timestamp: String,
        val value: Int,
        val threadId: Long,
        val benchmarkClientID: String,
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResultMessage(
        @JsonProperty("id") val id: Int?,
        @JsonProperty("timestamp") val timestamp: String?,
        @JsonProperty("value") var value: Int?,
        @JsonProperty("windowEnd") var windowEnd: Long?,
        @JsonProperty("windowStart") var windowStart: Long?,
        @JsonProperty("numRecords") var numRecords: Int?,
        @JsonProperty("writeOutTimestamp") var writeOutTimestamp: Long?,
        @JsonProperty("writeOutType") var writeOutType: String?,
        @JsonProperty("composed") var composed: IntArray,
) {
        override fun toString(): String {
                var result = listOf(id, timestamp, value, windowEnd, windowStart, numRecords, writeOutTimestamp, writeOutType).joinToString(separator=";")
                result += ";" + composed.joinToString(prefix = "[", postfix = "]", separator = ",")
                return result
        }

        companion object Helper {
                fun toPropertyCSVString(): String {
                        return "id;timestamp;value;windowEnd;windowStart;numRecords;writeOutTimestamp;writeOutType;composed"
                }
        }
}
