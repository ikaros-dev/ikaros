package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author li-guohao
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskConfig.class);

    @Scheduled(cron = "0 */30 * * * ?")
    public void execScanRSSUpdate() {

    }

}
