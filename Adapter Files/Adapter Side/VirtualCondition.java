package com.uppaal.tron;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * VirtualCondition replaces the Condition when running within virtual time
 * framework. If the VirtualThread is configured to run without virtual clock,
 * then the local Condition from ReentrantLock is created.
 * Only threads originating from VirtualThread can create and manipulate
 * instances of VirtualCondition.
 *
 *@author Marius Mikucionis <marius@cs.aau.dk>
 *@see VirtualLock#newCondition
 *@see VirtualThread
 *@see java.util.concurrent.locks.Condition
 */
public class VirtualCondition implements Condition
{
    /**
     * The id of this condition in a remote virtual clock process.
     */
    protected final int id;
    /**
     * The associated lock with this condition.
     */
    protected final VirtualLock lock;
    /**
     * Name of this condition. Helps debugging deadlocks.
     * @see VirtualThread#DBG
     */
    private final String name;
    /**
     * Creates virtual condition associated with a lock.
     * @param lock the lock to associate with.
     */
    public VirtualCondition(VirtualLock lock)
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	id = t.condInit();
	this.lock = lock;
	name = lock.getName()+"-cond";
    }
    /**
     * Creates virtual condition associated with a lock.
     * @param lock the lock to associate with.
     * @param name your frendly name of condition to appear in debug output.
     * @see VirtualThread#DBG
     */
    public VirtualCondition(VirtualLock lock, String name)
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	id = t.condInit();
	this.lock = lock;
	this.name = name;
    }
    /**
     * Returns the name of this condition.
     * @return name of this condition used in virtual clock debug logs.
     */
    public String getName() { return name; }

    /**
     * Blocks the calling thread, releases the associated lock and waits until
     * the condition is signalled. Thread resumes when condition is signalled
     * and the associated lock is reacquired.
     * @see Condition#await
     */
    public void await() throws InterruptedException
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	if (lock.getOwner() != t)
	    throw new IllegalMonitorStateException("waiter is not lock owner");
	lock.setOwner(null);
	t.condWait(this);
	lock.setOwner(t);
    }

    /**
     * Blocks the calling thread, releases the associated lock and waits until
     * the condition is signalled or the specified time delay has ellapsed.
     * Thread resumes when condition is signalled or the time ellapses and
     * the associated lock is reacquired.
     * @param time specifies the duration of time delay.
     * @param unit specifies the time units the time delay is in.
     * @return true if the condition was signaled, otherwise false for timeout.
     * @see Condition#await(long,TimeUnit)
     */
    public boolean await(long time, TimeUnit unit) throws InterruptedException
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	if (lock.getOwner() != t)
	    throw new IllegalMonitorStateException("waiter is not lock owner");
	boolean res;
	lock.setOwner(null);
	res = t.condDelay(this, time, unit);
	lock.setOwner(t);
	return res;
    }
    /**
     * Not implemented in virtual time framework.
     * @see Condition#awaitNanos
     */
    public long awaitNanos(long nanosTimeout) throws InterruptedException
    {
	throw new UnsupportedOperationException("not implemented");
    }
    /**
     * Not implemented in virtual time framework.
     * @see Condition#awaitUninterruptibly
     */
    public void awaitUninterruptibly(){
	throw new UnsupportedOperationException("not implemented");
    }
    /**
     * Blocks the calling thread and waits until the condition is signalled or
     * the specified deadline has ellapsed.
     * @param deadline specifies the moment in time.
     * @return true if condition was signaled, otherwise false when deadline 
     * has been reached.
     * @see Condition#awaitUntil
     */
    public boolean awaitUntil(Date deadline) throws InterruptedException
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	if (lock.getOwner() != t)
	    throw new IllegalMonitorStateException("waiter is not lock owner");
	boolean res;
	lock.setOwner(null);
	res = t.condWait(this, deadline.getTime());
	lock.setOwner(t);
	return res;
    }
    /**
     * Selects one thread waiting for this condition and signals it to
     * reacquire the associated lock.
     * @see Condition#signal
     */
    public void signal()
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	if (lock.getOwner() != t)
	    throw new IllegalMonitorStateException("thread is not lock owner");
	t.condSignal(this);
    }
    /**
     * Signals all threads waiting for this condition and lets them to
     * reacquire the associated lock.
     * @see Condition#signalAll
     */
    public void signalAll()
    {
	VirtualThread t = (VirtualThread)Thread.currentThread();
	if (lock.getOwner() != t)
	    throw new IllegalMonitorStateException("thread is not lock owner");
	t.condBroadcast(this);
    }
}
