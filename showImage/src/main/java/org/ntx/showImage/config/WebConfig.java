package org.ntx.showImage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.ntx.showImage.config.SystemConstant.IMAGE_FIND_ROUTE;
import static org.ntx.showImage.config.SystemConstant.IMAGE_UPLOAD_DIR_FIND;
//@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/"+IMAGE_FIND_ROUTE+"/**")
                        .addResourceLocations("file:" + IMAGE_UPLOAD_DIR_FIND );
    }
}
