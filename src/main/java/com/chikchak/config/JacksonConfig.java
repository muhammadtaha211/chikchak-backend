package com.chikchak.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // DateTime: always serialize with 'Z' suffix for FlutterFlow
        JavaTimeModule timeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        timeModule.addSerializer(java.time.LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        mapper.registerModule(timeModule);

        // Don't write dates as numeric timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // BigDecimal: write 20.00 not 20 or 2E+1
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        // ALWAYS include all properties, even if null
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // CRITICAL: Replace null values with safe defaults for FlutterFlow
        SimpleModule nullSafeModule = new SimpleModule("NullSafeModule");
        nullSafeModule.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(
                    SerializationConfig config,
                    BeanDescription beanDesc,
                    List<BeanPropertyWriter> beanProperties) {

                for (int i = 0; i < beanProperties.size(); i++) {
                    BeanPropertyWriter writer = beanProperties.get(i);
                    JavaType type = writer.getType();

                    if (type.isTypeOrSubTypeOf(String.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, ""));
                    } else if (type.isTypeOrSubTypeOf(Integer.class) || type.isTypeOrSubTypeOf(int.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, 0));
                    } else if (type.isTypeOrSubTypeOf(Long.class) || type.isTypeOrSubTypeOf(long.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, 0L));
                    } else if (type.isTypeOrSubTypeOf(Double.class) || type.isTypeOrSubTypeOf(double.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, 0.0));
                    } else if (type.isTypeOrSubTypeOf(Boolean.class) || type.isTypeOrSubTypeOf(boolean.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, false));
                    } else if (type.isTypeOrSubTypeOf(java.math.BigDecimal.class)) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, java.math.BigDecimal.ZERO));
                    } else if (type.isCollectionLikeType()) {
                        beanProperties.set(i, new NullSafePropertyWriter(writer, Collections.emptyList()));
                    }
                    // For complex objects (nested DTOs, LocalDateTime), null stays null
                    // FlutterFlow handles missing nested objects better than null primitives
                }
                return beanProperties;
            }
        });
        mapper.registerModule(nullSafeModule);

        return mapper;
    }

    /**
     * Custom property writer that replaces null values with a safe default.
     * This prevents FlutterFlow from choking on null Strings, Doubles, etc.
     */
    private static class NullSafePropertyWriter extends BeanPropertyWriter {
        private final Object defaultValue;

        public NullSafePropertyWriter(BeanPropertyWriter base, Object defaultValue) {
            super(base);
            this.defaultValue = defaultValue;
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
                throws Exception {
            Object value = get(bean);
            if (value == null) {
                gen.writeFieldName(getName());
                gen.writeObject(defaultValue);
            } else {
                super.serializeAsField(bean, gen, prov);
            }
        }
    }
}