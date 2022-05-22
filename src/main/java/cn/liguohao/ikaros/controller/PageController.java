package cn.liguohao.ikaros.controller;

import cn.liguohao.ikaros.entity.User;
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

    private final UserService userService;

    public PageController(UserService userService) {
        this.userService = userService;
    }

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
        User user = userService.getCurrentLoginUser();
        if (user != null) {
            model.addAttribute("user", user);
        }
        return "admin/index";
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
