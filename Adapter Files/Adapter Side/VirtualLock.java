package com.uppaal.tron;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * VirtualLock replaces the Lock when running within virtual time framework.
 * If the VirtualThread is configured to run without virtual clock,
 * then internal ReentrantLock (and its Conditions) is created.
 * Only threads originating from VirtualThread object can create and manipulate
 * instances of VirtualLock.
 *
 *@author Marius Mikucionis <marius@cs.aau.dk>
 *@see VirtualThread
 *@see VirtualCondition
 *@see java.util.concurrent.locks.Lock
 *@see java.util.concurrent.locks.Condition
 */
public class VirtualLock implements Lock
{
    /**
     * The id of this lock in a remote virtual clock process.
     */
    protected final int id;
    /**
     * Current owner thread of this lock, null if not owned.
     */
    protected VirtualThread owner;
    private final ReentrantLock l;
    private final String name;

    /**
     * Creates a virtual lock which is located at virtual clock process.
     * @param name specify your friendly name to identify it in debug logs.
     * @see VirtualThread#DBG
     */
    public VirtualLock(String name)
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	id = t.mutexInit();
	if (id < 0) l = new ReentrantLock();
	else l = null;
	owner = null;
	this.name = name;
    }

    /**
     * Creates a virtual lock which is located at virtual clock process.
     * The name will be inherited from the creating VirtualThread with "-lock"
     * appended.
     * @see VirtualThread#DBG
     */
    public VirtualLock()
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	id = t.mutexInit();
	if (id < 0) l = new ReentrantLock();
	else l = null;
	owner = null;
	name = t.getName()+"-lock";
    }

    /**
     * Returns the name of this lock.
     * @return name of this lock.
     */
    public String getName() { return name; }
    /**
     * Returns the owner thread of the lock if lock is acquired, returns null
     * if lock is not owned/acquired.
     * @return owner thread that holds this lock.
     */
    public VirtualThread getOwner(){ return owner; }
    /**
     * Sets the owner of this lock. This method should be used with caution,
     * currently only VirtualCondition has reasonable use of this method.
     */
    public void setOwner(VirtualThread t) { owner = t; }
    /**
     * Checks whether this lock is acquired/owned/locked.
     * @return true if the lock is taken.
     */
    public boolean isLocked(){ return (owner!=null); }

    /**
     * Acquires the ownership of this lock. 
     * If the lock is already taken, then the calling thread blocks until 
     * the lock is released.
     */
    public void lock()
    {
	if (l != null) l.lock();
	else {
	    VirtualThread t = (VirtualThread)Thread.currentThread();
	    t.mutexLock(this);
	    owner = t;// set reference after gaining mutex
	}
    }
    /**
     * Not implemented in virtual time framework.
     */
    public void lockInterruptibly() {
	throw new UnsupportedOperationException("not implemented");
    }
    /**
     * Not implemented in virtual time framework.
     */
    public boolean tryLock() {
	throw new UnsupportedOperationException("not implemented");
    }
    /**
     * Not implemented in virtual time framework.
     */
    public boolean tryLock(long time, TimeUnit unit) {
	throw new UnsupportedOperationException("not implemented");
    }
    /**
     * Creates a condition variable associated with this lock.
     * The condition should always be used in association with this lock, 
     * i.e. the thread should wait on this condition only when this lock is 
     * owned by the thread, the same applies to signaling (otherwise the signal
     * migt potentially be lost).
     * @return condition variable association with this lock.
     */
    public Condition newCondition()
    {
	if (l != null) return l.newCondition();
	else return new VirtualCondition(this);
    }
    /**
     * Creates a named condition variable associated with this lock.
     * The condition should always be used in association with this lock, 
     * i.e. the thread should wait on this condition only when this lock is 
     * owned by the thread, the same applies to signaling (otherwise the signal
     * migt potentially be lost).
     * @param name is the name of the condition that appears in virtual thread 
     * debug logs.
     * @return condition variable association with this lock.
     * @see VirtualThread#DBG
     */
    public Condition newCondition(String name)
    {
	if (l != null) return l.newCondition();
	else return new VirtualCondition(this, name);
    }

    /**
     * Releases this lock and lets any waiting thread to acquire it.
     */
    public void unlock()
    {
	if (l != null) l.unlock();
	else {
	    VirtualThread t = (VirtualThread)Thread.currentThread();
	    if (t != owner)
		throw new IllegalMonitorStateException("caller is not owner");
	    owner = null; // clear reference while protected by mutex
	    t.mutexUnlock(this);
	}
    }
}
