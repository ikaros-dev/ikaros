package run.ikaros.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import run.ikaros.server.dev.DevSubjectRecordsInitializer;
import run.ikaros.server.store.repository.SubjectRepository;

@Profile("dev")
@Configuration(proxyBeanMethods = false)
public class DevConfiguration {

    // @Bean
    DevSubjectRecordsInitializer devSubjectRecordsInitializer(SubjectRepository repository) {
        return new DevSubjectRecordsInitializer(repository);
    }
}
