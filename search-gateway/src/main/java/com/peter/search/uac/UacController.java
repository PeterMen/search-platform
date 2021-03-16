package com.peter.search.uac;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author wanghaitao
 *
 */
@RestController
@Slf4j
public class UacController {

    @Value("${oauth.domain}")
    private String url;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping(value = "/uac/code")
    public Map code(HttpServletRequest request, HttpServletResponse response, @RequestParam String code) throws IOException{


        return ImmutableMap.of("status", "1", "data", null);
    }
}