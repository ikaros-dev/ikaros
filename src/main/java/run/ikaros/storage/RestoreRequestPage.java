package run.ikaros.storage;

import java.util.List;

record RestoreRequestPage(List<StorageRestoreRequestView> items, String nextCursor) {
    RestoreRequestPage {
        items = List.copyOf(items);
    }
}
