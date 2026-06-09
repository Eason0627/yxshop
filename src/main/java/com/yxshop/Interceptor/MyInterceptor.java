package com.yxshop.Interceptor;

import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 拦截器
 *
 * 策略：
 *  - 对所有请求，若携带有效 Authorization Bearer token，则解析并将 id/role 注入 request attribute。
 *  - JWT claims 中 role 缺失时（旧 token 兼容），从 DB 实时补查，避免要求用户强制重登录。
 *  - 公开路径（public paths）：即使没有 token 也放行。
 *  - 受保护路径（protected paths）：没有 token 或 token 无效时返回 401。
 */
@Component
public class MyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MyInterceptor.class);

    @Autowired
    private UserMapper userMapper;

    /** 公开路径前缀：无 token 也允许访问 */
    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/ws/",               // WebSocket 端点：认证由 WebSocketHandler 自行处理（token 在 query param）
            "/static/images/",    // App 静态图片代理：无需鉴权，OSS 预签名 URL 重定向
            "/app/auth/",
            "/admin/login",
            "/app/marketing/",
            "/app/content/",
            "/app/products/",
            "/app/topics/",
            "/app/search/",
            "/app/shops/",
            "/app/brands",
            "/wechat/mini-program/",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private static final List<String> PUBLIC_EXACT = Arrays.asList(
            "/login", "/register", "/sendCode"
    );

    /**
     * 仅平台管理员（Admin）可访问的路径前缀。
     * 已登录但非 Admin 的用户访问这些路径时返回 403。
     */
    private static final List<String> ADMIN_ONLY_PREFIXES = Arrays.asList(
            "/app/products/admin/review",
            "/app/products/admin/categories",
            "/app/users/admin/",
            "/app/reviews/admin",       // GET 列表（屏蔽/通过单独处理）
            "/app/reviews/",            // PUT block/restore/approve — 审核操作
            "/admin/users",
            "/admin/roles",
            "/admin/menus",
            "/admin/login-logs"
    );

    /**
     * 在 ADMIN_ONLY_PREFIXES 中，部分路径对 ShopOwner/ShopStaff 开放的例外。
     * 格式：完整 URI 前缀 → 允许操作（HTTP Method 或 "ANY"）
     * 目前：/app/reviews/{id}/reply 对所有登录用户开放
     */
    private static final List<String> ADMIN_ONLY_EXCEPTIONS = Arrays.asList(
            "/app/reviews/"   // 下面会细化：reply 路径对商家开放，block/restore/approve 不开放
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. 尝试解析 token（无论路径是否公开）
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            String token = authHeader.trim();
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }
            try {
                Claims claims = JwtUtils.checkToken(token);
                if (claims != null) {
                    Object id = claims.get("id");
                    Object role = claims.get("role");
                    if (id != null) {
                        Long userId = Long.valueOf(id.toString());
                        request.setAttribute("currentUserId", userId);
                        if (role != null) {
                            request.setAttribute("currentUserRole", String.valueOf(role));
                        } else {
                            // 旧 token 没有 role claim，从 DB 实时补查，避免强制重登录
                            try {
                                User user = userMapper.getUserById(userId);
                                if (user != null && user.getRole() != null) {
                                    request.setAttribute("currentUserRole", user.getRole());
                                }
                            } catch (Exception dbEx) {
                                log.warn("Failed to lookup role from DB for userId {}: {}", userId, dbEx.getMessage());
                            }
                        }
                    }
                }
            } catch (ExpiredJwtException e) {
                // token 过期：公开路径继续放行，受保护路径下面会处理
                log.debug("JWT expired for URI: {}", uri);
            } catch (Exception e) {
                log.debug("JWT parse error for URI: {}: {}", uri, e.getMessage());
            }
        }

        // 2. 判断是否公开路径
        if (isPublicPath(uri)) {
            return true;  // 公开路径直接放行，currentUserId/Role 已按需设置
        }

        // 3. 受保护路径：必须有有效 token（currentUserId 已被设置）
        if (request.getAttribute("currentUserId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录！\"}");
            return false;
        }

        // 4. Admin-only 路径：非 Admin 角色返回 403
        String currentRole = (String) request.getAttribute("currentUserRole");
        if (isAdminOnlyPath(uri, request.getMethod())) {
            if (!"Admin".equals(currentRole)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足，仅平台管理员可操作\"}");
                return false;
            }
        }

        return true;
    }

    private boolean isPublicPath(String uri) {
        if (PUBLIC_EXACT.contains(uri)) return true;
        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * 判断是否为 Admin-only 路径。
     * 规则：
     *  - /app/reviews/{id}/reply (PUT) 对所有已登录用户开放（商家回复）
     *  - /app/reviews/{id}/approve|block|restore (PUT) 仅 Admin
     *  - /app/reviews/admin (GET) 仅 Admin
     *  - /app/users/admin/ 所有方法仅 Admin
     *  - 其余 ADMIN_ONLY_PREFIXES 匹配的路径仅 Admin
     */
    private boolean isAdminOnlyPath(String uri, String method) {
        // 例外：/app/reviews/{id}/reply 允许已登录用户（商家回复评价）
        if (uri.matches("/app/reviews/\\d+/reply") && "PUT".equalsIgnoreCase(method)) {
            return false;
        }
        for (String prefix : ADMIN_ONLY_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
