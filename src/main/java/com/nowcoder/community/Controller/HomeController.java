package com.nowcoder.community.Controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String getRoot() {
        return "forward:/index";
    }

    @RequestMapping(path = {"/index", "/index/{order}"}, method = RequestMethod.GET)
    public String getIndexPage (@PathVariable(name = "order", required = false) String order,
                                Model model, Page page) {

        if (order == null) {
            order = "latest";
        }

        // 注意SpringMVC可以自动实例化model和page
        // 并且会自动将page存入model中
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index/" + order);
        //System.out.println(model.getAttribute("page").toString());

        List<DiscussPost> list = null;
        if (order.equals("latest")) {
            list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        }
        else if (order.equals("hottest")) {
            list = discussPostService.findHottestDiscussPosts(page.getOffset(), page.getLimit());
        }
        else {
            throw new IllegalArgumentException("路径参数错误！");
        }

        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(list != null) {
            for(DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("order", order);
        return "/index";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }

}
