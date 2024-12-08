package com.squirrel.index12306.biz.userservice.toolkit;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;
import com.squirrel.index12306.biz.userservice.dto.UserLoginReqDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public final class JWTUtil {

    private static final long EXPIRATION = 86400L;
    public static final String ISS = "congo-mall";
    public static final String SECRET = "SecretKey039245678901232039487623456783092349288901402967890140939827";

    /**
     * 生成用户 Token
     * @param userInfo 用户信息
     * @return 用户访问Token
     */
    public static String generateAccessToken(UserLoginReqDTO userInfo) {
        Map<String,Object> customerUserMap = Maps.newHashMap();
        customerUserMap.put("username", userInfo.getUsername());
        customerUserMap.put("password", userInfo.getPassword());
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512,SECRET)
                .setIssuedAt(new Date())
                .setIssuer(ISS)
                .setSubject(JSON.toJSONString(customerUserMap))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                .compact();
    }
}
