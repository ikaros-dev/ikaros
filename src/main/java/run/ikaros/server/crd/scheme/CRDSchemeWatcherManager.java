package run.ikaros.server.crd.scheme;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @author: li-guohao
 */
public interface CRDSchemeWatcherManager {
    interface CRDSchemeWatcher {
        void onChange(CRDSchemeChangeEvent event);
    }

    void register(@NotNull CRDSchemeWatcher watcher);

    void unregister(@NotNull CRDSchemeWatcher watcher);

    @NotNull
    List<CRDSchemeWatcher> watchers();

    interface CRDSchemeChangeEvent {
        enum Action {
            REGISTER,
            UNREGISTER
        }

        Action getAction();

        CRDScheme getChangedScheme();
    }

    class RegisteredCRDSchemeEvent implements CRDSchemeChangeEvent {
        private final CRDScheme scheme;

        public RegisteredCRDSchemeEvent(CRDScheme scheme) {
            this.scheme = scheme;
        }

        @Override
        public Action getAction() {
            return Action.REGISTER;
        }

        @Override
        public CRDScheme getChangedScheme() {
            return scheme;
        }
    }

    class UnregisteredCRDSchemeEvent implements CRDSchemeChangeEvent {
        private final CRDScheme scheme;

        public UnregisteredCRDSchemeEvent(CRDScheme scheme) {
            this.scheme = scheme;
        }

        @Override
        public Action getAction() {
            return Action.UNREGISTER;
        }

        @Override
        public CRDScheme getChangedScheme() {
            return scheme;
        }
    }

}
