package run.ikaros.server.extension;

/**
 * @author: li-guohao
 */
@Extension(group = "demo.ikaros.run", version = "v1alpha1", kind = "DemoExtension")
public class DemoExtension {

    @Name
    private String title;

    private String other;
}
