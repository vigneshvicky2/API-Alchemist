package com.api.automate.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EndpointDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String path;
    private String responseBody;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Use LAZY loading to improve performance
    @JoinColumn(name = "api_id", nullable = false)
    @JsonBackReference // ✅ Prevents circular reference
    private ApiDefinition apiDefinition;
}
