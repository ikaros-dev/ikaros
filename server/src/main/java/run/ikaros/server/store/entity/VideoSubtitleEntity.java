package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "video_subtitle")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class VideoSubtitleEntity {

    @Id
    private Long id;

    @Column("video_file_id")
    private Long videoFileId;

    @Column("subtitle_file_id")
    private Long subtitleFileId;
}
