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

    // ========================== CREATE API ==========================
    public ApiDefinition createApi(ApiDefinition apiDefinition) {
        return apiRepository.save(apiDefinition);
    }

    // ========================== GET ALL APIs ==========================
    public List<ApiDefinition> getAllApis() {
        return apiRepository.findAll();
    }

    // ========================== GENERATE API CODE ==========================
    public File generateApiCode(Long id) throws IOException {
        // 🔹 Fetch API definition from database
        ApiDefinition apiDefinition = apiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API not found"));

        String entityName = apiDefinition.getEntityName();
        List<String> fields = apiDefinition.getFields();

        // 🔹 Create working directory in the project root
        Path workingDir = Paths.get(System.getProperty("user.dir"), "generated-api");
        Files.createDirectories(workingDir);

        // 🔹 Generate all necessary files using Gemini API
        generateAndWriteFile(workingDir, entityName + ".java", generateModel(entityName, fields));
        generateAndWriteFile(workingDir, entityName + "Repository.java", generateRepository(entityName));
        generateAndWriteFile(workingDir, entityName + "Service.java", generateService(entityName, fields));
        generateAndWriteFile(workingDir, entityName + "Controller.java", generateController(entityName, fields));
        generateAndWriteFile(workingDir, "Application.java", generateMainClass(entityName));
        generateAndWriteFile(workingDir, "application.properties", generateApplicationProperties());

        // 🔹 Create a ZIP file in the project root
        File zipFile = new File(System.getProperty("user.dir"), "generated-api.zip");
        zipDirectory(workingDir, zipFile);

        // 🔹 Cleanup temporary files (optional)
        deleteDirectory(workingDir);

        return zipFile;
    }

    // ========================== GENERATE FILES USING GEMINI ==========================
    private String generateModel(String entityName, List<String> fields) throws IOException {
        String prompt = "Generate a Java Spring Boot model for entity " + entityName +
                " with fields: " + fields + ". Include Lombok annotations and correct imports. Dont include other codes here only include model code and import all required files in name of com.example.{filename} file name is like controller, service etc..,";
        return callGeminiAPI(prompt);
    }

    private String generateRepository(String entityName) throws IOException {
        String prompt = "Generate a Spring Boot JPA repository interface for entity " + entityName + "Dont include other codes here only include repository code and import all required files in name of com.example.{filename} file name is like controller, service etc..,.";
        return callGeminiAPI(prompt);
    }

    private String generateService(String entityName, List<String> fields) throws IOException {
        String prompt = "Generate a Spring Boot service class for entity " + entityName +
                " with CRUD operations for fields: " + fields + "Dont include other codes here only include service code and import all required files in name of com.example.{filename} file name is like controller, service etc..,";
        return callGeminiAPI(prompt);
    }

    private String generateController(String entityName, List<String> fields) throws IOException {
        String prompt = "Generate a Spring Boot REST controller for entity " + entityName +
                " with CRUD endpoints for fields: " + fields + "Dont include other codes here only include controller code and import all required files in name of com.example.{filename} file name is like controller, service etc..,";
        return callGeminiAPI(prompt);
    }

    private String generateMainClass(String entityName) throws IOException {
        String prompt = "Generate a Spring Boot main application class. Dont include other codes here only include main file code and import all required files in name of com.example.file.{filename} file name is like controller, service etc..,";
        return callGeminiAPI(prompt);
    }

    private String generateApplicationProperties() throws IOException {
        String prompt = "Generate a basic application.properties file for a Spring Boot application.";
        return callGeminiAPI(prompt);
    }

    // ========================== CALL GEMINI API ==========================
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

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Error calling Gemini API: " + e.getMessage(), e);
        }

        // Parse JSON response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());

        // Check for errors
        if (rootNode.has("error")) {
            throw new RuntimeException("Gemini API error: " + rootNode.path("error").path("message").asText());
        }

        // Extract generated content
        
        String generatedCode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        // 🔹 Remove Markdown Formatting
            generatedCode = generatedCode.replaceAll("^```java\\s*", "")  // Remove leading ```java
            .replaceAll("```$", "")        // Remove trailing ```
            .trim();                        // Trim extra spaces

        return generatedCode;
    }

    // ========================== WRITE GENERATED FILE ==========================
    private void generateAndWriteFile(Path directory, String fileName, String content) throws IOException {
        Path filePath = directory.resolve(fileName);
        Files.writeString(filePath, content);
    }

    // ========================== ZIP DIRECTORY ==========================
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

    // ========================== DELETE DIRECTORY ==========================
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
             .sorted((a, b) -> b.compareTo(a))
             .map(Path::toFile)
             .forEach(File::delete);
    }
}
