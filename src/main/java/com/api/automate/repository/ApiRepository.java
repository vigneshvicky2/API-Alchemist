package com.api.automate.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.automate.model.ApiDefinition;

@Repository
public interface ApiRepository extends JpaRepository<ApiDefinition, Long> {
}
