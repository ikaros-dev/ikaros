package run.ikaros.server.core.plugin;

import static run.ikaros.server.plugin.listener.PluginDatabaseUtils.getPluginByDescriptor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.plugin.PluginInstallException;
import run.ikaros.api.infra.exception.plugin.PluginUpgradeException;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.plugin.IkarosPluginManager;

@Slf4j
@Service
public class PluginServiceImpl implements PluginService {
    private final IkarosPluginManager pluginManager;
    private final ReactiveCustomClient customClient;

    public PluginServiceImpl(IkarosPluginManager pluginManager, ReactiveCustomClient customClient) {
        this.pluginManager = pluginManager;
        this.customClient = customClient;
    }

    @Override
    public Mono<PluginState> operateState(@NotBlank String pluginId,
                                          @NotNull PluginStateOperate operate) {
        Assert.hasText(pluginId, "'pluginId' must has text.");
        Assert.notNull(operate, "'operate' must not null.");
        switch (operate) {
            case LOAD -> pluginManager.loadPlugin(pluginId);
            case LOAD_ALL -> pluginManager.loadPlugins();
            case RELOAD -> pluginManager.reloadPlugin(pluginId);
            case RELOAD_ALL -> pluginManager.reloadPlugins();
            case RELOAD_ALL_STARTED -> pluginManager.reloadStartedPlugins();
            case UNLOAD -> pluginManager.unloadPlugin(pluginId);
            case ENABLE -> pluginManager.enablePlugin(pluginId);
            case DISABLE -> pluginManager.disablePlugin(pluginId);
            case START -> pluginManager.startPlugin(pluginId);
            case STOP -> pluginManager.stopPlugin(pluginId);
            case DELETE -> pluginManager.deletePlugin(pluginId);
            default -> throw new PluginRuntimeException("No support operate for id(name): "
                + pluginId);
        }
        if (pluginManager.getPlugins().isEmpty() || StringUtils.isBlank(pluginId)
            || "ALL".equalsIgnoreCase(pluginId)) {
            log.warn("Skip get plugin state operate. pluginId: [{}], manager plugins: [{}]",
                pluginId, pluginManager.getPlugins());
            return Mono.empty();
        }
        return Mono.justOrEmpty(pluginManager.getPlugin(pluginId))
            .map(PluginWrapper::getPluginState);
    }

    @Override
    public Mono<PluginState> start(@NotBlank String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.startPlugin(pluginId);
        return Mono.just(pluginState);
    }

    @Override
    public Mono<PluginState> stop(@NotBlank String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.stopPlugin(pluginId);
        return Mono.just(pluginState);
    }

