package run.ikaros.api.infra.utils;


import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.api.infra.exception.RegexMatchingException;

class RegexUtilsTest {


    private static final Map<Long, String> seqNameMap = new HashMap<>();

    static {
        seqNameMap.put(3L,
            "[ANi] Reign of the Seven Spellblades "
                + "- 七魔剑支配天下 - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]");
        seqNameMap.put(5L,
            "[桜都字幕组] 堀与宫村 -piece- / Horimiya Piece [05][1080p][简体内嵌]");
        seqNameMap.put(34L,
            "[桜都字幕组] 莱莎的炼金工房 ～常暗女王与秘密藏身处～ /"
                + " Ryza no Atelier：Tokoyami no Joou to Himitsu no Kakurega [34][1080p][简体内嵌]");
    }


    @Test
    void parseEpisodeSeqByFileName() {
        for (Map.Entry<Long, String> entry : seqNameMap.entrySet()) {
            Long seq = RegexUtils.parseEpisodeSeqByFileName(entry.getValue());
            Assertions.assertThat(seq).isEqualTo(entry.getKey());
        }
    }

    @Test
    void getFileNameTagEpSeq() {
        String str =
            "[喵萌奶茶屋&LoliHouse] 可爱过头大危机 /"
                +
                " Kawaisugi Crisis [01-12 精校合集][WebRip 1080p HEVC-10bit AAC][简繁内封字幕][Fin]";
        try {
            RegexUtils.getFileNameTagEpSeq(str);
            Assertions.fail("NotRunCurrentLine");
        } catch (RegexMatchingException e) {
            Assertions.assertThat(e).isNotNull();
            Assertions.assertThat(e.getMessage())
                .contains("file name tag episode seq matching exception");
        }
    }
}