package com.yxshop.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.sms")
public class AliyunSmsProperties {
    private String accessKeyId = "";
    private String accessKeySecret = "";
    /** 号码验证服务系统签名（由阿里云提供，非自定义签名） */
    private String signName = "";
    /** 验证码模板 code（可选，dypnsapi 测试签名可使用默认模板） */
    private String templateCode = "";
    private String regionId = "cn-hangzhou";
}
