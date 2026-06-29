package com.pdfman.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Entity
@Table(name = "template")
public class TemplateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "template_type", nullable = false, unique = true, length = 255)
    public String templateType;

    @Column(length = 1000)
    public String description;

    @Column(name = "json_template_string", nullable = false, columnDefinition = "TEXT")
    public String jsonTemplateString;

    @Column(name = "html_template", nullable = false, columnDefinition = "TEXT")
    public String htmlTemplate;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at")
    public OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 255)
    public String createdBy;

    @Column(name = "updated_by", length = 255)
    public String updatedBy;

    @PrePersist
    public void onPrePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        createdBy = (createdBy == null) ? "system" : createdBy;
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        updatedBy = (updatedBy == null) ? "system" : updatedBy;
    }

    public static Optional<TemplateEntity> findByTemplateType(String templateType) {
        return find("templateType", templateType).firstResultOptional();
    }
}
