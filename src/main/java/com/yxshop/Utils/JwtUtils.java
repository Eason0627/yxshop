package com.yxshop.Utils;

import com.yxshop.Module.User.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    private static final String JWT_KEY = "yxshopHymAndHdyqazwsxedcrfvtgbyhnujmikbcnrbcxc";
    private static  String token;

    public static String getSecret(User userInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id",       userInfo.getId());
        claims.put("username", userInfo.getUsername());
        claims.put("role",     userInfo.getRole()); // 必须包含 role，否则拦截器无法识别身份

        String jwt = Jwts.builder()
                .setClaims(claims)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_KEY)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 12 * 3600 * 1000 * 24))
                .compact();
        return jwt;
    }

    public static String getSecret(Long id, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("role", role);

        String jwt = Jwts.builder()
                .setClaims(claims)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_KEY)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 12 * 3600 * 1000 * 24))
                .compact();
        return jwt;
    }

    public static Claims checkToken(String token) {
        if(token != null) {
            Claims claims = Jwts.parser().setSigningKey(JWT_KEY).parseClaimsJws(token).getBody();
//            return Jwts.parser().setSigningKey(JWT_KEY).parseClaimsJws(token).getBody() != null;
            return claims;
        }
       return null;
    }
}
