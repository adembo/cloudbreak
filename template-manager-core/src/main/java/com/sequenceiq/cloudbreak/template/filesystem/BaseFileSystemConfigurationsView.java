package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Collection;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public class BaseFileSystemConfigurationsView implements ProvisionEntity {

    private String storageContainer;

    private final boolean defaultFs;

    private final String type;

    private final Collection<StorageLocationView> locations;

    protected BaseFileSystemConfigurationsView(String type, Collection<StorageLocationView> locations) {
        this.type = type;
        this.defaultFs = false;
        this.locations = locations;
    }

    protected BaseFileSystemConfigurationsView(String type, String storageContainer, boolean defaultFs, Collection<StorageLocationView> locations) {
        this.type = type;
        this.storageContainer = storageContainer;
        this.defaultFs = defaultFs;
        this.locations = locations;
    }

    public String getStorageContainer() {
        return storageContainer;
    }

    public void setStorageContainer(String storageContainer) {
        this.storageContainer = storageContainer;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public Collection<StorageLocationView> getLocations() {
        return locations;
    }

    public String getType() {
        return type;
    }
}

