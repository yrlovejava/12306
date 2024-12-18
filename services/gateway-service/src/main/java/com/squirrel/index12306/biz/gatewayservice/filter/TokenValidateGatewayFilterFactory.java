package com.squirrel.index12306.biz.gatewayservice.filter;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.squirrel.index12306.biz.gatewayservice.config.Config;
import com.squirrel.index12306.biz.gatewayservice.toolkit.JWTUtil;
import com.squirrel.index12306.biz.gatewayservice.toolkit.UserInfoDTO;
import com.squirrel.index12306.framework.starter.bases.constant.UserConstant;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * SpringCloud Gateway Token拦截器
 */
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

    public TokenValidateGatewayFilterFactory(){
        super(Config.class);
    }

    /**
     * 注销用户时需要传递 Token
     */
    public static final String DELETION_PATH = "/api/user-service/deletion";

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().toString();
            // 如果不在白名单中
            if(!isPathInWhiteList(requestPath,config.getWhitePathList())) {
                // 如果在黑名单中
                if (isPathInBlackPreList(requestPath, config.getBlackPathPreList())) {
                    // 如果合法
                    // 获取Token
                    String token = request.getHeaders().getFirst("Authorization");
                    UserInfoDTO userInfo = JWTUtil.parseJwtToken(token);
                    // 如果userInfo不合法
                    if (!validateToken(userInfo)) {
                        // 直接报错401
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }

                    // 向请求头中添加用户基本信息
                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate().headers(httpHeaders -> {
                        httpHeaders.set(UserConstant.USER_ID_KEY, userInfo.getUserId());
                        httpHeaders.set(UserConstant.USER_NAME_KEY, userInfo.getUsername());
                        httpHeaders.set(UserConstant.REAL_NAME_KEY, URLEncoder.encode(userInfo.getRealName(), StandardCharsets.UTF_8));
                        // 判断是否是注销请求
                        if (Objects.equals(requestPath, DELETION_PATH)) {
                            httpHeaders.set(UserConstant.USER_TOKEN_KEY, token);
                        }
                    });
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                }
            }
            return chain.filter(exchange);
        });
    }

    /**
     * 验证请求路径是否在黑名单中
     * @param requestPath 请求路径
     * @param blackPathPreList 黑名单路径
     * @return 是否在
     */
    private boolean isPathInBlackPreList(String requestPath, List<String> blackPathPreList) {
        if (CollectionUtils.isEmpty(blackPathPreList)) {
            return false;
        }
        return blackPathPreList.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 验证请求路径是否在白名单中
     * @param requestPath 请求路径
     * @param whitelist 白名单路径
     * @return 是否在
     */
    private boolean isPathInWhiteList(String requestPath, List<String> whitelist) {
        if (CollectionUtils.isEmpty(whitelist)) {
            return false;
        }
        return whitelist.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 验证token是否合法
     * @param userInfoDTO token解析出来的用户信息
     * @return 是否合法
     */
    private boolean validateToken(UserInfoDTO userInfoDTO) {
        return userInfoDTO != null;
    }
}
