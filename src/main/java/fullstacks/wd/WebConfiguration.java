package fullstacks.wd;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final SiteImageStorage imageStorage;

    public WebConfiguration(SiteImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = imageStorage.getUploadDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
