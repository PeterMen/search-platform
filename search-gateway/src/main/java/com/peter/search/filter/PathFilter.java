package com.peter.search.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 过滤URL
 * */
@Component
public class PathFilter extends ZuulFilter {

    @Value("${ui.access.path.list}")
    private String urlPath;

    @Value("${server.servlet.context-path}")
    private String cotextPath;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        if(!pathFilter(request.getRequestURI().replaceFirst(cotextPath,""))){
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(404);
        }
        return null;
    }

    private boolean pathFilter(String url){
        if(StringUtils.isNotEmpty(urlPath)){
            return Arrays.stream(urlPath.split(",")).filter(StringUtils::isNotEmpty).anyMatch(s -> pathMatcher.match(s, url));
        }
        return false;
    }
}
