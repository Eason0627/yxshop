package com.yxshop.Aop;

import com.alibaba.fastjson.JSONObject;
import com.yxshop.Module.Admin.Entity.OperationLog;
import com.yxshop.Module.Admin.Mapper.OPerationLogMapper;
import com.yxshop.Utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Autowired
    private OPerationLogMapper oPerationLogMapper;

    /**
     * 每次通过 RequestContextHolder 获取当前请求（线程安全，避免 singleton Bean 注入失效）
     */
    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    // 拦截所有 Module 下 Controller 包中的公共方法
    @Around("execution(* com.yxshop.Module..Controller.*.*(..))")
    public Object recordOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = null;
        String resultType = "SUCCESS";

        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            resultType = "ERROR";
            persistLog(joinPoint, beginTime, resultType);
            throw e;
        }
        persistLog(joinPoint, beginTime, resultType);
        return result;
    }

    private void persistLog(ProceedingJoinPoint joinPoint, long beginTime, String resultType) {
        try {
            HttpServletRequest request = currentRequest();
            if (request == null) return;

            String httpMethod = request.getMethod();
            // 只记录写操作，减少日志量
            if ("GET".equalsIgnoreCase(httpMethod) || "OPTIONS".equalsIgnoreCase(httpMethod)) return;

            // 从 JWT 中提取操作人 ID（null-safe）
            Long userId = null;
            try {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    Claims claims = JwtUtils.checkToken(authHeader.substring(7));
                    if (claims != null) {
                        Object idObj = claims.get("id");
                        userId = idObj instanceof Number ? ((Number) idObj).longValue() : null;
                    }
                }
            } catch (Exception ignored) {}

            if (userId == null) return; // 未登录请求不记录

            String requestUrl = request.getRequestURI();
            String ip         = request.getRemoteAddr();

            // 序列化请求参数（跳过 HttpServletRequest 类型，限制长度）
            String params = null;
            try {
                params = Arrays.stream(joinPoint.getArgs())
                        .filter(a -> !(a instanceof HttpServletRequest))
                        .map(a -> {
                            try { return JSONObject.toJSONString(a); }
                            catch (Exception ex) { return String.valueOf(a); }
                        })
                        .collect(Collectors.joining(", "));
                if (params.length() > 2000) params = params.substring(0, 2000) + "...";
            } catch (Exception ignored) {}

            OperationLog operationLog = new OperationLog(
                    null, userId,
                    buildOperationDesc(httpMethod, requestUrl),
                    requestUrl, httpMethod,
                    params, ip, resultType, null,
                    LocalDateTime.now(),
                    System.currentTimeMillis() - beginTime
            );
            oPerationLogMapper.insertOperationLog(operationLog);
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private String buildOperationDesc(String method, String url) {
        String action;
        switch (method.toUpperCase()) {
            case "POST":   action = "新增/提交"; break;
            case "PUT":    action = "修改";      break;
            case "DELETE": action = "删除";      break;
            default:       action = "操作";      break;
        }
        String module = "未知模块";
        if (url.contains("/products"))   module = "商品";
        else if (url.contains("/orders")) module = "订单";
        else if (url.contains("/users"))  module = "用户";
        else if (url.contains("/shops"))  module = "店铺";
        else if (url.contains("/reviews"))module = "审核";
        else if (url.contains("/admin"))  module = "管理后台";
        else if (url.contains("/coupon") || url.contains("/activit")) module = "营销";
        else if (url.contains("/brand"))  module = "品牌";
        else if (url.contains("/categor"))module = "分类";
        else if (url.contains("/banner")) module = "Banner";
        else if (url.contains("/message") || url.contains("/support")) module = "消息/工单";
        else if (url.contains("/points")) module = "积分";
        else if (url.contains("/media") || url.contains("/files")) module = "资源";
        return action + module;
    }
}
