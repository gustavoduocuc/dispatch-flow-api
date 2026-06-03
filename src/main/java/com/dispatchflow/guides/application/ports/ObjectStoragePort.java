package com.dispatchflow.guides.application.ports;

public interface ObjectStoragePort {

    void store(String key, byte[] content);

    void delete(String key);
}
