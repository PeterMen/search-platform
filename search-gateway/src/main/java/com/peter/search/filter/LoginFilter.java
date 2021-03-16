package com.peter.search.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 过滤URL
 * */
@Component
public class LoginFilter extends ZuulFilter {

    public static final String LOGIN_NAME = "login_name_";

    @Value("${oauth.domain}")
    private String url;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        if(!checkLogin(ctx)){
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(302);
        }
        return null;
    }

    public boolean checkLogin(RequestContext ctx) {
        HttpServletRequest request = ctx.getRequest();
        HttpServletResponse response = ctx.getResponse();
        if (StringUtils.isNotEmpty(request.getHeader("token"))){
            String loginName = stringRedisTemplate.opsForValue().get(LOGIN_NAME+request.getHeader("token"));
            if(StringUtils.isNotEmpty(loginName)){

                // 已登录
                ctx.addZuulRequestHeader("userId", loginName);
                return true;
            }
        }
        // 未登录，跳转到登陆地址
        response.setHeader("redirectUrl", url + "/oauth/entry/");
        response.setStatus(302);
        return false;
    }
}
