package com.nexuswms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(1)
                .addSupportedVersions("1.0", "2.0")
                .setDefaultVersion("1.0")
                .setVersionParser(new ApiVersionParser<String>() {
                    @Override
                    public String parseVersion(String version) {
                        if (version.startsWith("v") || version.startsWith("V")) {
                            version = version.substring(1);
                        }
                        if (!version.contains(".")) {
                            version = version + ".0";
                        }
                        return version;
                    }
                });
    }
}