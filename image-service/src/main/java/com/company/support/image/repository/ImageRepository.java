package com.company.support.image.repository;

import com.company.support.image.entity.Image;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ImageRepository implements PanacheRepositoryBase<Image, Long> {

    public Uni<List<Image>> findByIncidentId(Long incidentId) {
        return list("incidentId", incidentId);
    }

    public Uni<Long> deleteByIncidentId(Long incidentId) {
        return delete("incidentId", incidentId);
    }
}
