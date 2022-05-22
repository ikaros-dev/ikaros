package cn.liguohao.ikaros.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import cn.liguohao.ikaros.handler.HelloHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author li-guohao
 */
@Configuration(proxyBeanMethods = false)
public class HelloRouter {

    private final HelloHandler helloHandler;


    public HelloRouter(HelloHandler helloHandler) {
        this.helloHandler = helloHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> demoRouter() {
        return route(GET("/hello"), helloHandler::hello);
    }
}
