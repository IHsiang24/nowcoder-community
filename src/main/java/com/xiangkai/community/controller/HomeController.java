package com.xiangkai.community.controller;

import com.xiangkai.community.constant.CommunityConstant;
import com.xiangkai.community.model.entity.DiscussPost;
import com.xiangkai.community.model.entity.Page;
import com.xiangkai.community.model.entity.User;
import com.xiangkai.community.service.DiscussPostService;
import com.xiangkai.community.service.LikeService;
import com.xiangkai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String index(Model model, Page page) {
        // index方法调用之前，springMVC DispatcherServlet会自动实例化Model和Page，
        // 并且会自动把Page注入给Model，所以在thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost post : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("post", post);
            User user = userService.findUserById(post.getUserId());
            map.put("user", user);
            Long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            map.put("likeCount", likeCount);
            discussPosts.add(map);
        }
        model.addAttribute("discussPosts", discussPosts);
        return "/index";
    }

    @RequestMapping(path = "error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

}
