package com.yxshop.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "YXShop API", version = "1.0", description = "YXShop backend interfaces"))
public class OpenApiConfig {
}
