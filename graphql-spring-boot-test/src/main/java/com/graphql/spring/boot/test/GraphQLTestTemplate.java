package com.graphql.spring.boot.test;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/** Helper class to test GraphQL queries and mutations. */
public class GraphQLTestTemplate {

  private static final Pattern GRAPHQL_OP_NAME_PATTERN = Pattern.compile(
      "(query|mutation|subscription)\\s+([a-z0-9]+)\\s*[({]",
      (Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
  );

  private final ResourceLoader resourceLoader;
  private final TestRestTemplate restTemplate;
  private final String graphqlMapping;
  private final ObjectMapper objectMapper;
  @Getter private final HttpHeaders headers = new HttpHeaders();

  public GraphQLTestTemplate(
      final ResourceLoader resourceLoader,
      final TestRestTemplate restTemplate,
      @Value("${graphql.servlet.mapping:/graphql}") final String graphqlMapping,
      final ObjectMapper objectMapper) {
    this.resourceLoader = resourceLoader;
    this.restTemplate = restTemplate;
    this.graphqlMapping = graphqlMapping;
    this.objectMapper = objectMapper;
  }

  private String createJsonQuery(String graphql, String operation, ObjectNode variables)
      throws JsonProcessingException {

    ObjectNode wrapper = objectMapper.createObjectNode();
    wrapper.put("query", graphql);
    if (nonNull(operation)) {
      wrapper.put("operationName", operation);
    }
    if (nonNull(variables)) {
      wrapper.set("variables", variables);
    }
    return objectMapper.writeValueAsString(wrapper);
  }

  private String loadQuery(String location) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:" + location);
    return loadResource(resource);
  }

  private String loadResource(Resource resource) throws IOException {
    try (InputStream inputStream = resource.getInputStream()) {
      return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    }
  }

  private String getOperationName(String graphql) {
    if (isNull(graphql)) {
      return null;
    }

    Matcher matcher = GRAPHQL_OP_NAME_PATTERN.matcher(graphql);

    return (matcher.find() ? matcher.group(2) : null);
  }

  /**
   * Add an HTTP header that will be sent with each request this sends.
   *
   * @param name Name (key) of HTTP header to add.
   * @param value Value(s) of HTTP header to add.
   * @return self
   */
  public GraphQLTestTemplate withAdditionalHeader(final String name, final String... value) {
    headers.addAll(name, Arrays.asList(value));
    return this;
  }

  /**
   * Add multiple HTTP header that will be sent with each request this sends.
   *
   * @param additionalHeaders additional headers to add
   * @return self
   */
  public GraphQLTestTemplate withAdditionalHeaders(
      final MultiValueMap<String, String> additionalHeaders) {
    headers.addAll(additionalHeaders);
    return this;
  }

  /**
   * Adds a bearer token to the authorization header.
   *
   * @param token the bearer token
   * @return self
   */
  public GraphQLTestTemplate withBearerAuth(@NonNull final String token) {
    headers.setBearerAuth(token);
    return this;
  }

  /**
   * Adds basic authentication to the authorization header.
   *
   * @param username the username
   * @param password the password
   * @param charset the charset used by the credentials
   * @return self
   */
  public GraphQLTestTemplate withBasicAuth(
      @NonNull final String username,
      @NonNull final String password,
      @Nullable final Charset charset) {
    headers.setBasicAuth(username, password, charset);
    return this;
  }

  /**
   * Adds basic authentication to the authorization header.
   *
   * @param username the username
   * @param password the password
   * @return self
   */
  public GraphQLTestTemplate withBasicAuth(
      @NonNull final String username, @NonNull final String password) {
    headers.setBasicAuth(username, password, null);
    return this;
  }

  /**
   * Adds basic authentication to the authorization header.
   *
   * @param encodedCredentials the encoded credentials
   * @return self
   */
  public GraphQLTestTemplate withBasicAuth(@NonNull final String encodedCredentials) {
    headers.setBasicAuth(encodedCredentials);
    return this;
  }

  /**
   * Replace any associated HTTP headers with the provided headers.
   *
   * @param newHeaders Headers to use.
   * @return self
   */
  public GraphQLTestTemplate withHeaders(final HttpHeaders newHeaders) {
    return withClearHeaders().withAdditionalHeaders(newHeaders);
  }

