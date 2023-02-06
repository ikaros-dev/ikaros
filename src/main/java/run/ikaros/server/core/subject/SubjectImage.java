package run.ikaros.server.core.subject;

import lombok.Data;

@Data
public class SubjectImage {
    private String large;
    private String common;
    private String medium;
    private String small;
    private String grid;
}
