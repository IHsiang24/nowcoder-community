package com.xiangkai.community.controller;

import com.google.code.kaptcha.Producer;
import com.xiangkai.community.constant.CommunityConstant;
import com.xiangkai.community.model.dto.UserLoginInfo;
import com.xiangkai.community.model.entity.User;
import com.xiangkai.community.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer producer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getloginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, String> map = userService.register(user);
        if (CollectionUtils.isEmpty(map)) {
            model.addAttribute("msg", "账号注册成功，我们已经向您发送激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }
        model.addAttribute("usernameMsg", map.get("usernameMsg"));
        model.addAttribute("passwordMsg", map.get("passwordMsg"));
        model.addAttribute("emailMsg", map.get("emailMsg"));

        return "/site/register";
    }

    @RequestMapping(path = "/activation/{userId}/{code}")
    public String activation(Model model,
                             @PathVariable("userId") int userId,
                             @PathVariable("code") String code) {

        Integer activationResult = userService.activation(userId, code);

        if (activationResult.equals(ACTIVATION_SUCCESS)) {
            model.addAttribute("msg", "账号激活成功，您可以正常使用账号！");
            model.addAttribute("target", "/login");
        } else if (activationResult.equals(ACTIVATION_FAIL)) {
            model.addAttribute("msg", "账号激活失败，请联系管理员处理！");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "账号已激活过，请勿重复激活！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void kaptcha(HttpServletRequest request, HttpServletResponse response) {

        // 获取验证码并存入会话session中
        String kaptchaCode = producer.createText();
        HttpSession session = request.getSession();
        session.setAttribute("kaptchaCode", kaptchaCode);

        // 输出验证码图片至浏览器响应
        BufferedImage image = producer.createImage(kaptchaCode);
        response.setContentType("image/png");
        try (ServletOutputStream os = response.getOutputStream()) {
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            LOGGER.error("获取验证码出错：" + e);
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(Model model,
                        UserLoginInfo userLoginInfo,
                        HttpServletRequest request, HttpServletResponse response) {

        // 校验验证码
        HttpSession session = request.getSession();
        Object kaptchaCode = session.getAttribute("kaptchaCode");
        if (!userLoginInfo.getVerifyCode().equalsIgnoreCase((String) kaptchaCode)) {
            model.addAttribute("verifyCodeMsg", "验证码不正确！");
            return "/site/login";
        }

        if (!Objects.isNull(userLoginInfo.getRememberMe()) && userLoginInfo.getRememberMe()) {
            userLoginInfo.setExpiredSeconds(REMEMBER_SECONDS);
        } else {
            userLoginInfo.setExpiredSeconds(NO_REMEMBER_SECONDS);
        }
        Map<String, String> map = userService.login(userLoginInfo);
        if (map.containsKey("ticket")) {
            // 登录凭证写入cookie
            Cookie cookie = new Cookie("ticket", map.get("ticket"));
            cookie.setPath(contextPath);
            cookie.setMaxAge(userLoginInfo.getExpiredSeconds());
            response.addCookie(cookie);
            return "redirect:/index";
        }

        // 登陆失败的情况
        model.addAttribute("usernameMsg", map.get("usernameMsg"));
        model.addAttribute("passwordMsg", map.get("passwordMsg"));
        return "/site/login";
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Integer logoutResult = userService.logout(request, response);
        if (logoutResult == 0) {
            // 重定向的是url而非html页面
            return "redirect:/login";
        }
        return "/index";
    }

}
