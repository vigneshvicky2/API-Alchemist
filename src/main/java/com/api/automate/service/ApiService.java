package com.api.automate.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.api.automate.model.ApiDefinition;
import com.api.automate.repository.ApiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ApiService {
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private ApiRepository apiRepository;

    public ApiDefinition createApi(ApiDefinition apiDefinition) {
        return apiRepository.save(apiDefinition);
    }

    public List<ApiDefinition> getAllApis() {
        return apiRepository.findAll();
    }

    public File generateApiCode(Long id) throws IOException {
        ApiDefinition apiDefinition = apiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API not found"));

        String entityName = apiDefinition.getEntityName();
        List<String> fields = apiDefinition.getFields();

        Path workingDir = Paths.get(System.getProperty("user.dir"), "generated-api");
        Files.createDirectories(workingDir);

        Path modelDir = workingDir.resolve("src/main/java/com/example/model");
        Path repositoryDir = workingDir.resolve("src/main/java/com/example/repository");
        Path serviceDir = workingDir.resolve("src/main/java/com/example/service");
        Path controllerDir = workingDir.resolve("src/main/java/com/example/controller");
        Path mainDir = workingDir.resolve("src/main/java/com/example");
        Path resourcesDir = workingDir.resolve("src/main/resources");

        Files.createDirectories(modelDir);
        Files.createDirectories(repositoryDir);
        Files.createDirectories(serviceDir);
        Files.createDirectories(controllerDir);
        Files.createDirectories(mainDir);
        Files.createDirectories(resourcesDir);

        String combinedCode = generateCombinedCode(entityName, fields);

        splitAndWriteFiles(combinedCode, modelDir, repositoryDir, serviceDir, controllerDir, mainDir, resourcesDir, workingDir, entityName);

        File zipFile = new File(System.getProperty("user.dir"), "generated-api.zip");
        zipDirectory(workingDir, zipFile);

        deleteDirectory(workingDir);

        return zipFile;
    }

    private String generateCombinedCode(String entityName, List<String> fields) throws IOException {
        String prompt = "Generate a Spring Boot application for entity " + entityName +
                " with fields: " + fields + ". Include model, repository, service, controller, main application, application.properties, and pom.xml.\n\n" +
                "//---MODEL---\n(Model code here) Use com.example.model.* for all imports. use jakarta.persistence.* for all imports model code .\n\n" +
                "//---REPOSITORY---\n(Repository code here) Use com.example.repository.* for all imports.\n\n" +
                "//---SERVICE---\n(Service code here) Use com.example.service.* for all imports.\n\n" +
                "//---CONTROLLER---\n(Controller code here) Use com.example.controller.* for all imports.\n\n" +
                "//---APPLICATION---\n(Spring Boot main application class code here) Use com.example.* for all imports. Do not include application.properties content.\n\n" +
                "//---APPLICATION_PROPERTIES---\n" +
                "spring.application.name=student-app\n" +
                "spring.datasource.url=jdbc:h2:mem:testdb\n" +
                "spring.datasource.driverClassName=org.h2.Driver\n" +
                "spring.datasource.username=sa\n" +
                "spring.datasource.password=\n" +
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect\n" +
                "spring.h2.console.enabled=true\n" +
                "spring.jpa.defer-datasource-initialization=true\n\n" +
                "//---POM.XML---\n(pom.xml content here)";
        return callGeminiAPI(prompt);
    }

    private void splitAndWriteFiles(String combinedCode, Path modelDir, Path repositoryDir, Path serviceDir, Path controllerDir, Path mainDir, Path resourcesDir, Path workingDir, String entityName) throws IOException {
        String[] parts = combinedCode.split("//---");

        for (String part : parts) {
            if (part.contains("MODEL")) {
                generateAndWriteFile(modelDir, entityName + ".java", extractCode(part));
            } else if (part.contains("REPOSITORY")) {
                generateAndWriteFile(repositoryDir, entityName + "Repository.java", extractCode(part));
            } else if (part.contains("SERVICE")) {
                generateAndWriteFile(serviceDir, entityName + "Service.java", extractCode(part));
            } else if (part.contains("CONTROLLER")) {
                generateAndWriteFile(controllerDir, entityName + "Controller.java", extractCode(part));
            } else if (part.contains("APPLICATION")) {
                generateAndWriteFile(mainDir, "Application.java", extractCode(part));
            } else if (part.contains("APPLICATION_PROPERTIES")) {
                generateAndWriteFile(resourcesDir, "application.properties", extractCode(part));
            } else if (part.contains("POM.XML")) {
                generateAndWriteFile(workingDir, "pom.xml", extractCode(part));
            }
        }
    }

    private String extractCode(String part) {
        return part.substring(part.indexOf("\n") + 1).trim();
    }

    private String callGeminiAPI(String prompt) throws IOException {
        String apiKey = geminiApiKey;
        String geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        String requestBody = String.format("""
                {
                  "contents": [{
                    "parts": [{"text": "%s"}]
                  }]
                }
                """, "Write only the Java code. Do NOT include explanations. Do NOT add comments.\n" + prompt);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geminiEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            if (rootNode.has("error")) {
                throw new RuntimeException("Gemini API error: " + rootNode.path("error").path("message").asText());
            }

            String generatedCode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            generatedCode = generatedCode.replaceAll("```java", "").replaceAll("```xml", "").trim();
            int javaIndex = generatedCode.lastIndexOf("java");
            int xmlIndex = generatedCode.lastIndexOf("xml");
            int lastIndex = Math.max(javaIndex, xmlIndex);

            if (lastIndex != -1) {
                generatedCode = generatedCode.substring(0, lastIndex);
            }

            return generatedCode.trim();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private void generateAndWriteFile(Path directory, String fileName, String content) throws IOException {
        Files.writeString(directory.resolve(fileName), content);
    }

    private void zipDirectory(Path sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try (FileInputStream fis = new FileInputStream(path.toFile())) {
                            zipOut.putNextEntry(new ZipEntry(sourceDir.relativize(path).toString()));
                            zipOut.write(Files.readAllBytes(path));
                            zipOut.closeEntry();
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory).sorted((a, b) -> b.compareTo(a)).map(Path::toFile).forEach(File::delete);
    }
}