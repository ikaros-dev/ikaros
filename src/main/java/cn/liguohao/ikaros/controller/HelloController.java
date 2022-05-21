package cn.liguohao.ikaros.controller;

import cn.liguohao.ikaros.pojo.HelloIkaros;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

/**
 * @author li-guohao
 */
@Controller
public class HelloController {

    @GetMapping("/hello")
    @ResponseBody
    public Mono<HelloIkaros> hello() {
        return Mono.create(
            sink -> sink.success(HelloIkaros.ofMsg()));
    }

}
