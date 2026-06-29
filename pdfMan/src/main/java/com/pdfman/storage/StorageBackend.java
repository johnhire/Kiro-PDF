package com.pdfman.storage;

public interface StorageBackend {

    /**
     * Store data under the given key.
     *
     * @param key         storage key (path/filename)
     * @param data        raw bytes to store
     * @param contentType MIME type of the data
     * @return the key used to store the data
     */
    String store(String key, byte[] data, String contentType);

    /**
     * Retrieve data by key.
     *
     * @param key storage key
     * @return raw bytes
     * @throws com.pdfman.exception.StorageException if the key does not exist or an I/O error occurs
     */
    byte[] retrieve(String key);

    /**
     * Delete data by key.
     *
     * @param key storage key
     * @throws com.pdfman.exception.StorageException if the key does not exist or an I/O error occurs
     */
    void delete(String key);
}
