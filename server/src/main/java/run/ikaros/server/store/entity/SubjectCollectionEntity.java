package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.CollectionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject_collection")
@Accessors(chain = true)
public class SubjectCollectionEntity {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("subject_id")
    private Long subjectId;
    /**
     * collection status.
     *
     * @see CollectionType
     */
    private CollectionType type;

    /**
     * User main group episode watching progress.
     */
    @Column("main_ep_progress")
    private Integer mainEpisodeProgress;

    /**
     * Whether it can be accessed without login.
     */
    @Column("is_private")
    private Boolean isPrivate;

    private String comment;

    /**
     * Subject score, from 0 to 10.
     */
    private Integer score;
    private UUID uuid;
}
