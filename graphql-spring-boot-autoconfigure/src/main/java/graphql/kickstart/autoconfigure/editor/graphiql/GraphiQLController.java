package graphql.kickstart.autoconfigure.editor.graphiql;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.autoconfigure.editor.graphiql.GraphiQLProperties.Props.GraphiQLVariables;
import graphql.kickstart.autoconfigure.editor.graphiql.GraphiQLProperties.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** @author Andrew Potter */
@Slf4j
@RequiredArgsConstructor
public abstract class GraphiQLController {

  private static final String CDNJS_CLOUDFLARE_COM_AJAX_LIBS = "//cdnjs.cloudflare.com/ajax/libs/";
  private static final String CDN_JSDELIVR_NET_NPM = "//cdn.jsdelivr.net/npm/";
  private static final String GRAPHIQL = "graphiql";
  private static final String FAVICON_GRAPHQL_ORG = "//graphql.org/img/favicon.png";

  private final GraphiQLProperties graphiQLProperties;

  private String template;
  private String props;

  public void onceConstructed() throws IOException {
    loadTemplate();
    loadProps();
  }

  private void loadTemplate() throws IOException {
    try (InputStream inputStream =
        new ClassPathResource("templates/graphiql.html").getInputStream()) {
      template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    }
  }

  private void loadProps() throws IOException {
    Resources resources = graphiQLProperties.getProps().getResources();
    GraphiQLVariables combinedVariables = graphiQLProperties.getProps().getVariables();
    if (nonNull(resources.getVariables())) {
      combinedVariables = combinedVariables.withVariables(getContent(resources.getVariables()));
    }
    if (nonNull(resources.getDefaultQuery())) {
      combinedVariables
          = combinedVariables.withDefaultQuery(getContent(resources.getDefaultQuery()));
    }
    if (nonNull(resources.getQuery())) {
      combinedVariables = combinedVariables.withQuery(getContent(resources.getQuery()));
    }
    this.props = new ObjectMapper().writeValueAsString(combinedVariables);
  }

