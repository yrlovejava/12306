package com.squirrel.index12306.biz.userservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                // 接口文档标题
                .info(new Info().title("12306用户服务应用 API")
                        // 接口文档描述
                        .description("12306用户服务应用接口文档")
                        // 接口文档版本
                        .version("v1.0")
                        // 开发者联系方式
                        .contact(new Contact().name("yrlovejava").url("https://github.com/yrlovejava")))
                .externalDocs(new ExternalDocumentation()
                        // 额外补充说明
                        .description("Github 仓库")
                        // 额外补充链接
                        .url("https://github.com/yrlovejava/12306"));
    }

}
