package ahqpck.maintenance.report.config;

import ahqpck.maintenance.report.mapper.PartMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

    @Bean
    public PartMapper partMapper() {
        return Mappers.getMapper(PartMapper.class);
    }
}
