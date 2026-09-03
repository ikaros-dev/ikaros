package run.ikaros.storage;

import java.util.List;

public record RestoreRequestContractListView(List<RestoreRequestContractView> items, String nextCursor) {
    public RestoreRequestContractListView { items = List.copyOf(items == null ? List.of() : items); }
}
