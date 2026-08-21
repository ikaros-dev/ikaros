package run.ikaros.server.custom.scheme;

import java.util.List;
import org.springframework.lang.NonNull;
import run.ikaros.api.custom.scheme.CustomScheme;

public interface CustomSchemeWatcherManager {

    void register(@NonNull SchemeWatcher watcher);

    void unregister(@NonNull SchemeWatcher watcher);

    List<SchemeWatcher> watchers();

    interface SchemeWatcher {

        void onChange(ChangeEvent event);

    }

    interface ChangeEvent {

    }

    class SchemeRegistered implements ChangeEvent {
        private final CustomScheme newScheme;

        public SchemeRegistered(CustomScheme newScheme) {
            this.newScheme = newScheme;
        }

        public CustomScheme getNewScheme() {
            return newScheme;
        }
    }

    class SchemeUnregistered implements ChangeEvent {

        private final CustomScheme deletedScheme;

        public SchemeUnregistered(CustomScheme deletedScheme) {
            this.deletedScheme = deletedScheme;
        }

        public CustomScheme getDeletedScheme() {
            return deletedScheme;
        }

    }
}
