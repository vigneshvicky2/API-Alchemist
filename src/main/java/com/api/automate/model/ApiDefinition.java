package com.api.automate.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ApiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityName;
    private String basePath;

    @ElementCollection
    @CollectionTable(name = "api_fields", joinColumns = @JoinColumn(name = "api_id"))
    @Column(name = "field")
    private List<String> fields = new ArrayList<>();

    @OneToMany(mappedBy = "apiDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // ✅ Prevents circular reference
    private List<EndpointDefinition> endpoints = new ArrayList<>();

    // ✅ Helper method to set bidirectional relationship
    public void addEndpoint(EndpointDefinition endpoint) {
        endpoints.add(endpoint);
        endpoint.setApiDefinition(this); // Ensuring consistency
    }

    public void removeEndpoint(EndpointDefinition endpoint) {
        endpoints.remove(endpoint);
        endpoint.setApiDefinition(null);
    }
}
