package ru.qatools.mongodb;

import ru.qatools.mongodb.error.ConcurrentReadWriteException;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Ilya Sadykov
 */
public interface PessimisticRepo<T extends Serializable> {
    /**
     * Get and lock
     */
    T tryLockAndGet(String key, long timeoutMs) throws LockWaitTimeoutException, ConcurrentReadWriteException;

    /**
     * Put and unlock
     */
    void putAndUnlock(String key, T object) throws InvalidLockOwnerException, ConcurrentReadWriteException;

    /**
     * Remove and unlock
     */
    void removeAndUnlock(String key) throws InvalidLockOwnerException;

    /**
     * Remove without locking
     */
    void remove(String key);

    /**
     * Put without locking
     */
    void put(String key, T object);

    /**
     * Get without locking
     */
    T get(String key);

    /**
     * Returns keys set
     */
    Set<String> keySet();

    /**
     * Returns internal lock
     */
    PessimisticLocking getLock();
}
