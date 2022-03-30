package graphql.kickstart.spring;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import org.springframework.web.server.ServerWebExchange;

public interface GraphQLSpringContext extends GraphQLKickstartContext {

  ServerWebExchange getServerWebExchange();
}
