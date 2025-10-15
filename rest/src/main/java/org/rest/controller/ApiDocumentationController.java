package org.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
public class ApiDocumentationController {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    @GetMapping(value = "/openapi.yaml", produces = "application/x-yaml")
    public ResponseEntity<String> getOpenApiSpec() {
        try {
            Resource resource = new ClassPathResource("openapi.yaml");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-yaml"))
                    .body(content);
        } catch (IOException e) {
            log.error("Error reading OpenAPI specification", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/api-docs")
    public ResponseEntity<String> getApiDocs() {
        try {
            Resource resource = new ClassPathResource("openapi.yaml");
            String yamlContent = resource.getContentAsString(StandardCharsets.UTF_8);
            String jsonContent = convertYamlToJson(yamlContent);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonContent);
        } catch (IOException e) {
            log.error("Error reading OpenAPI specification", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String convertYamlToJson(String yamlContent) {
        try {
            Object yamlObject = yamlMapper.readValue(yamlContent, Object.class);
            return jsonMapper.writeValueAsString(yamlObject);
        } catch (Exception e) {
            log.error("Error converting YAML to JSON", e);
            return yamlContent; // Fall back to YAML if conversion fails
        }
    }
}