package com.brcsrc.yaws;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:file:openapi-specgen?mode=memory&cache=shared",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.defer-datasource-initialization=true"
})
public class OpenApiSpecGeneratorTest {

    @Autowired
    private OpenApiWebMvcResource openApiResource;

    @Test
    void generateOpenApiSpec() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v3/api-docs");

        byte[] specBytes = openApiResource.openapiJson(request, "/v3/api-docs", null);

        Path outputDir = Paths.get("build");
        Files.createDirectories(outputDir);
        Files.write(outputDir.resolve("openapi.json"), specBytes);
    }
}
