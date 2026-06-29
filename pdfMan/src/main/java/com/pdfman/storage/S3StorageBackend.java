package com.pdfman.storage;

import com.pdfman.exception.StorageException;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * S3 storage backend stub.
 * Full implementation requires the quarkus-amazon-s3 extension on the classpath.
 * Activated only when app.storage.backend=s3 is configured.
 */
@ApplicationScoped
@IfBuildProfile("s3")
public class S3StorageBackend implements StorageBackend {

    @ConfigProperty(name = "storage.s3.bucket-name", defaultValue = "pdfman")
    String bucketName;

    @Override
    public String store(String key, byte[] data, String contentType) {
        throw new StorageException("S3 storage backend is not available in this build. "
                + "Enable the quarkus-amazon-s3 extension for production use.");
    }

    @Override
    public byte[] retrieve(String key) {
        throw new StorageException("S3 storage backend is not available in this build. "
                + "Enable the quarkus-amazon-s3 extension for production use.");
    }

    @Override
    public void delete(String key) {
        throw new StorageException("S3 storage backend is not available in this build. "
                + "Enable the quarkus-amazon-s3 extension for production use.");
    }
}
