package graphql.kickstart.spring;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import lombok.NonNull;
import org.dataloader.DataLoaderRegistry;
import org.springframework.web.server.ServerWebExchange;

public class GraphQLSpringServerWebExchangeContext extends DefaultGraphQLContext
    implements GraphQLSpringContext {

  public GraphQLSpringServerWebExchangeContext(ServerWebExchange serverWebExchange) {
    this(new DataLoaderRegistry(), serverWebExchange);
  }

  public GraphQLSpringServerWebExchangeContext(
      DataLoaderRegistry dataLoaderRegistry, @NonNull ServerWebExchange serverWebExchange) {
    super(dataLoaderRegistry);
    put(ServerWebExchange.class, serverWebExchange);
  }

  /**
   * @deprecated Use {@code
   *     dataFetchingEnvironment.getGraphQlContext().get(ServerWebExchange.class)} instead. Since
   *     13.0.0
   */
  @Override
  @Deprecated
  public ServerWebExchange getServerWebExchange() {
    return (ServerWebExchange) getMapOfContext().get(ServerWebExchange.class);
  }
}
