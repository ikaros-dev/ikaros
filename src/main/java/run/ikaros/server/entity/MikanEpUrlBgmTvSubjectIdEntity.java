package run.ikaros.server.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "mikan_ep_url_bgmtv_subject_id")
public class MikanEpUrlBgmTvSubjectIdEntity extends BaseEntity {
    @Column(name = "mikan_episode_url", nullable = false)
    private String mikanEpisodeUrl;
    @Column(name = "bgmtv_subject_id", nullable = false)
    private Long bgmtvSubjectId;

    public MikanEpUrlBgmTvSubjectIdEntity() {
    }

    public MikanEpUrlBgmTvSubjectIdEntity(String mikanEpisodeUrl, Long bgmtvSubjectId) {
        this.mikanEpisodeUrl = mikanEpisodeUrl;
        this.bgmtvSubjectId = bgmtvSubjectId;
    }

    public String getMikanEpisodeUrl() {
        return mikanEpisodeUrl;
    }

    public void setMikanEpisodeUrl(String mikanEpisodeUrl) {
        this.mikanEpisodeUrl = mikanEpisodeUrl;
    }

    public Long getBgmtvSubjectId() {
        return bgmtvSubjectId;
    }

    public void setBgmtvSubjectId(Long bgmtvSubjectId) {
        this.bgmtvSubjectId = bgmtvSubjectId;
    }
}
