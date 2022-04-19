package com.graphql.spring.boot.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.beans.FooBar;
import graphql.GraphQLError;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQLTestTemplateIntegrationTest {

  private static final String SIMPLE_TEST_QUERY = "simple-test-query.graphql";
  private static final String SIMPLE_TEST_QUERY_WITH_FRAGMENTS =
      "simple-test-query-with-fragments.graphql";
  private static final String TEST_FRAGMENT_FILE = "foo-bar-fragment.graphql";
  private static final String QUERY_WITH_VARIABLES = "query-with-variables.graphql";
  private static final String COMPLEX_TEST_QUERY = "complex-query.graphql";
  private static final String MULTIPLE_QUERIES = "multiple-queries.graphql";
  private static final String UPLOAD_FILES_MUTATION = "upload-files.graphql";
  private static final String UPLOAD_FILE_MUTATION = "upload-file.graphql";
  private static final String INPUT_STRING_VALUE = "input-value";
  private static final String INPUT_STRING_NAME = "input";
  private static final String INPUT_HEADER_NAME = "headerName";
  private static final String FILES_STRING_NAME = "files";
  private static final String UPLOADING_FILE_STRING_NAME = "uploadingFile";
  private static final String TEST_HEADER_NAME = "x-test";
  private static final String TEST_HEADER_VALUE = String.valueOf(UUID.randomUUID());
  private static final String FOO = "FOO";
  private static final String BAR = "BAR";
  private static final String TEST = "TEST";
  private static final String DATA_FIELD_FOO_BAR = "$.data.fooBar";
  private static final String DATA_FIELD_QUERY_WITH_VARIABLES = "$.data.queryWithVariables";
  private static final String DATA_FIELD_OTHER_QUERY = "$.data.otherQuery";
  private static final String DATA_FIELD_QUERY_WITH_HEADER = "$.data.queryWithHeader";
  private static final String DATA_FIELD_DUMMY = "$.data.dummy";
  private static final String DATA_FILE_UPLOAD_FILES = "$.data.uploadFiles";
  private static final String DATA_FILE_UPLOAD_FILE = "$.data.uploadFile";
  private static final String OPERATION_NAME_WITH_VARIABLES = "withVariable";
  private static final String OPERATION_NAME_TEST_QUERY_1 = "testQuery1";
  private static final String OPERATION_NAME_TEST_QUERY_2 = "testQuery2";
  private static final String OPERATION_NAME_COMPLEX_QUERY = "complexQuery";
  private static final String GRAPHQL_ENDPOINT = "/graphql";

  @Autowired private ResourceLoader resourceLoader;

  @Autowired private TestRestTemplate testRestTemplate;

  @Autowired private ObjectMapper objectMapper;

  private GraphQLTestTemplate graphQLTestTemplate;

  @BeforeEach
  void setUp() {
    graphQLTestTemplate =
        new GraphQLTestTemplate(resourceLoader, testRestTemplate, GRAPHQL_ENDPOINT, objectMapper);
  }

  @Test
  @DisplayName("Test postForResource with only the GraphQL resource provided.")
  void testPostForResource() throws IOException {
    graphQLTestTemplate
        .postForResource(SIMPLE_TEST_QUERY)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_OTHER_QUERY)
        .asString()
        .isEqualTo(TEST);
  }

  @Test
  @DisplayName("Test postForResource with fragments.")
  void testPostForResourceWithFragments() throws IOException {
    graphQLTestTemplate
        .postForResource(
            SIMPLE_TEST_QUERY_WITH_FRAGMENTS, Collections.singletonList(TEST_FRAGMENT_FILE))
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_FOO_BAR)
        .as(FooBar.class)
        .usingRecursiveComparison()
        .ignoringAllOverriddenEquals()
        .isEqualTo(FooBar.builder().foo(FOO).bar(BAR).build());
  }

  @Test
  @DisplayName("Test perform with variables.")
  void testPerformWithVariables() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);
    // WHEN - THEN
    graphQLTestTemplate
        .perform(QUERY_WITH_VARIABLES, variables)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test perform with variables and operation name")
  void testPerformWithOperationAndVariables() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);
    // WHEN - THEN
    graphQLTestTemplate
        .perform(MULTIPLE_QUERIES, OPERATION_NAME_WITH_VARIABLES, variables)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test perform with variables and fragments")
  void testPerformWithVariablesAndFragments() throws IOException {
    // GIVEN
    final FooBar expected =
        new FooBar(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
    final ObjectNode variables = objectMapper.valueToTree(expected);
    // WHEN - THEN
    graphQLTestTemplate
        .perform(
            SIMPLE_TEST_QUERY_WITH_FRAGMENTS,
            variables,
            Collections.singletonList(TEST_FRAGMENT_FILE))
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_FOO_BAR)
        .as(FooBar.class)
        .usingRecursiveComparison()
        .ignoringAllOverriddenEquals()
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("Test perform with operation name.")
  void testPerformWithOperationName() throws IOException {
    // WHEN - THEN
    graphQLTestTemplate
        .perform(MULTIPLE_QUERIES, OPERATION_NAME_TEST_QUERY_1)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_DUMMY)
        .asBoolean()
        .isTrue();
    graphQLTestTemplate
        .perform(MULTIPLE_QUERIES, OPERATION_NAME_TEST_QUERY_2)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_OTHER_QUERY)
        .asString()
        .isEqualTo(TEST);
  }

  @Test
  @DisplayName("Test perform with GraphQL errors.")
  void testPerformWithGraphQLError() throws IOException {
    graphQLTestTemplate
        .postForResource(SIMPLE_TEST_QUERY, Collections.singletonList(TEST_FRAGMENT_FILE))
        .assertThatDataField()
        .isNotPresentOrNull()
        .and()
        .assertThatNumberOfErrors()
        .isOne()
        .and()
        .assertThatListOfErrors()
        .extracting(GraphQLError::getMessage)
        .allMatch(message -> message.contains("UnusedFragment"));
  }

  @Test
  @DisplayName("Test perform with all possible inputs.")
  void testPerformWithAllInputs() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);
    variables.put(INPUT_HEADER_NAME, TEST_HEADER_NAME);
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(TEST_HEADER_NAME, TEST_HEADER_VALUE);
    // WHEN - THEN
    graphQLTestTemplate
        .withHeaders(httpHeaders)
        .perform(
            COMPLEX_TEST_QUERY,
            OPERATION_NAME_COMPLEX_QUERY,
            variables,
            Collections.singletonList(TEST_FRAGMENT_FILE))
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_HEADER)
        .asString()
        .isEqualTo(TEST_HEADER_VALUE)
        .and()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE)
        .and()
        .assertThatField(DATA_FIELD_FOO_BAR)
        .as(FooBar.class)
        .isEqualTo(new FooBar(FOO, BAR));
  }

  @Test
  @DisplayName("Test post with custom payload.")
  void testPost() {
    // GIVEN
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(TEST_HEADER_NAME, TEST_HEADER_VALUE);
    final String payload =
        "{\"query\":"
            + "\"query ($input: String!, $headerName: String!) "
            + "{ queryWithVariables(input: $input) queryWithHeader(headerName: $headerName) }\", "
            + "\"variables\": {\"input\": \"input-value\", \"headerName\": \"x-test\"}}";
    // WHEN - THEN
    graphQLTestTemplate
        .withHeaders(httpHeaders)
        .post(payload)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE)
        .and()
        .assertThatField(DATA_FIELD_QUERY_WITH_HEADER)
        .asString()
        .isEqualTo(TEST_HEADER_VALUE);
  }

  @Test
  @DisplayName("Test perform with file uploads.")
  void testPerformWithFileUploads() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    ArrayNode nodes = objectMapper.valueToTree(Arrays.asList(null, null));
    variables.putArray(FILES_STRING_NAME).addAll(nodes);

    List<String> fileNames = Arrays.asList("multiple-queries.graphql", "simple-test-query.graphql");
    List<ClassPathResource> testUploadFiles =
        fileNames.stream().map(ClassPathResource::new).collect(Collectors.toList());
    // WHEN - THEN
    graphQLTestTemplate
        .postFiles(UPLOAD_FILES_MUTATION, variables, testUploadFiles)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FILE_UPLOAD_FILES)
        .asListOf(String.class)
        .isEqualTo(fileNames);
  }

  @Test
  @DisplayName("Test perform with individual file upload and custom path.")
  void testPerformWithIndividualFileUpload() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(UPLOADING_FILE_STRING_NAME, objectMapper.valueToTree(null));

    List<String> fileNames = Arrays.asList("multiple-queries.graphql");
    List<ClassPathResource> testUploadFiles =
        fileNames.stream().map(ClassPathResource::new).collect(Collectors.toList());
    // WHEN - THEN
    graphQLTestTemplate
        .postFiles(UPLOAD_FILE_MUTATION, variables, testUploadFiles, index -> "variables.file")
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FILE_UPLOAD_FILE)
        .asString()
        .isEqualTo(fileNames.get(0));
  }

  @Test
  @DisplayName("Test postForString without operation name and without variables.")
  void testPostString() throws IOException {
    // GIVEN
    final String graphql = "query {\n"
            + "    otherQuery\n"
            + "}";
    // WHEN - THEN
    graphQLTestTemplate
        .postForString(graphql)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_OTHER_QUERY)
        .asString()
        .isEqualTo(TEST);
  }

  @Test
  @DisplayName("Test postForString with embedded operation name and without variables.")
  void testPostStringWithEmbeddedOperationName() throws IOException {
    // GIVEN
    final String graphql = "query TestOperationYo {\n"
        + "    otherQuery\n"
        + "}";
    // WHEN - THEN
    graphQLTestTemplate
        .postForString(graphql)
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_OTHER_QUERY)
        .asString()
        .isEqualTo(TEST);
  }

  @Test
  @DisplayName("Test postForString with explicit operation name and without variables.")
  void testPostStringWithExplicitOperationName() throws IOException {
    // GIVEN
    final String graphql = "query TestOperationYo {\n"
        + "    otherQuery\n"
        + "}";
    // WHEN - THEN
    graphQLTestTemplate
        .postForString(graphql, "TestOperationYo")
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_OTHER_QUERY)
        .asString()
        .isEqualTo(TEST);
  }

  @Test
  @DisplayName("Test postForString with fragments.")
  void testPostForStringWithFragments() throws IOException {
    graphQLTestTemplate
        .postForString(
            "query($foo: String, $bar: String) {\n"
                + "    fooBar(foo: $foo, bar: $bar) {\n"
                + "        ...FooBarFragment\n"
                + "    }\n"
                + "}"
                + "fragment FooBarFragment on FooBar {\n"
                + "    foo\n"
                + "    bar\n"
                + "}"
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_FOO_BAR)
        .as(FooBar.class)
        .usingRecursiveComparison()
        .ignoringAllOverriddenEquals()
        .isEqualTo(FooBar.builder().foo(FOO).bar(BAR).build());
  }

  @Test
  @DisplayName("Test postForString with ObjectNode variables.")
  void testPostForStringWithObjectNodeVariables() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);

    // WHEN - THEN
    graphQLTestTemplate
        .postForString(
            "query ($input: String!) {"
                + "    queryWithVariables(input: $input)"
                + "}",
            variables
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test postForString with Map variables.")
  void testPostForStringWithMapVariables() throws IOException {
    // GIVEN
    final Map<String, Object> variables = new LinkedHashMap<>();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);

    // WHEN - THEN
    graphQLTestTemplate
        .postForString(
            "query ($input: String!) {"
                + "    queryWithVariables(input: $input)"
                + "}",
            variables
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test postForString with embedded operation name and variables.")
  void testPostForStringWithEmbeddedOperationNameAndVariables() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);

    // WHEN - THEN
    graphQLTestTemplate
        .postForString(
            "query TestOperationYo ($input: String!) {"
                + "    queryWithVariables(input: $input)"
                + "}",
            variables
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test postForString with explicit operation name and ObjectNode variables.")
  void testPostForStringWithExplicitOperationNameAndObjectNodeVariables() throws IOException {
    // GIVEN
    final ObjectNode variables = objectMapper.createObjectNode();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);

    // WHEN - THEN
    graphQLTestTemplate
        .postForString(
            "query TestOperationYo ($input: String!) {"
                + "    queryWithVariables(input: $input)"
                + "}",
            "TestOperationYo",
            variables
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test postForString with explicit operation name and Map variables.")
  void testPostForStringWithExplicitOperationNameAndMapVariables() throws IOException {
    // GIVEN
    final Map<String, Object> variables = new LinkedHashMap<>();
    variables.put(INPUT_STRING_NAME, INPUT_STRING_VALUE);

    // WHEN - THEN
    graphQLTestTemplate
        .postForString(
            "query TestOperationYo ($input: String!) {"
                + "    queryWithVariables(input: $input)"
                + "}",
            "TestOperationYo",
            variables
        )
        .assertThatNoErrorsArePresent()
        .assertThatField(DATA_FIELD_QUERY_WITH_VARIABLES)
        .asString()
        .isEqualTo(INPUT_STRING_VALUE);
  }

  @Test
  @DisplayName("Test postForString with null string.")
  void testPostStringWithNullString() {
    // GIVEN
    final String graphql = null;

    // WHEN - THEN
    assertThrows(NullPointerException.class, () -> {
        graphQLTestTemplate.postForString(graphql);
    });
  }
}
