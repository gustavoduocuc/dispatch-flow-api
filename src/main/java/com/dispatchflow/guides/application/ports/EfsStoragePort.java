package com.dispatchflow.guides.application.ports;

public interface EfsStoragePort {

    void write(String path, byte[] content);
}
