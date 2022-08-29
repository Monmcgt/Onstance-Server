package me.monmcgt.code.onstance.server.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class InstanceList {
    private final List<Instance> instances;

    public InstanceList() {
        this.instances = new ArrayList<>();
    }

    public boolean isAlreadyExists(String id) {
        return this.instances.stream().anyMatch((instance) -> instance.getId().equals(id));
    }

    public synchronized void addInstance(Instance instance) {
        this.getInstances().add(instance);
    }

    public synchronized void removeInstance(Instance instance) {
        this.getInstances().remove(instance);
    }

    public synchronized boolean isInstanceExists(Instance instance) {
        return this.getInstances().contains(instance);
    }

    public synchronized List<Instance> getInstances() {
        return this.instances;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Instance {
        private String id;
    }
}
