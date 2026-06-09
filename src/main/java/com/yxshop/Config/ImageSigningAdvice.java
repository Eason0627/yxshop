package com.yxshop.Config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yxshop.Utils.AliOSSUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局图片签名拦截器
 *
 * 拦截所有 API 响应，递归扫描响应体中所有看起来像图片字段的字符串值：
 * 若该值是 OSS 对象键（AliOSSUtils.isObjectKey 返回 true），
 * 则自动生成 2 小时预签名 URL 后替换，确保所有服务无需手动调用签名逻辑。
 *
 * 覆盖场景：Map、List、POJO（通过反射）
 */
@ControllerAdvice
public class ImageSigningAdvice implements ResponseBodyAdvice<Object> {

    private final AliOSSUtils aliOSSUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 判断是否为图片相关字段名的关键字（小写匹配）
    private static final Set<String> IMAGE_KEY_WORDS = new HashSet<>(Arrays.asList(
            "image", "img", "avatar", "logo", "banner", "cover",
            "photo", "icon", "thumbnail", "picture"
            // "pic" 已移除：会误匹配 "topic"、"topicName" 等非图片字段
    ));

    // POJO 类的字段缓存，避免每次反射扫描
    private final ConcurrentHashMap<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    public ImageSigningAdvice(AliOSSUtils aliOSSUtils) {
        this.aliOSSUtils = aliOSSUtils;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 对所有返回值都生效
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof com.yxshop.Utils.Result) {
            // Result 的 data 是 Object 类型，直接取出递归处理
            Object data = ((com.yxshop.Utils.Result) body).getData();
            if (data != null) processValue(data);
        } else if (body != null) {
            processValue(body);
        }
        return body;
    }

    // ── 跳过不需要处理的类型 ──────────────────────────────────────────────────
    private static final Set<String> SKIP_PACKAGES = new HashSet<>(Arrays.asList(
            "java.", "javax.", "sun.", "org.springframework.", "org.apache.",
            "com.fasterxml.", "io.jsonwebtoken.", "org.mybatis."
    ));

    // ── 递归处理 ─────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processValue(Object obj) {
        if (obj == null) return;

        if (obj instanceof Map) {
            processMap((Map<Object, Object>) obj);
        } else if (obj instanceof IPage) {
            // MyBatis-Plus 分页对象（Page<T>）
            processValue(((IPage<?>) obj).getRecords());
        } else if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                processValue(item);
            }
        } else if (obj instanceof Object[]) {
            for (Object item : (Object[]) obj) {
                processValue(item);
            }
        } else {
            // POJO 对象：通过反射处理图片字段
            processPojo(obj);
        }
    }

    private void processMap(Map<Object, Object> map) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && isImageKey((String) key)) {
                if (value instanceof String) {
                    String signed = sign((String) value);
                    if (signed != null) entry.setValue(signed);
                } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    // List<String>（图片数组，如 images 字段）→ 逐项签名
                    // List<POJO/Map>（如 banners、activities）→ 递归处理每个对象
                    if (!list.isEmpty() && list.get(0) instanceof String) {
                        signListInPlace(list);
                    } else {
                        processValue(value);
                    }
                } else if (value instanceof Map) {
                    processValue(value);
                }
            } else {
                // 非图片键：递归子值（Map / List / POJO 均需处理；String/数字/Boolean 由 processValue 内部快速跳过）
                if (value != null) {
                    processValue(value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void signListInPlace(List<?> list) {
        List<Object> mutable = (List<Object>) list;
        for (int i = 0; i < mutable.size(); i++) {
            Object item = mutable.get(i);
            if (item instanceof String) {
                String signed = sign((String) item);
                if (signed != null) mutable.set(i, signed);
            }
        }
    }

    private void processPojo(Object pojo) {
        // 跳过基础类型、Java 内置类型、枚举等
        Class<?> cls = pojo.getClass();
        if (cls.isPrimitive() || cls.isEnum()) return;
        String pkg = cls.getPackageName();
        for (String skip : SKIP_PACKAGES) {
            if (pkg.startsWith(skip)) return;
        }

        List<Field> fields = fieldCache.computeIfAbsent(cls, this::collectImageFields);
        for (Field field : fields) {
            try {
                Object value = field.get(pojo);
                if (value instanceof String) {
                    String signed = sign((String) value);
                    if (signed != null) field.set(pojo, signed);
                } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    String fieldName = field.getName().toLowerCase();
                    if (isImageKey(fieldName) && !list.isEmpty() && list.get(0) instanceof String) {
                        // 图片字段的 List<String>（如 images 数组），逐项签名
                        signListInPlace(list);
                    } else {
                        processValue(value);
                    }
                } else if (value instanceof Map) {
                    processValue(value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private List<Field> collectImageFields(Class<?> cls) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                Class<?> ft = f.getType();
                String name = f.getName().toLowerCase();
                // 1. String 类型的图片字段 → 直接签名
                if (ft == String.class && isImageKey(name)) {
                    f.setAccessible(true);
                    result.add(f);
                }
                // 2. Map / List / Object 类型字段 → 递归处理
                else if (Map.class.isAssignableFrom(ft)
                        || List.class.isAssignableFrom(ft)
                        || ft == Object.class) {
                    f.setAccessible(true);
                    result.add(f);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    // ── 工具 ─────────────────────────────────────────────────────────────────

    /**
     * 判断字段名是否是图片类字段
     */
    private boolean isImageKey(String key) {
        String lower = key.toLowerCase();
        for (String word : IMAGE_KEY_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    /**
     * 若值是 OSS objectKey 或包含 objectKey 的 JSON 数组，生成预签名 URL 并返回；否则返回 null。
     * 支持：
     *   - 裸对象键 "uuid.jpg" → 签名后的 URL
     *   - JSON 数组 ["uuid1.jpg","uuid2.jpg"] → 每项单独签名，重新序列化为 JSON 数组
     *   - 逗号分隔 "uuid1.jpg,uuid2.jpg" → 每项单独签名，重新序列化为 JSON 数组
     */
    private String sign(String value) {
        if (value == null || value.isBlank()) return null;

        // 尝试解析为 JSON 数组
        String trimmed = value.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> keys = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                List<String> signed = new ArrayList<>(keys.size());
                boolean changed = false;
                for (String key : keys) {
                    if (AliOSSUtils.isObjectKey(key)) {
                        String url = aliOSSUtils.generatePresignedUrl(key, 120);
                        if (url != null && !url.equals(key)) {
                            signed.add(url);
                            changed = true;
                            continue;
                        }
                    }
                    signed.add(key);
                }
                if (!changed) return null;
                return objectMapper.writeValueAsString(signed);
            } catch (Exception ignored) {
                // 不是合法 JSON 数组，继续尝试逗号分隔处理
            }
        }

        // 逗号分隔的多个 key（如 "uuid1.jpg,uuid2.jpg"）
        if (!trimmed.contains("://") && !trimmed.startsWith("/") && trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            List<String> signed = new ArrayList<>(parts.length);
            boolean changed = false;
            for (String part : parts) {
                String key = part.trim();
                if (AliOSSUtils.isObjectKey(key)) {
                    String url = aliOSSUtils.generatePresignedUrl(key, 120);
                    if (url != null && !url.equals(key)) {
                        signed.add(url);
                        changed = true;
                        continue;
                    }
                }
                signed.add(key);
            }
            if (!changed) return null;
            try {
                return objectMapper.writeValueAsString(signed);
            } catch (Exception ignored) {
                return null;
            }
        }

        // 单个 key
        if (!AliOSSUtils.isObjectKey(trimmed)) return null;
        String signedUrl = aliOSSUtils.generatePresignedUrl(trimmed, 120);
        return (signedUrl != null && !signedUrl.equals(trimmed)) ? signedUrl : null;
    }
}
