package cloud.gamja.vm.config;

import cloud.gamja.vm.vms.record.VmDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableR2dbcAuditing
@RequiredArgsConstructor
public class R2dbcConfig {

    private final ObjectMapper objectMapper;

    @Bean
    R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.NONE,
                List.of(
                        new VmDetailWriteConverter(objectMapper),
                        new VmDetailReadConverter(objectMapper)
                )
        );
    }

    // VmDetail -> Json (DB 쓰기)
    static class VmDetailWriteConverter implements Converter<VmDetail, Json> {
        private final ObjectMapper om;
        VmDetailWriteConverter(ObjectMapper om) { this.om = om; }

        @Override public Json convert(VmDetail source) {
            try {
                return Json.of(om.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Serialize VmDetail failed", e);
            }
        }
    }

    // Json -> VmDetail (DB 읽기)
    static class VmDetailReadConverter implements Converter<Json, VmDetail> {
        private final ObjectMapper om;
        VmDetailReadConverter(ObjectMapper om) { this.om = om; }

        @Override public VmDetail convert(Json source) {
            try {
                return om.readValue(source.asString(), VmDetail.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Deserialize VmDetail failed", e);
            }
        }
    }
}
