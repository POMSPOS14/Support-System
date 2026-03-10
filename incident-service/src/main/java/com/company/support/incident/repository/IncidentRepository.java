package com.company.support.incident.repository;

import com.company.support.incident.entity.Incident;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IncidentRepository implements PanacheRepositoryBase<Incident, Long> {
}
