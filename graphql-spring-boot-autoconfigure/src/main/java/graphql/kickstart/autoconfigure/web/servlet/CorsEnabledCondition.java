package graphql.kickstart.autoconfigure.web.servlet;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CorsEnabledCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    PropertyResolver resolver = context.getEnvironment();
    Boolean kebabCorsEnabled = resolver.getProperty("graphql.servlet.cors-enabled", Boolean.class);
    if (kebabCorsEnabled != null) {
      return kebabCorsEnabled;
    }
    Boolean camelCorsEnabled = resolver.getProperty("graphql.servlet.corsEnabled", Boolean.class);
    if (camelCorsEnabled != null) {
      return camelCorsEnabled;
    }
    return true;
  }
}