  /**
   * Clear all associated HTTP headers.
   *
   * @return self
   */
  public GraphQLTestTemplate withClearHeaders() {
    headers.clear();
    return this;
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse perform(String graphqlResource, ObjectNode variables) throws IOException {
    return perform(graphqlResource, null, variables, Collections.emptyList());
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param operationName the name of the GraphQL operation to be executed
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse perform(String graphqlResource, String operationName) throws IOException {
    return perform(graphqlResource, operationName, null, Collections.emptyList());
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param operation the name of the GraphQL operation to be executed
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse perform(String graphqlResource, String operation, ObjectNode variables)
      throws IOException {
    return perform(graphqlResource, operation, variables, Collections.emptyList());
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @param fragmentResources an ordered list of classpath resources containing GraphQL fragment
   *     definitions.
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse perform(
      String graphqlResource, ObjectNode variables, List<String> fragmentResources)
      throws IOException {
    return perform(graphqlResource, null, variables, fragmentResources);
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @param fragmentResources an ordered list of classpath resources containing GraphQL fragment
   *     definitions.
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse perform(
      String graphqlResource,
      String operationName,
      ObjectNode variables,
      List<String> fragmentResources)
      throws IOException {
    String payload = getPayload(graphqlResource, operationName, variables, fragmentResources);
    return post(payload);
  }

  /**
   * Generate GraphQL payload, which consist of 3 elements: query, operationName and variables
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param operationName the name of the GraphQL operation to be executed
   * @param variables the input variables for the GraphQL query
   * @param fragmentResources an ordered list of classpath resources containing GraphQL fragment
   *     definitions.
   * @return the payload
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  private String getPayload(
      String graphqlResource,
      String operationName,
      ObjectNode variables,
      List<String> fragmentResources)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    for (String fragmentResource : fragmentResources) {
      sb.append(loadQuery(fragmentResource));
    }
    String graphql = sb.append(loadQuery(graphqlResource)).toString();
    return createJsonQuery(graphql, operationName, variables);
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource and sends it to the GraphQL
   * server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse postForResource(String graphqlResource) throws IOException {
    return perform(graphqlResource, null, null, Collections.emptyList());
  }

  /**
   * Loads a GraphQL query or mutation from the given classpath resource, appending any graphql
   * fragment resources provided and sends it to the GraphQL server.
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param fragmentResources an ordered list of classpath resources containing GraphQL fragment
   *     definitions.
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse postForResource(String graphqlResource, List<String> fragmentResources)
      throws IOException {
    return perform(graphqlResource, null, null, fragmentResources);
  }

  public GraphQLResponse postMultipart(String query, String variables) {
    return postRequest(RequestFactory.forMultipart(query, variables, headers));
  }

  /**
   * Handle the multipart files upload request to GraphQL servlet
   *
   * <p>In contrast with usual the GraphQL request with body as json payload (consist of query,
   * operationName and variables), multipart file upload request will use multipart/form-data body
   * with the following structure:
   *
   * <ul>
   *   <li><b>operations</b> the payload that we used to use for the normal GraphQL request
   *   <li><b>map</b> a map for referencing between one part of multi-part request and the
   *       corresponding <i>Upload</i> element inside <i>variables</i>
   *   <li>a consequence of upload files embedded into the multi-part request, keyed as numeric
   *       number starting from 1, valued as File payload of usual multipart file upload
   * </ul>
   *
   * <p>Example uploading two files:
   *
   * <p>* Please note that we can't embed binary data into json. Clients library supporting graphql
   * file upload will set variable.files to null for every element inside the array, but each file
   * will be a part of multipart request. GraphQL Servlet will use <i>map</i> part to walk through
   * variables.files and validate the request in combination with other binary file parts
   *
   * <p>----------------------------dummyid
   *
   * <p>Content-Disposition: form-data; name="operations"
   *
   * <p>{ "query": "mutation($files:[Upload]!) {uploadFiles(files:$files)}", "operationName":
   * "uploadFiles", "variables": { "files": [null, null] } }
   *
   * <p>----------------------------dummyid
   *
   * <p>Content-Disposition: form-data; name="map"
   *
   * <p>map: { "1":["variables.files.0"], "2":["variables.files.1"] }
   *
   * <p>----------------------------dummyid
   *
   * <p>Content-Disposition: form-data; name="1"; filename="file1.pdf"
   *
   * <p>Content-Type: application/octet-stream
   *
   * <p>--file 1 binary code--
   *
   * <p>----------------------------dummyid
   *
   * <p>Content-Disposition: form-data; name="2"; filename="file2.pdf"
   *
   * <p>Content-Type: application/octet-stream
   *
   * <p>2: --file 2 binary code--
   *
   * <p>
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @param files ClassPathResource instance for each file that will be uploaded to GraphQL server.
   *     When Spring RestTemplate processes the request, it will automatically produce a valid part
   *     representing given file inside multipart request (including size, submittedFileName, etc.)
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse postFiles(
      String graphqlResource, ObjectNode variables, List<ClassPathResource> files)
      throws IOException {

    return postFiles(
        graphqlResource, variables, files, index -> String.format("variables.files.%d", index));
  }

  /**
   * Handle the multipart files upload request to GraphQL servlet
   *
   * @param graphqlResource path to the classpath resource containing the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @param files ClassPathResource instance for each file that will be uploaded to GraphQL server.
   *     When Spring RestTemplate processes the request, it will automatically produce a valid part
   *     representing given file inside multipart request (including size, submittedFileName, etc.)
   * @param pathFunc function to generate the path to file inside variables. For example:
   *     <ul>
   *       <li>index -&gt; String.format("variables.files.%d", index) for multiple files
   *       <li>index -&gt; "variables.file" for single file
   *     </ul>
   *
   * @return {@link GraphQLResponse} containing the result of query execution
   * @throws IOException if the resource cannot be loaded from the classpath
   */
  public GraphQLResponse postFiles(
      String graphqlResource,
      ObjectNode variables,
      List<ClassPathResource> files,
      IntFunction<String> pathFunc)
      throws IOException {
    MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

    for (int i = 0; i < files.size(); i++) {
      String valueKey = String.valueOf(i + 1); // map value and part index starts at 1
      map.add(valueKey, pathFunc.apply(i));

      values.add(valueKey, files.get(i));
    }

    String payload = getPayload(graphqlResource, null, variables, Collections.emptyList());
    values.add("operations", payload);
    values.add("map", map);

    return postRequest(RequestFactory.forMultipart(values, headers));
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string.
   *
   * Operation name will be derived from the provided GraphQL query string.
   *
   * @param graphql the GraphQL query
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql) throws IOException {
    return postForString(graphql, getOperationName(graphql), ((ObjectNode) null));
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string and operation name.
   *
   * @param graphql the GraphQL query
   * @param operation the name of the GraphQL operation to be executed
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql, String operation) throws IOException {
    return postForString(graphql, operation, ((ObjectNode) null));
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string and variables.
   *
   * Operation name will be derived from the provided GraphQL query string.
   *
   * @param graphql the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql, Map<String, ?> variables) throws IOException {
    return postForString(graphql, getOperationName(graphql), variables);
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string, operation name, and
   * variables.
   *
   * @param graphql the GraphQL query
   * @param operation the name of the GraphQL operation to be executed
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql, String operation, Map<String, ?> variables) throws IOException {
    return postForString(graphql, operation, ((ObjectNode) new ObjectMapper().valueToTree(variables)));
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string and variables.
   *
   * Operation name will be derived from the provided GraphQL query string.
   *
   * @param graphql the GraphQL query
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql, ObjectNode variables) throws IOException {
    return post(createJsonQuery(graphql, getOperationName(graphql), variables));
  }

  /**
   * Performs a GraphQL request using the provided GraphQL query string, operation name, and
   * variables.
   *
   * @param graphql the GraphQL query
   * @param operation the name of the GraphQL operation to be executed
   * @param variables the input variables for the GraphQL query
   * @return {@link GraphQLResponse} containing the result of the query execution
   * @throws IOException if the request json cannot be created because of issues with one of the
   *         provided arguments
   */
  public GraphQLResponse postForString(String graphql, String operation, ObjectNode variables) throws IOException {
    requireNonNull(graphql, "GraphQL query string cannot be null");

    return post(createJsonQuery(graphql, operation, variables));
  }

  /**
   * Performs a GraphQL request with the provided payload.
   *
   * @param payload the GraphQL payload
   * @return {@link GraphQLResponse} containing the result of query execution
   */
  public GraphQLResponse post(String payload) {
    return postRequest(RequestFactory.forJson(payload, headers));
  }

  private GraphQLResponse postRequest(HttpEntity<Object> request) {
    ResponseEntity<String> response =
        restTemplate.exchange(graphqlMapping, HttpMethod.POST, request, String.class);
    return new GraphQLResponse(response, objectMapper);
  }
}
