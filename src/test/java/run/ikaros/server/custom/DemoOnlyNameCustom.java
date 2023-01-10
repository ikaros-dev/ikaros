package run.ikaros.server.custom;

@Custom(group = "demo.ikaros.run", version = "v1alpha1",
    kind = "DemoOnlyNameCustom")
public class DemoOnlyNameCustom {
    @Name
    private String title;

    public String getTitle() {
        return title;
    }

    public DemoOnlyNameCustom setTitle(String title) {
        this.title = title;
        return this;
    }
}