    @Override
    public Mono<PluginState> reload(String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text");
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        if (pluginWrapper == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        PluginState pluginState = pluginManager.reloadPlugin(pluginId);
        return Mono.just(Objects.requireNonNull(pluginState));
    }

    @Override
    public Mono<Void> install(@NotNull FilePart filePart) {
        Assert.notNull(filePart, "'filePart' must not null.");
        String pluginDir = System.getProperty("pf4j.pluginsDir");
        try {
            Path pluginDirPath = new File(pluginDir).toPath().normalize();
            if (Files.notExists(pluginDirPath)) {
                Files.createDirectories(pluginDirPath);
            }

            // Sanitize filename: strip directory components to prevent path traversal
            String filename = filePart.filename();
            if (StringUtils.isBlank(filename)) {
                return Mono.error(new PluginInstallException("Plugin filename must not be empty."));
            }
            String safeFilename = Path.of(filename).normalize().getFileName().toString();
            if (StringUtils.isBlank(safeFilename)) {
                return Mono.error(new PluginInstallException("Invalid plugin filename."));
            }

            Path destPath = pluginDirPath.resolve(safeFilename).normalize();

            // Zip Slip prevention: ensure resolved path stays within plugin directory
            if (!destPath.startsWith(pluginDirPath)) {
                return Mono.error(new PluginInstallException(
                    "Path traversal detected in plugin filename: " + filename));
            }

            return filePart.transferTo(destPath.toFile())
                .doOnSuccess(unused -> log.debug("Upload plugin file [{}] to plugin dir [{}].",
                    safeFilename, destPath))
                .then(Mono.fromCallable(() -> pluginManager.loadPlugin(destPath)))
                .doOnSuccess(pluginId ->
                    log.debug("Load plugin by path success, pluginId: [{}].", pluginId))
                .doOnError(error -> deleteFailedPluginFile(destPath))
                .then();
        } catch (Exception e) {
            throw new PluginInstallException(e);
        }
    }

    private void deleteFailedPluginFile(Path pluginPath) {
        try {
            if (Files.deleteIfExists(pluginPath)) {
                log.debug("Delete invalid plugin file for path: {}", pluginPath);
            }
        } catch (IOException e) {
            log.warn("Delete invalid plugin file failed for path: {}", pluginPath, e);
        }
    }

    @Override
    public Mono<Void> upgrade(String pluginId, FilePart filePart) {
        Assert.hasText(pluginId, "'pluginId' must has text.");
        Assert.notNull(filePart, "'filePart' must not null.");

        PluginWrapper oldPlugin = pluginManager.getPlugin(pluginId);
        if (oldPlugin == null) {
            return Mono.error(new NotFoundException("Not found plugin for id: " + pluginId));
        }
        pluginManager.validatePlugin(pluginId);
        Path oldPath = oldPlugin.getPluginPath();
        PluginState oldState = oldPlugin.getPluginState();
        Path backupPath = backupPlugin(oldPath);
        try {
            if (!pluginManager.unloadPlugin(pluginId)) {
                throw new PluginUpgradeException("Unload old plugin [%s] failed.", pluginId);
            }
            Files.delete(oldPath);
            log.debug("delete old plugin for path: {}", oldPath);
        } catch (RuntimeException | IOException e) {
            try {
                restorePlugin(pluginId, oldPath, backupPath, oldState);
            } finally {
                deleteUpgradeBackup(backupPath);
            }
            throw e instanceof PluginUpgradeException pluginUpgradeException
                ? pluginUpgradeException : new PluginUpgradeException(e);
        }
        return install(filePart)
            .then(Mono.just(pluginManager))
            .map(ikarosPluginManager -> ikarosPluginManager.getPlugin(pluginId))
            .map(pluginWrapper -> getPluginByDescriptor(pluginId, pluginManager, pluginWrapper))
            .flatMap(customClient::update)
            .then()
            .onErrorResume(error -> restorePluginAfterUpgradeFailure(pluginId, oldPath,
                backupPath, oldState).then(Mono.error(error)))
            .doFinally(signalType -> deleteUpgradeBackup(backupPath));
    }

    private Mono<Void> restorePluginAfterUpgradeFailure(String pluginId, Path oldPath,
                                                        Path backupPath,
                                                        PluginState oldState) {
        return Mono.fromRunnable(() -> restorePlugin(pluginId, oldPath, backupPath, oldState));
    }

    private Path backupPlugin(Path oldPath) {
        Path backupPath = null;
        try {
            backupPath = Files.createTempFile(oldPath.getParent(),
                oldPath.getFileName().toString() + ".", ".upgrade-backup");
            Files.copy(oldPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return backupPath;
        } catch (IOException e) {
            if (backupPath != null) {
                deleteUpgradeBackup(backupPath);
            }
            throw new PluginUpgradeException(e);
        }
    }

    private void restorePlugin(String pluginId, Path oldPath, Path backupPath,
                               PluginState oldState) {
        PluginWrapper newPlugin = pluginManager.getPlugin(pluginId);
        if (newPlugin != null) {
            Path newPath = newPlugin.getPluginPath();
            pluginManager.unloadPlugin(pluginId);
            if (!newPath.equals(oldPath)) {
                deleteFailedPluginFile(newPath);
            }
        }
        try {
            Files.copy(backupPath, oldPath, StandardCopyOption.REPLACE_EXISTING);
            pluginManager.loadPlugin(oldPath);
            if (PluginState.STARTED == oldState) {
                pluginManager.startPlugin(pluginId);
            }
            log.info("Restore plugin [{}] after upgrade failure.", pluginId);
        } catch (IOException e) {
            throw new PluginUpgradeException(e,
                "Restore plugin [%s] after upgrade failure failed.", pluginId);
        }
    }

    private void deleteUpgradeBackup(Path backupPath) {
        try {
            Files.deleteIfExists(backupPath);
        } catch (IOException e) {
            log.warn("Delete plugin upgrade backup failed for path: {}", backupPath, e);
        }
    }
}
