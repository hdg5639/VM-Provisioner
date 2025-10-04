package cloud.gamja.vm.config;

import cloud.gamja.vm.vms.record.VmDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableR2dbcAuditing
@RequiredArgsConstructor
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, List.of(
                new VmDetailWritingConverter(objectMapper),
                new VmDetailReadingConverter(objectMapper)
        ));
    }

    @WritingConverter
    static class VmDetailWritingConverter implements Converter<VmDetail, Json> {
        private final ObjectMapper objectMapper;

        public VmDetailWritingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(@NonNull VmDetail source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to convert VmDetail to JSON", e);
            }
        }
    }

    @ReadingConverter
    static class VmDetailReadingConverter implements Converter<Json, VmDetail> {
        private final ObjectMapper objectMapper;

        public VmDetailReadingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public VmDetail convert(Json source) {
            try {
                return objectMapper.readValue(source.asString(), VmDetail.class);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to convert JSON to VmDetail", e);
            }
        }
    }
}