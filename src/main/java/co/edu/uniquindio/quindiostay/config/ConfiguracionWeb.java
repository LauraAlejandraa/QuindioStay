package co.edu.uniquindio.quindiostay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor de sesión en todas las rutas menos login, registro y archivos estáticos. */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorSesion interceptorSesion;

    public ConfiguracionWeb(InterceptorSesion interceptorSesion) {
        this.interceptorSesion = interceptorSesion;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorSesion)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/login", "/registro", "/logout", "/error",
                        "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico");
    }
}
