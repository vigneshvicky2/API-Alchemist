package com.api.automate.model;


public class ApiRequest {
    private Long id;
    private String entityName;
    private String fields;

    // Constructors
    public ApiRequest() {}

    public ApiRequest(Long id, String entityName, String fields) {
        this.id = id;
        this.entityName = entityName;
        this.fields = fields;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }
}
