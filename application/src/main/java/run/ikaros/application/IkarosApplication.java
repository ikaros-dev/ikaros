package run.ikaros.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ikaros 应用启动入口，承载全部模块化单体能力。
 */
@SpringBootApplication(scanBasePackages = "run.ikaros")
@EnableR2dbcRepositories(basePackages = {
    "run.ikaros.activity",
    "run.ikaros.authentication",
    "run.ikaros.authorization",
    "run.ikaros.backup",
    "run.ikaros.collection",
    "run.ikaros.document",
    "run.ikaros.drive",
    "run.ikaros.event",
    "run.ikaros.finance",
    "run.ikaros.game",
    "run.ikaros.ingestion",
    "run.ikaros.media",
    "run.ikaros.metadata",
    "run.ikaros.music",
    "run.ikaros.notes",
    "run.ikaros.offline",
    "run.ikaros.operations",
    "run.ikaros.password",
    "run.ikaros.photo",
    "run.ikaros.planning",
    "run.ikaros.plugin",
    "run.ikaros.progress",
    "run.ikaros.reading",
    "run.ikaros.relation",
    "run.ikaros.resource",
    "run.ikaros.search",
    "run.ikaros.sharing",
    "run.ikaros.storage",
    "run.ikaros.sync"
})
@EnableScheduling
public class IkarosApplication {

    /**
     * 启动 Ikaros 服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IkarosApplication.class, args);
    }
}
