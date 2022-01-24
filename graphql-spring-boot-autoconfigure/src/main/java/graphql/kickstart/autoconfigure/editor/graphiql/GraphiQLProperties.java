package graphql.kickstart.autoconfigure.editor.graphiql;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.core.io.ClassPathResource;

@Data
@ConfigurationProperties("graphql.graphiql")
public class GraphiQLProperties {

  private boolean enabled = false;
  private Endpoint endpoint = new Endpoint();
  private CodeMirror codeMirror = new CodeMirror();
  private Props props = new Props();
  private String pageTitle = "GraphiQL";
  private String mapping = "/graphiql";
  private Subscriptions subscriptions = new Subscriptions();
  private Cdn cdn = new Cdn();
  private String basePath = "/";
  private Map<String, String> headers;

  @Data
  public static class Endpoint {

    private String graphql = "/graphql";
    private String subscriptions = "/subscriptions";
  }

  @Data
  public static class CodeMirror {

    private String version = "5.47.0";
  }

  @Data
  public static class Resources {
    private ClassPathResource query;
    private ClassPathResource variables;
    private ClassPathResource defaultQuery;
  }

  @Data
  public static class Props {

    private GraphiQLVariables variables = new GraphiQLVariables();
    private Resources resources = new Resources();

    /** See https://github.com/graphql/graphiql/tree/main/packages/graphiql#props */
    @Data
    @With
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GraphiQLVariables {

      private String query;
      private String variables;
      private String operationName;
      private String response;
      private String defaultQuery;
      private boolean defaultVariableEditorOpen;
      private boolean defaultSecondaryEditorOpen;
      private String editorTheme;
      private boolean readOnly;
      private boolean docsExplorerOpen;
      private boolean headerEditorEnabled;
      private boolean shouldPersistHeaders;
    }
  }

  @Data
  public static class Cdn {

    private boolean enabled = false;
    private String version = "1.0.6";
  }

  @Data
  public static class Subscriptions {

    /**
     * Subscription timeout. If a duration suffix is not specified, second will be used.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = Duration.ofSeconds(30);
    private boolean reconnect = false;
  }
}
