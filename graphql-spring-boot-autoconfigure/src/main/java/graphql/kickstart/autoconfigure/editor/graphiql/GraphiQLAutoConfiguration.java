package graphql.kickstart.autoconfigure.editor.graphiql;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;;

/**
 * @author Andrew Potter
 * @author Ronny Bräunlich
 */
@Configuration
@ConditionalOnProperty(value = "graphql.graphiql.enabled", havingValue = "true")
@EnableConfigurationProperties(GraphiQLProperties.class)
public class GraphiQLAutoConfiguration {

  @Bean(name = "graphiQLController")
  @ConditionalOnWebApplication(type = SERVLET)
  ServletGraphiQLController servletGraphiQLController(GraphiQLProperties properties) {
    return new ServletGraphiQLController(properties);
  }

  @Bean(name = "graphiQLController")
  @ConditionalOnMissingBean(ServletGraphiQLController.class)
  @ConditionalOnWebApplication(type = REACTIVE)
  ReactiveGraphiQLController reactiveGraphiQLController(GraphiQLProperties properties) {
    return new ReactiveGraphiQLController(properties);
  }

  @Bean
  @ConditionalOnWebApplication(type = REACTIVE)
  @ConditionalOnExpression("'${graphql.graphiql.cdn.enabled:false}' == 'false'")
  public RouterFunction<ServerResponse> graphiqlStaticFilesRouter() {

    return RouterFunctions.resources(
        "/vendor/graphiql/**", new ClassPathResource("static/vendor/graphiql/"));
  }

  @Configuration
  @EnableWebMvc
  @ConditionalOnWebApplication(type = SERVLET)
  @ConditionalOnExpression("'${graphql.graphiql.cdn.enabled:false}' == 'false'")
  public static class GraphiQLWebMvcResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

      registry.addResourceHandler("/vendor/graphiql/**")
          .addResourceLocations(new ClassPathResource("static/vendor/graphiql/"));
    }
  }
}
