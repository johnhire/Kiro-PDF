package com.pdfman;

import com.pdfman.exception.StorageException;
import com.pdfman.storage.StorageBackend;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class MockStorageBackend implements StorageBackend {

    private final ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<>();
    private boolean shouldFail = false;

    @Override
    public String store(String key, byte[] data, String contentType) {
        if (shouldFail) {
            throw new StorageException("MockStorageBackend: simulated storage failure");
        }
        store.put(key, data);
        return key;
    }

    @Override
    public byte[] retrieve(String key) {
        if (shouldFail) {
            throw new StorageException("MockStorageBackend: simulated storage failure");
        }
        byte[] data = store.get(key);
        if (data == null) {
            throw new StorageException("MockStorageBackend: key not found: " + key);
        }
        return data;
    }

    @Override
    public void delete(String key) {
        if (shouldFail) {
            throw new StorageException("MockStorageBackend: simulated storage failure");
        }
        byte[] removed = store.remove(key);
        if (removed == null) {
            throw new StorageException("MockStorageBackend: key not found: " + key);
        }
    }

    /** Clears all stored data — call between tests to ensure isolation. */
    public void reset() {
        store.clear();
        shouldFail = false;
    }

    /** When set to true, all operations throw StorageException (for Property 15 testing). */
    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }
}

