package net.ramixin.mixson;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class MixsonRegistry<T> {
    
    private final Map<UUID, T> valuesById = new HashMap<>();
    private final Map<UUID, T> deferredValuesById = new HashMap<>();
    private final SortedMap<Integer, List<T>> valuesByPriority = new TreeMap<>();
    private final Function<T, UUID> idFunction;
    private final Function<T, Integer> priorityFunction;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public MixsonRegistry(Function<T, UUID> idFunction, Function<T, Integer> priorityFunction) {
        this.idFunction = idFunction;
        this.priorityFunction = priorityFunction;
    }
    
    public UUID register(T value) {
        lock.writeLock().lock();

        int priority = priorityFunction.apply(value);
        UUID uuid = idFunction.apply(value);
        valuesById.put(uuid, value);
        List<T> componentSet;
        if(valuesByPriority.get(priority) == null) componentSet = new ArrayList<>();
        else componentSet = valuesByPriority.get(priority);
        componentSet.add(value);
        valuesByPriority.put(priority, componentSet);

        lock.writeLock().unlock();
        return uuid;
    }

    public UUID registerDeferred(T value) {
        lock.writeLock().lock();

        UUID uuid = idFunction.apply(value);
        deferredValuesById.put(uuid, value);

        lock.writeLock().unlock();
        return uuid;
    }
    
    public Optional<T> pullDeferred(UUID uuid) {
        lock.writeLock().lock();

        T value = deferredValuesById.remove(uuid);

        lock.writeLock().unlock();
        return Optional.ofNullable(value);
    }

    public Optional<T> get(UUID uuid) {
        lock.readLock().lock();

        T value = valuesById.get(uuid);

        lock.readLock().unlock();
        return Optional.ofNullable(value);
    }

    public boolean contains(UUID uuid) {
        lock.readLock().lock();

        boolean contains = valuesById.containsKey(uuid);

        lock.readLock().unlock();
        return contains;
    }
    
    public boolean unregister(UUID value) {
        lock.writeLock().lock();
        
        T val = valuesById.remove(value);
        boolean returnVal;
        if(val == null)
            returnVal = false;
        else {
            returnVal = true;
            int priority = priorityFunction.apply(val);
            List<T> componentSet = valuesByPriority.get(priority);
            boolean removed = componentSet.remove(val);
            if(!removed)
                throw new IllegalStateException("Found component by UUID, but not present by priority");
        }
        
        lock.writeLock().unlock();
        return returnVal;
    }
    
    public SortedMap<Integer, List<T>> captureSnapshot() {
        lock.readLock().lock();
        
        SortedMap<Integer, List<T>> snapshot = new TreeMap<>();
        for(Map.Entry<Integer, List<T>> entry : valuesByPriority.entrySet()) {
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        
        lock.readLock().unlock();
        return snapshot;
    }
}
