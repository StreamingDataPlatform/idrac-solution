package io.pravega.connectors.flink.serialization;

import io.pravega.client.stream.impl.JavaSerializer;
import org.apache.flink.streaming.util.serialization.SerializationSchema;

import java.io.Serializable;

/**
 * @deprecated use Json serializer instead
 */
@Deprecated
public class JavaSerializationSchema<T extends Serializable> implements SerializationSchema<T> {
    @Override
    public byte[] serialize(T element) {
        return new JavaSerializer<>().serialize(element).array();
    }
}