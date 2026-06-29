package com.pdfman.storage;

import com.pdfman.exception.StorageException;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
@DefaultBean
public class FilesystemStorageBackend implements StorageBackend {

    @ConfigProperty(name = "app.storage.filesystem.base-dir", defaultValue = "./documents")
    String baseDir;

    @Override
    public String store(String key, byte[] data, String contentType) {
        Path target = Paths.get(baseDir, key);
        System.out.println("FilesystemStorageBackend.store() - baseDir: " + baseDir + ", key: " + key + ", target: " + target.toAbsolutePath());
        try {
            Files.createDirectories(target.getParent() != null ? target.getParent() : target);
            Files.write(target, data);
        } catch (IOException e) {
            System.out.println("FilesystemStorageBackend.store() FAILED: " + e.getMessage());
            throw new StorageException("Failed to store file at key: " + key, e);
        }
        return key;
    }

    @Override
    public byte[] retrieve(String key) {
        Path target = Paths.get(baseDir, key);
        try {
            return Files.readAllBytes(target);
        } catch (NoSuchFileException e) {
            throw new StorageException("File not found for key: " + key, e);
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve file at key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        Path target = Paths.get(baseDir, key);
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                throw new StorageException("File not found for key: " + key);
            }
        } catch (StorageException e) {
            throw e;
        } catch (IOException e) {
            throw new StorageException("Failed to delete file at key: " + key, e);
        }
    }
}
