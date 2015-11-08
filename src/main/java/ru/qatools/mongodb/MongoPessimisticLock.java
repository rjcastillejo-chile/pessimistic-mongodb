package ru.qatools.mongodb;

import ru.qatools.mongodb.error.LockWaitTimeoutException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticLock implements Lock {
    final PessimisticLocking lock;
    final String uuid = UUID.randomUUID().toString();

    public MongoPessimisticLock(PessimisticLocking lock) {
        this.lock = lock;
    }

    public boolean isLockedByCurrentThread(){
        return lock.isLockedByMe(uuid);
    }

    @Override
    public void lock() {
        lock.tryLock(uuid, Long.MAX_VALUE);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new NotImplementedException();
    }

    @Override
    public boolean tryLock() {
        try {
            lock.tryLock(uuid, 0);
            return true;
        } catch (LockWaitTimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            lock.tryLock(uuid, unit.toMillis(time));
            return true;
        } catch (LockWaitTimeoutException e) {
            return false;
        }
    }

    @Override
    public void unlock() {
        lock.unlock(uuid);
    }

    @Override
    public Condition newCondition() {
        throw new NotImplementedException();
    }
}
