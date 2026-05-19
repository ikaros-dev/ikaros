package run.ikaros.server.core.subject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.api.store.enums.SubjectType;

class SubjectRelationCourtTest {

    @Test
    void judgeBeforeReturnsAfter() {
        assertThat(SubjectRelationCourt.judge(SubjectType.ANIME, SubjectRelationType.BEFORE))
            .isEqualTo(SubjectRelationType.AFTER);
    }

    @Test
    void judgeAfterReturnsBefore() {
        assertThat(SubjectRelationCourt.judge(SubjectType.ANIME, SubjectRelationType.AFTER))
            .isEqualTo(SubjectRelationType.BEFORE);
    }

    @Test
    void judgeSameWorldviewReturnsSameWorldview() {
        assertThat(SubjectRelationCourt.judge(SubjectType.ANIME, SubjectRelationType.SAME_WORLDVIEW))
            .isEqualTo(SubjectRelationType.SAME_WORLDVIEW);
    }

    @Test
    void judgeAnimeReturnsAnime() {
        assertThat(SubjectRelationCourt.judge(SubjectType.ANIME, SubjectRelationType.ANIME))
            .isEqualTo(SubjectRelationType.ANIME);
    }

    @Test
    void judgeComicReturnsComic() {
        assertThat(SubjectRelationCourt.judge(SubjectType.COMIC, SubjectRelationType.COMIC))
            .isEqualTo(SubjectRelationType.COMIC);
    }

    @Test
    void judgeGameReturnsGame() {
        assertThat(SubjectRelationCourt.judge(SubjectType.GAME, SubjectRelationType.GAME))
            .isEqualTo(SubjectRelationType.GAME);
    }

    @Test
    void judgeMusicReturnsMusic() {
        assertThat(SubjectRelationCourt.judge(SubjectType.MUSIC, SubjectRelationType.MUSIC))
            .isEqualTo(SubjectRelationType.MUSIC);
    }

    @Test
    void judgeNovelReturnsNovel() {
        assertThat(SubjectRelationCourt.judge(SubjectType.NOVEL, SubjectRelationType.NOVEL))
            .isEqualTo(SubjectRelationType.NOVEL);
    }

    @Test
    void judgeRealReturnsReal() {
        assertThat(SubjectRelationCourt.judge(SubjectType.REAL, SubjectRelationType.REAL))
            .isEqualTo(SubjectRelationType.REAL);
    }

    @Test
    void judgeOtherReturnsOther() {
        assertThat(SubjectRelationCourt.judge(SubjectType.OTHER, SubjectRelationType.OTHER))
            .isEqualTo(SubjectRelationType.OTHER);
    }

    @Test
    void judgeAnimeWithTypeOtherReturnsAnime() {
        assertThat(SubjectRelationCourt.judge(SubjectType.ANIME, SubjectRelationType.OTHER))
            .isEqualTo(SubjectRelationType.ANIME);
    }

    @Test
    void judgeComicWithTypeBeforeReturnsAfter() {
        assertThat(SubjectRelationCourt.judge(SubjectType.COMIC, SubjectRelationType.BEFORE))
            .isEqualTo(SubjectRelationType.AFTER);
    }
}
