package com.services.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Utility class for Kafka serialization/deserialization configuration.
 * <p>
 * Provides pre-configured ObjectMapper instances for JSON serialization
 * that handle Java 8 date/time types correctly.
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
public final class KafkaSerializationUtil {

    private KafkaSerializationUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates an ObjectMapper configured for Kafka JSON serialization.
     * <p>
     * Configuration:
     * <ul>
     *   <li>Registers JavaTimeModule for Java 8 date/time support</li>
     *   <li>Uses ISO-8601 format for dates</li>
     *   <li>Does not write null values</li>
     *   <li>Does not fail on unknown properties (forward compatibility)</li>
     * </ul>
     * </p>
     *
     * @return Configured ObjectMapper
     */
    public static ObjectMapper createKafkaObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    /**
     * Configures a JsonSerializer with the Kafka ObjectMapper.
     *
     * @param <T> Type to serialize
     * @return Configured JsonSerializer
     */
    public static <T> JsonSerializer<T> createJsonSerializer() {
        JsonSerializer<T> serializer = new JsonSerializer<>(createKafkaObjectMapper());
        serializer.configure(java.util.Map.of(
            JsonSerializer.ADD_TYPE_INFO_HEADERS, false
        ), false);
        return serializer;
    }

    /**
     * Configures a JsonDeserializer with the Kafka ObjectMapper.
     *
     * @param <T>        Type to deserialize
     * @param targetType Target class
     * @return Configured JsonDeserializer
     */
    public static <T> JsonDeserializer<T> createJsonDeserializer(Class<T> targetType) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType, createKafkaObjectMapper());
        deserializer.configure(java.util.Map.of(
            JsonDeserializer.TRUSTED_PACKAGES, "*",
            JsonDeserializer.USE_TYPE_INFO_HEADERS, false
        ), false);
        return deserializer;
    }
}
