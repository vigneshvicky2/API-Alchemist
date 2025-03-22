package com.api.automate.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.automate.model.ApiDefinition;
import com.api.automate.service.ApiService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/generator")
public class ApiController {

    private final ApiService apiService;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping("/create")
    public ApiDefinition createApi(@RequestBody ApiDefinition apiDefinition) {
        logger.info("Creating API: {}", apiDefinition.getEntityName());
        return apiService.createApi(apiDefinition);
    }

    @GetMapping("/list")
    public List<ApiDefinition> getAllApis() {
        logger.info("Fetching all APIs");
        return apiService.getAllApis();
    }

    @PostMapping("/generate/{id}")
    public ResponseEntity<?> generateApi(@PathVariable Long id) {
        try {
            logger.info("Generating API with id: {}", id);
            File zipFile = apiService.generateApiCode(id);

            if (zipFile != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=" + zipFile.getName());

                byte[] data = Files.readAllBytes(zipFile.toPath());
                logger.info("✅ File generated at: {}", zipFile.getAbsolutePath());
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(data);
            } else {
                logger.error("❌ Failed to generate file");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate API");
            }
        } catch (IOException e) {
            logger.error("❌ Error while generating file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while generating API: " + e.getMessage());
        }
    }

    @GetMapping("/testGenerate/{id}")
    public String testGenerate(@PathVariable Long id) {
        try {
            logger.info("Testing API generation for id: {}", id);
            File zipFile = apiService.generateApiCode(id);
            if (zipFile != null) {
                logger.info("✅ File generated at: {}", zipFile.getAbsolutePath());
                return "File generated at: " + zipFile.getAbsolutePath();
            } else {
                logger.error("❌ Failed to generate file");
                return "Failed to generate file";
            }
        } catch (Exception e) {
            logger.error("❌ Error while generating file: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
