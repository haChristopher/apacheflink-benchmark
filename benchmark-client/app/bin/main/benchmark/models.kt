package benchmark.client

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import kotlin.reflect.full.declaredMemberProperties

interface CSVWriteable {
    fun toCSVString(): String {
        return ""; 
    }

    fun toPropertyCSVString(): String {
        return "";
    }
}

data class Message(

    val id: String,
    var sendTimestamp: Long,
    val value: Int,
    val threadId: Long,
    val benchmarkClientID: String,
    var logAppendTime: Long? = null,
    var wasLate: Boolean = false

): CSVWriteable {

    override fun toCSVString(): String {
        return listOf(id, sendTimestamp, value, threadId, benchmarkClientID, logAppendTime, wasLate).joinToString(separator=";")
    }

    override fun toPropertyCSVString(): String {
        // return Message::class.declaredMemberProperties.joinToString(separator=";")
        return "id;sendTimestamp;value;threadId;benchmarkClientI;logAppendTimeD;wasLate";
    }

}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResultMessage(

    @JsonProperty("id") val id: Int?,
    @JsonProperty("timestamp") val timestamp: Long?,
    @JsonProperty("sendTimestamp") val sendTimestamp: Long?,
    @JsonProperty("latestTimestamp") val latestTimestamp: Long?,
    @JsonProperty("value") var value: Int?,
    @JsonProperty("windowEnd") var windowEnd: Long?,
    @JsonProperty("windowStart") var windowStart: Long?,
    @JsonProperty("numRecords") var numRecords: Int?,

    @JsonProperty("writeInTimestamp") val writeInTimestamp: Long?,
    @JsonProperty("writeInType") var writeInType: String?,
    @JsonProperty("writeOutTimestamp") var writeOutTimestamp: Long?,
    @JsonProperty("writeOutType") var writeOutType: String?,
    
    @JsonProperty("composed") var composed: Array<String>,
    @JsonProperty("debug") var debug: String?

): CSVWriteable {

    override fun toCSVString(): String {
            var result = listOf(id, sendTimestamp, latestTimestamp, value, windowEnd, windowStart, numRecords, writeInTimestamp, writeOutTimestamp, writeOutType).joinToString(separator=";")
            result += ";" + composed.joinToString(prefix = "[", postfix = "]", separator = ",")
            return result
    }

    override fun toPropertyCSVString(): String {
        return "id;sendTimestamp;latestTimestamp;value;windowEnd;windowStart;numRecords;writeInTimestamp;writeOutTimestamp;writeOutType;composed"
    }

    companion object Helper {
        fun toPropertyCSVString(): String {
                return "id;sendTimestamp;latestTimestamp;value;windowEnd;windowStart;numRecords;writeInTimestamp;writeOutTimestamp;writeOutType;composed"
        }
    }

}
