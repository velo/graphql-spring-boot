package com.graphql.spring.boot.test.beans;

import graphql.kickstart.servlet.apollo.ApolloScalars;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.Part;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class DummyMutation implements GraphQLMutationResolver {

  @Bean
  private GraphQLScalarType getUploadScalar() {
    // since the test doesn't inject this built-in Scalar,
    // so we inject here for test run purpose
    return ApolloScalars.Upload;
  }

  public List<String> uploadFiles(List<Part> files, DataFetchingEnvironment env) {
    List<Part> actualFiles = env.getArgument("files");
    return actualFiles.stream().map(Part::getSubmittedFileName).collect(Collectors.toList());
  }

  public String uploadFile(Part file, DataFetchingEnvironment env) {
    Part actualFile = env.getArgument("file");
    return actualFile.getSubmittedFileName();
  }
}