  public byte[] graphiql(
      String contextPath, @PathVariable Map<String, String> params, Object csrf) {
    Map<String, String> finalHeaders = Optional.ofNullable(graphiQLProperties.getHeaders())
        .orElseGet(Collections::emptyMap);
    if (csrf != null) {
      CsrfToken csrfToken = (CsrfToken) csrf;
      finalHeaders = new HashMap<>(finalHeaders);
      finalHeaders.put(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    Map<String, String> replacements =
        getReplacements(
            constructGraphQlEndpoint(contextPath, params),
            contextPath + graphiQLProperties.getEndpoint().getSubscriptions(),
            contextPath + graphiQLProperties.getBasePath(),
            finalHeaders);

    String populatedTemplate = StringSubstitutor.replace(template, replacements);
    return populatedTemplate.getBytes(Charset.defaultCharset());
  }

  private Map<String, String> getReplacements(
      String graphqlEndpoint, String subscriptionsEndpoint, String staticBasePath,
      Map<String, String> headers) {
    Map<String, String> replacements = new HashMap<>();
    replacements.put("graphqlEndpoint", graphqlEndpoint);
    replacements.put("subscriptionsEndpoint", subscriptionsEndpoint);
    replacements.put("staticBasePath", staticBasePath);
    replacements.put("pageTitle", graphiQLProperties.getPageTitle());
    replacements.put(
        "pageFavicon", getResourceUrl(staticBasePath, "favicon.ico", FAVICON_GRAPHQL_ORG));
    replacements.put(
        "es6PromiseJsUrl",
        getResourceUrl(
            staticBasePath,
            "es6-promise.auto.min.js",
            joinCdnjsPath("es6-promise", "4.1.1", "es6-promise.auto.min.js")));
    replacements.put(
        "fetchJsUrl",
        getResourceUrl(
            staticBasePath, "fetch.min.js", joinCdnjsPath("fetch", "2.0.4", "fetch.min.js")));
    replacements.put(
        "reactJsUrl",
        getResourceUrl(
            staticBasePath,
            "react.min.js",
            joinCdnjsPath("react", "16.8.3", "umd/react.production.min.js")));
    replacements.put(
        "reactDomJsUrl",
        getResourceUrl(
            staticBasePath,
            "react-dom.min.js",
            joinCdnjsPath("react-dom", "16.8.3", "umd/react-dom.production.min.js")));
    replacements.put(
        "graphiqlCssUrl",
        getResourceUrl(
            staticBasePath,
            "graphiql.min.css",
            joinJsDelivrPath(GRAPHIQL, graphiQLProperties.getCdn().getVersion(), "graphiql.css")));
    replacements.put(
        "graphiqlJsUrl",
        getResourceUrl(
            staticBasePath,
            "graphiql.min.js",
            joinJsDelivrPath(
                GRAPHIQL, graphiQLProperties.getCdn().getVersion(), "graphiql.min.js")));
    replacements.put(
        "subscriptionsTransportWsBrowserClientUrl",
        getResourceUrl(
            staticBasePath,
            "subscriptions-transport-ws-browser-client.js",
            joinJsDelivrPath("subscriptions-transport-ws", "0.8.3", "browser/client.js")));
    replacements.put(
        "graphiqlSubscriptionsFetcherBrowserClientUrl",
        getResourceUrl(
            staticBasePath,
            "graphiql-subscriptions-fetcher-browser-client.js",
            joinJsDelivrPath("graphiql-subscriptions-fetcher", "0.0.2", "browser/client.js")));
    replacements.put("props", props);
    try {
      replacements.put("headers", new ObjectMapper().writeValueAsString(headers));
    } catch (JsonProcessingException e) {
      log.error("Cannot serialize headers", e);
    }
    replacements.put(
        "subscriptionClientTimeout",
        String.valueOf(graphiQLProperties.getSubscriptions().getTimeout().toMillis()));
    replacements.put(
        "subscriptionClientReconnect",
        String.valueOf(graphiQLProperties.getSubscriptions().isReconnect()));
    replacements.put("editorThemeCss", getEditorThemeCssURL());
    return replacements;
  }

  private String getEditorThemeCssURL() {
    String theme = graphiQLProperties.getProps().getVariables().getEditorTheme();
    if (theme != null) {
      return String.format(
          "https://cdnjs.cloudflare.com/ajax/libs/codemirror/%s/theme/%s.min.css",
          graphiQLProperties.getCodeMirror().getVersion(), theme.split("\\s")[0]);
    }
    return "";
  }

  private String getResourceUrl(String staticBasePath, String staticFileName, String cdnUrl) {
    if (graphiQLProperties.getCdn().isEnabled() && StringUtils.isNotBlank(cdnUrl)) {
      return cdnUrl;
    }
    return joinStaticPath(staticBasePath, staticFileName);
  }

  private String joinStaticPath(String staticBasePath, String staticFileName) {
    return staticBasePath + "vendor/graphiql/" + staticFileName;
  }

  private String joinCdnjsPath(String library, String cdnVersion, String cdnFileName) {
    return CDNJS_CLOUDFLARE_COM_AJAX_LIBS + library + "/" + cdnVersion + "/" + cdnFileName;
  }

  private String joinJsDelivrPath(String library, String cdnVersion, String cdnFileName) {
    return CDN_JSDELIVR_NET_NPM + library + "@" + cdnVersion + "/" + cdnFileName;
  }

  private String constructGraphQlEndpoint(
      String contextPath, @RequestParam Map<String, String> params) {
    String endpoint = graphiQLProperties.getEndpoint().getGraphql();
    for (Map.Entry<String, String> param : params.entrySet()) {
      endpoint = endpoint.replaceAll("\\{" + Pattern.quote(param.getKey()) + "}", param.getValue());
    }
    if (StringUtils.isNotBlank(contextPath) && !endpoint.startsWith(contextPath)) {
      return contextPath + endpoint;
    }
    return endpoint;
  }

  private String getContent(final ClassPathResource resource) throws IOException {
    return new String(Files.readAllBytes(resource.getFile().toPath()), StandardCharsets.UTF_8);
  }
}
