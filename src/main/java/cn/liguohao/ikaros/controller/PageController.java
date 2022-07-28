package cn.liguohao.ikaros.controller;

import cn.liguohao.ikaros.persistence.structural.entity.UserEntity;
import cn.liguohao.ikaros.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author <a href="http://liguohao.cn" target="_blank">liguohao</a>
 * @date 2022/4/3
 */
@Controller
@RequestMapping("/page")
public class PageController {

    @GetMapping
    public String home() {
        return "index";
    }

    /**
     * @param model mvc数据模型
     * @return 模板渲染地址
     */
    @GetMapping("/manager")
    public String manager(Model model) {
        UserEntity userEntity = UserService.getCurrentLoginUser();
        if (userEntity == null) {
            model.addAttribute("loginError", "用户名密码错误");
            return "login";
        } else {
            model.addAttribute("user", userEntity);
            return "admin/index";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("loginError", true);
        return login();
    }

}
