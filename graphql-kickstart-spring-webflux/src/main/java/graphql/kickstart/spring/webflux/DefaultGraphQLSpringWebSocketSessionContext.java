package graphql.kickstart.spring.webflux;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import lombok.NonNull;
import org.dataloader.DataLoaderRegistry;
import org.springframework.web.reactive.socket.WebSocketSession;

public class DefaultGraphQLSpringWebSocketSessionContext extends DefaultGraphQLContext
    implements GraphQLSpringWebSocketSessionContext {

  public DefaultGraphQLSpringWebSocketSessionContext(WebSocketSession webSocketSession) {
    this(new DataLoaderRegistry(), webSocketSession);
  }

  public DefaultGraphQLSpringWebSocketSessionContext(
      DataLoaderRegistry dataLoaderRegistry, @NonNull WebSocketSession webSocketSession) {
    super(dataLoaderRegistry);
    put(WebSocketSession.class, webSocketSession);
  }

  /**
   * @deprecated Use {@code dataFetchingEnvironment.getGraphQlContext().get(WebSocketSession.class)}
   *     instead. Since 13.0.0
   */
  @Override
  public WebSocketSession getWebSocketSession() {
    return (WebSocketSession) getMapOfContext().get(WebSocketSession.class);
  }
}
