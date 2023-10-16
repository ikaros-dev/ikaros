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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "episode_file")
@Accessors(chain = true)
public class EpisodeFileEntity {
    @Id
    private Long id;
    @Column("episode_id")
    private Long episodeId;
    @Column("file_id")
    private Long fileId;
}
