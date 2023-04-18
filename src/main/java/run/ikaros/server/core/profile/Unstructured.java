package run.ikaros.server.core.profile;

import java.util.HashMap;
import java.util.Map;

public class Unstructured {

    private final Map data;

    public Unstructured() {
        this(new HashMap());
    }

    public Unstructured(Map data) {
        this.data = data;
    }

    public Map getData() {
        return data;
    }
}
