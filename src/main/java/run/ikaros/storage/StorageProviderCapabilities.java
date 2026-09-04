package run.ikaros.storage;

import java.util.Map;

/** Normalized Provider capabilities; raw provider metadata stays provider-owned. */
public record StorageProviderCapabilities(boolean archiveRestore, boolean temporaryRestoreWindow,
    boolean restoreWindowExtension, boolean serverSideCopy, boolean rangeRead,
    boolean objectLock, boolean retentionLock, boolean versioning, boolean lifecycleIntrospection,
    String restoreUnit) {
    public static StorageProviderCapabilities from(StorageProvider provider) {
        Map<String, Object> m = provider.metadata();
        return new StorageProviderCapabilities(bool(m, "supports_archive_restore"),
            bool(m, "supports_temporary_restore_window"), bool(m, "supports_restore_window_extension"),
            bool(m, "supports_server_side_copy"), bool(m, "supports_range_read"), bool(m, "supports_object_lock"),
            bool(m, "supports_retention_lock"), bool(m, "supports_versioning"),
            bool(m, "supports_lifecycle_introspection"), String.valueOf(m.getOrDefault("restore_unit", "PROVIDER_DEFINED")));
    }

    private static boolean bool(Map<String, Object> metadata, String key) {
        return Boolean.TRUE.equals(metadata.get(key));
    }
}
