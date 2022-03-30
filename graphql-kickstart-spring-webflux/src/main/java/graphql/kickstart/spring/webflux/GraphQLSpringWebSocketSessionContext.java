package graphql.kickstart.spring.webflux;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface GraphQLSpringWebSocketSessionContext extends GraphQLKickstartContext {

  WebSocketSession getWebSocketSession();
}
