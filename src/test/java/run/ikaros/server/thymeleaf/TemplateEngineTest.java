package run.ikaros.server.thymeleaf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import run.ikaros.server.constants.TemplateConst;
import run.ikaros.server.utils.StringUtils;

import java.util.HashMap;

/**
 * @author <a href="http://liguohao.cn" target="_blank">liguohao</a>
 * @date 2022/4/6
 */
@SpringBootTest
public class TemplateEngineTest {

    @Autowired
    TemplateEngine templateEngine;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.profiles.active", "local");
    }

    @Test
    public void animeUpdateTemplateRendering() {
        System.out.println(buildAnimeUpdateHtml());
    }

    public String buildAnimeUpdateHtml() {
        final var template = TemplateConst.MAIL_ANIME_UPDATE;

        final var context = new Context();
        var vars = new HashMap<String, Object>();
        vars.put("title", "孤独摇滚！");
        vars.put("epTitle", "孤独摇滚！");
        vars.put("epSeq", "12");
        vars.put("epIntroduction", """
            極度の人見知りで陰キャな少女、後藤ひとり。バンド活動に憧れギターを始めるも友達が出来ず一人で
            練習する毎日。ある日“結束バンド”というバンドでドラムをやっている伊地知虹夏に声をかけられ１日だけサポートギターをすることに……
                        
            脚本：吉田恵里香
            絵コンテ：斎藤圭一郎
            演出：斎藤圭一郎
            作画監督：けろりら""");
        vars.put("introduction", """
            作为网络吉他手“吉他英雄”而广受好评的后藤一里，在现实中却是个什么都不会的沟通障碍者。
            一里有着组建乐队的梦想，但因为不敢向人主动搭话而一直没有成功，
            直到一天在公园中被伊地知虹夏发现并邀请进入缺少吉他手的“结束乐队”。可是，完全没有和他人合作经历的一里，
            在人前完全发挥不出原本的实力。为了努力克服沟通障碍，一里与“结束乐队”的成员们一同开始努力……""");
        vars.put("coverImgUrl", "http://192.168.2.229:9090/upload/2022/12/5/8/51e966853f2f4729a83efe520a1376f4.jpg");
        vars.put("epUrlFileName",
            "[Sakurato] Bocchi the Rock! [10][HEVC-10bit 1080p AAC][CHS&CHT].mkv");
        context.setVariables(vars);

        return templateEngine.process(template, context);
    }

}
