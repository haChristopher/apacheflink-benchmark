package org.chris.csb.flink.aggregations;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

/**

class SimpleSumAggregator implements AggregateFunction<ObjectNode, AverageAccumulator, Tuple2> {

    @Override
    public AverageAccumulator createAccumulator() {
        return new AverageAccumulator();
    }

    @Override
    public AverageAccumulator add(ObjectNode value, AverageAccumulator accumulator) {
        // a little bit weird but we need to keep the key as part of the accumulator to be able
        // to serialize it back in the end
        accumulator.key = value.get("key").asText();
        accumulator.sum += value.get("value").get("value").asLong();
        accumulator.count++;
        return accumulator;
    }

    @Override
    public Tuple2<String, Double> getResult(AverageAccumulator accumulator) {
        return new Tuple2<>(accumulator.key, accumulator.sum / (double)accumulator.count);
    }

    @Override
    public AverageAccumulator merge(AverageAccumulator a, AverageAccumulator b) {
        a.count += b.count;
        a.sum += b.sum;
        return a;
    }
}

*/