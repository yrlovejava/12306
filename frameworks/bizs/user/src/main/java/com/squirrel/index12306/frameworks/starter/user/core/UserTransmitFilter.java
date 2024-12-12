package com.squirrel.index12306.frameworks.starter.user.core;

import com.squirrel.index12306.framework.starter.bases.constant.UserConstant;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 用户信息传输过滤器
 */
public class UserTransmitFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
        if(StringUtils.hasText(userId)) {
            String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
            String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);
            if(StringUtils.hasText(userName)) {
                userName = URLDecoder.decode(userName, StandardCharsets.UTF_8);
            }
            if(StringUtils.hasText(realName)) {
                realName = URLDecoder.decode(realName, StandardCharsets.UTF_8);
            }
            MDC.put(UserConstant.USER_ID_KEY, userId);
            MDC.put(UserConstant.USER_NAME_KEY, userName);
            MDC.put(UserConstant.REAL_NAME_KEY, realName);
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        }finally {
            MDC.clear();
        }
    }
}
