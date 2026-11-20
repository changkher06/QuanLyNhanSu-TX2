package api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import repository.InMemoryNhanSuRepository;
import repository.NhanSuRepository;
import service.NgayCongService;
import service.NhanVienService;
import service.TimKiemNhanVienService;

@Configuration
public class ApiConfiguration {
    static final ZoneId DATE_ZONE = ZoneId.of("Asia/Bangkok");

    @Bean NhanSuRepository repository() { return new InMemoryNhanSuRepository(); }
    @Bean NhanVienService employees(NhanSuRepository repo) { return new NhanVienService(repo); }
    @Bean NgayCongService workDays(NhanSuRepository repo) { return new NgayCongService(repo); }
    @Bean TimKiemNhanVienService search(NhanSuRepository repo) { return new TimKiemNhanVienService(repo); }
    @Bean Jackson2ObjectMapperBuilderCustomizer jsonContract() {
        return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Textual)
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
