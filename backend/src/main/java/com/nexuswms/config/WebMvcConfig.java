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
                .usePathSegment(1, path -> {
                // If the path starts with swagger or api-docs, return null (meaning: not versioned)
                String pathString = path.toString();
                if (pathString.contains("/swagger-ui") || 
                    pathString.contains("/api-docs") || 
                    pathString.contains("/webjars")) {
                    return false; 
                }
                // Otherwise, process as a normal versioned path
                return true;
            })
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