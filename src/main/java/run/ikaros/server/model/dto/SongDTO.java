package run.ikaros.server.model.dto;

import java.util.Date;
import java.util.Map;

public class SongDTO {

    private String url;
    private String name;
    private Map<String, String> metadata;
    private Long menuId;
    private Long albumId;
    private String uploadUser;
    private Date uploadTime;

    public String getName() {
        return name;
    }

    public SongDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public SongDTO setUrl(String url) {
        this.url = url;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public SongDTO setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Long getMenuId() {
        return menuId;
    }

    public SongDTO setMenuId(Long menuId) {
        this.menuId = menuId;
        return this;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public SongDTO setAlbumId(Long albumId) {
        this.albumId = albumId;
        return this;
    }

    public String getUploadUser() {
        return uploadUser;
    }

    public SongDTO setUploadUser(String uploadUser) {
        this.uploadUser = uploadUser;
        return this;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public SongDTO setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
        return this;
    }
}
