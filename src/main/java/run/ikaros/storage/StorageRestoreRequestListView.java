package run.ikaros.storage;

import java.util.List;

public record StorageRestoreRequestListView(List<StorageRestoreRequestView> items, String nextCursor) {
    public StorageRestoreRequestListView { items = List.copyOf(items == null ? List.of() : items); }
}
