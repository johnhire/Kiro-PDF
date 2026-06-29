package com.pdfman.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "document")
public class DocumentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    /**
     * The JSON String template associated with this document. This field is required
     * and establishes a many-to-one relationship with the TemplateEntity.
     */
    public TemplateEntity template;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(length = 1000)
    public String description;

    @Column(name = "json_origin_string", columnDefinition = "TEXT")
    public String jsonOriginString;

    @Column(name = "document_html", columnDefinition = "TEXT")
    public String documentHtml;

    @Column(name = "storage_key", nullable = false, length = 512)
    public String storageKey;

    /*
    * audit fields
    */
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
}
