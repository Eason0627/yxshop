package com.yxshop.Config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.yxshop.Interceptor.MyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MyInterceptor myInterceptor;

    /**
     * Spring 托管的 ObjectMapper bean（被 WebSocket SessionManager 等组件注入）。
     * 在此统一注册 Long→String 序列化，确保 WebSocket 推送的 JSON 与 HTTP 响应格式一致，
     * 避免 JS 64 位整数精度丢失导致 conversationId 比较失败。
     */
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void configureGlobalObjectMapper() {
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        longModule.addSerializer(java.math.BigInteger.class, ToStringSerializer.instance);
        objectMapper.registerModule(longModule);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.format.DateTimeFormatter dtFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(java.time.LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(java.time.LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(java.time.LocalDateTime.class, new LocalDateTimeSerializer(dtFormatter));
        javaTimeModule.addDeserializer(java.time.LocalDateTime.class, new LocalDateTimeDeserializer(dtFormatter));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/sendCode", "/wechat/mini-program/**","/products/getProductPagination");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 找到 Spring Boot 自动注册的 MappingJackson2HttpMessageConverter 并在其上追加模块，
        // 不新增 converter，避免与 StringHttpMessageConverter（springdoc 依赖）产生优先级冲突
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                ObjectMapper om = ((MappingJackson2HttpMessageConverter) converter).getObjectMapper();

                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
                simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
                simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
                om.registerModule(simpleModule);

                JavaTimeModule javaTimeModule = new JavaTimeModule();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
                javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dtFormatter));
                javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dtFormatter));
                om.registerModule(javaTimeModule);

                om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                break;
            }
        }
    }
}