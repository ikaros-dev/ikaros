package run.ikaros.server.custom;

import java.time.LocalDateTime;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;

@Custom(group = DemoCustom.GROUP, version = DemoCustom.VERSION, kind = DemoCustom.KIND,
    singular = "demo", plural = "demos")
public class DemoCustom {
    public static final String GROUP = "demo.ikaros.run";
    public static final String VERSION = "v1alpha1";
    public static final String KIND = "DemoExtension";

    @Name
    private String title;

    private LocalDateTime time;

    private Long number;

    private Boolean flag;

    private byte head;
    private byte[] headerMap;
    private User user;

    static class User {
        private String username;

        public String getUsername() {
            return username;
        }

        public User setUsername(String username) {
            this.username = username;
            return this;
        }
    }

    public String getTitle() {
        return title;
    }

    public DemoCustom setTitle(String title) {
        this.title = title;
        return this;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public DemoCustom setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public Long getNumber() {
        return number;
    }

    public DemoCustom setNumber(Long number) {
        this.number = number;
        return this;
    }

    public Boolean getFlag() {
        return flag;
    }

    public DemoCustom setFlag(Boolean flag) {
        this.flag = flag;
        return this;
    }

    public byte getHead() {
        return head;
    }

    public DemoCustom setHead(byte head) {
        this.head = head;
        return this;
    }

    public byte[] getHeaderMap() {
        return headerMap;
    }

    public DemoCustom setHeaderMap(byte[] headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public User getUser() {
        return user;
    }

    public DemoCustom setUser(User user) {
        this.user = user;
        return this;
    }
}
