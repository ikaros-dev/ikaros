package run.ikaros.api.infra.utils;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.api.infra.exception.RegexMatchingException;

class RegexUtilsTest {


    private static final Map<Double, String> seqNameMap = new HashMap<>();

    static {
        seqNameMap.put(3D,
            "[ANi] Reign of the Seven Spellblades "
                + "- 七魔剑支配天下 - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]");
        seqNameMap.put(5D,
            "[桜都字幕组] 堀与宫村 -piece- / Horimiya Piece [05][1080p][简体内嵌]");
        seqNameMap.put(34D,
            "[桜都字幕组] 莱莎的炼金工房 ～常暗女王与秘密藏身处～ /"
                + " Ryza no Atelier：Tokoyami no Joou to Himitsu no Kakurega [34][1080p][简体内嵌]");
        seqNameMap.put(134D,
            "[GM-Team][Dou Luo Da Lu][Douro Mainland][2019][134][AVC][GB][1080P].mp4");
        seqNameMap.put(249D,
            "[GM-Team][Dou Luo Da Lu][Douro Mainland][2019][249][AVC][GB][1080P].mp4");
        seqNameMap.put(6D,
            "爱神巧克力[06].mp4");
        seqNameMap.put(15D, "[ANK-Raws&VCB-Studio] Aldnoah Zero [15][Ma10p_1080p][x265_flac]");
        seqNameMap.put(15.5D, "[ANK-Raws&VCB-Studio] Aldnoah Zero [15.5][Ma10p_1080p][x265_flac]");
        seqNameMap.put(15.8D, "[ANK-Raws&VCB-Studio] Aldnoah Zero [15.8][Ma10p_1080p][x265_flac]");
        seqNameMap.put(158D, "[ANK-Raws&VCB-Studio] Aldnoah Zero [158][Ma10p_1080p][x265_flac]");
        seqNameMap.put(158.5D,
            "[ANK-Raws&VCB-Studio] Aldnoah Zero [158.5][Ma10p_1080p][x265_flac]");
    }


    @Test
    void parseEpisodeSeqByFileName() {
        for (Map.Entry<Double, String> entry : seqNameMap.entrySet()) {
            Double seq = RegexUtils.parseEpisodeSeqByFileName(entry.getValue());
            assertThat(seq).isEqualTo(entry.getKey());
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
            assertThat(e).isNotNull();
            assertThat(e.getMessage())
                .contains("file name tag episode seq matching exception");
        }
    }

    @Test
    void getEpFileNameIntegrallySeq() {
        String str =
            "Clannad 2007 EP01 [BluRay 1920x1080p 23.976fps x264-Hi10P FLACx2] - mawen1250.mkv";

        Double seq = RegexUtils.getEpFileNameIntegrallySeq(str);
        assertThat(seq).isEqualTo(1);
    }
}