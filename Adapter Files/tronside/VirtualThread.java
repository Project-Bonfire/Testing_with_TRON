package com.uppaal.tron;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.concurrent.TimeUnit;

/**
 * Virtual time framework thread. Use VirtualThread instead of Thread if you
 * want your application to be able to run both: realtime and virtual time.
 *<p>
 * Virtual time framework assumptions:
 *<p>
 * All threads are created via VirtualThread (the initial thread from main()
 * should not be used).
 *<p>
 * The time spent doing computations is negligible and virtually is zero.
 *<p>
 * Time ellapses only during explicit delays. Use VirtualCondition.await()
 * methods.
 *<p>
 * Monitor paradigm is supported through VirtualLock and VirtualCondition, 
 * i.e. lock VirtualLock object, compute, potentially call 
 * VirtualCondition.await or VirtualCondition.signal()/signalAll().
 *<p>
 *
 *@author Marius Mikucionis <marius@cs.aau.dk>
 *@see java.lang.Thread
 *@see VirtualCondition
 *@see VirtualLock
 */
public class VirtualThread extends Thread
{
    /**
     * Controls whether the debug information should be produced into err
     * stream. true enables and false disables debug output.
     * This variable can be set via environment variable DEBUG_THREADS.
     * The debug information show messages about thread activities, such as
     * locking and unlocking locks, waiting for conditional variable etc..
     */
    public static boolean DBG = (System.getenv("DEBUG_THREADS")!=null);
    static String host = null;
    static int port;
    private static Object lock = new Object();
    /**
     * Returns true if host clock is used instead of virtual time framework.
     */
    public static boolean realtime() { return (host == null); }
    /**
     * Returns true if virtual time framework is used instead of host clock.
     */
    public static boolean virtualtime() { return (host != null); }

    /**
     * Turn on the virtual time framework, where the clock service is provided
     * at specified network location.
     * @param host the name of the machine where the clock service is running.
     * @param port the port number where clock service is listening.
     */
    public static void setRemoteClock(String host, int port) {
	if (DBG) System.err.println("Setting virtual clock to "+host+":"+port);
	synchronized (lock) {
	    VirtualThread.host = host;
	    VirtualThread.port = port;
	}
    }

    /**
     * Creates a virtual thread to be used within virtual time framework.
     */
    public VirtualThread(){ super(); }
    /**
     * Creates a virtual thread with a name to be used within virtual time
     * framework.
     */
    public VirtualThread(String name){ super(name); }
    /**
     * Creates a virtual thread to be used to run another runnable object
     * within virtual time framework.
     */
    public VirtualThread(Runnable r){ super(r); }
    /**
     * Creates a virtual thread with a name to be used to run another runnable
     * object within virtual time framework.
     */
    public VirtualThread(Runnable r, String name){ super(r, name); }

    static final char C_OK = 0;
    static final char C_Deactivate = 1;
    static final char C_Activate = 2;
    static final char C_MutexInit = 3;
    static final char C_MutexDest = 4;
    static final char C_MutexLock = 5;
    static final char C_MutexUnlock = 6;
    static final char C_CondInit = 7;
    static final char C_CondDest = 8;
    static final char C_Wait = 9;
    static final char C_TimedWait = 10;
    static final char C_Delay = 11;// timedwait from now
    static final char C_Signal = 12;
    static final char C_Broadcast = 13;
    static final char C_GetTime = 14;

    static final char C_Error = 64;
    static final char C_TimedOut = 65;
    static final char C_Interrupt = 66;
    static final char C_Busy = 67;

    static final char C_Quit = 127;

    private boolean abort = false;
    private DataInputStream is = null;
    private DataOutputStream os = null;

    /**
     * Starts this thread.
     */
    public void start()
    {
	String myhost = null;
	int myport = 0;
	synchronized (lock) {
	    if (host != null) {
		myhost = host;
		myport = port;
	    }
	}
	if (myhost != null) {
	    int tries = 10;
	    while (tries > 0) try {
		tries--;
		if (DBG) System.err.println("Connecting to clock #"+tries);
		Socket socket = new Socket(myhost, myport);
		socket.setTcpNoDelay(true);
		is = new DataInputStream(socket.getInputStream());
		os = new DataOutputStream(socket.getOutputStream());
		String id = getName();
		os.writeByte(id.length());
		os.writeBytes(id);
	    } catch (UnknownHostException e) {
		System.err.println(e);
		System.exit(1);
	    } catch (ConnectException e) {
		System.err.println(e);
		try { Thread.sleep(2000); } // let's retry
		catch(InterruptedException ex){}
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    } finally {
		if (is != null) {
		    if (DBG)
			System.out.println("Connected to clock: "+getName());
		    break;
		}
		if (tries <= 0) System.exit(1);
	    }
	}
	super.start();
    }
    /**
     * The thread is requested to be temporarily removed from virtual time
     * framework accounting. The deactivated thread does not block the virtual
     * time passage but still can signal on VirtualCondition instances.
     * This is usefull in threads which are interacting with outside world
     * (i.e. providing side channel communication to this virtual time
     * framework). Use with caution, currently only Reporter has
     * reasonable use, which is communicating with tester.
     */
    protected void deactivate()
    {
	if (os != null)  try {
	    os.writeByte(C_Deactivate);
	    os.flush();
	    is.readInt();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }
    /**
     * The thread is requested to be returned into virtual time framework
     * accounting. The time is blocked by active threads until they call time
     * delaying functions.
     * @see VirtualThread#deactivate
     */
    protected void activate()
    {
	if (os != null)  try {
	    os.writeByte(C_Activate);
	    os.flush();
	    is.readInt();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected void quit()
    {
	if (os != null)  try {
	    os.writeByte(C_Quit);
	    os.flush();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected int mutexInit()
    {
	if (os != null)  try {
	    os.writeByte(C_MutexInit);
	    os.flush();
	    return is.readInt();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
	return -1;
    }

    protected void mutexDestroy(VirtualLock m)
    {
	if (os != null)  try {
	    os.writeByte(C_MutexDest);
	    os.writeInt(m.id);
	    os.flush();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected void mutexLock(VirtualLock m)
    {
	if (os != null)  try {
	    os.writeByte(C_MutexLock);
	    os.writeInt(m.id);
	    os.flush();
	    if (DBG)
		System.err.println(getName()+" tries to lock "+m.getName());
	    int res = is.readInt(); // block until result comes out
	    if (DBG) System.err.println(getName()+" locked "+m.getName());
	    if (res==0) return;
	    else {
		System.err.println(getName()+" failed to lock mutex: "+res);
		System.exit(1);
	    }
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected void mutexUnlock(VirtualLock m)
    {
	if (os != null)  try {
	    os.writeByte(C_MutexUnlock);
	    os.writeInt(m.id);
	    os.flush();
	    int res = is.readInt();
	    if (res!=0) {
		System.err.println(getName()+" failed to unlock "+m.getName()
				   +": "+res);
		System.exit(1);
	    }
	    if (DBG) System.err.println(getName()+" unlocked "+m.getName());
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected int condInit()
    {
	if (os != null)  try {
	    os.writeByte(C_CondInit);
	    os.flush();
	    return is.readInt();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
	return -1;
    }

    protected void condDestroy(VirtualCondition c)
    {
	if (os != null)  try {
	    os.writeByte(C_CondDest);
	    os.writeInt(c.id);
	    os.flush();
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

    protected void condWait(VirtualCondition c) throws InterruptedException
    {
	if (os != null) {
	    try {
		os.writeByte(C_Wait);
		os.writeInt(c.id);
		os.writeInt(c.lock.id);
		os.flush();
		if (DBG)
		    System.err.println(getName()+" waiting for "+c.getName());
		is.readInt();// block on reading the result
		if (DBG)
		    System.err.println(getName()+" received "+c.getName());
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    }
	} else synchronized(c) { c.wait(); }
    }

    protected boolean condDelay(VirtualCondition c, long time, TimeUnit unit)
	throws InterruptedException
    {
	if (os != null) {
	    int res, sec, micros;
	    switch (unit) {
	    case MICROSECONDS:
		sec = (int)(time / 1000000);
		micros = (int)(time % 1000000);
		break;
	    case MILLISECONDS:
		sec = (int)(time / 1000);
		micros = (int)(time % 1000 * 1000);
		break;
	    case NANOSECONDS:
		sec = (int)(time / 1000000000);
		micros = (int)(time / 1000 % 1000000);
		break;
	    case SECONDS:
		sec = (int) time;
		micros = 0;
		break;
	    default:
		throw new UnsupportedOperationException("unknown timeunit");
	    }
	    try {
		os.writeByte(C_Delay);
		os.writeInt(c.id);
		os.writeInt(c.lock.id);
		os.writeInt(sec);
		os.writeInt(micros);
		os.flush();
		if (DBG) System.err.println(getName()+" waiting for "+c.getName()+" for "+time);
		res = is.readInt(); // block on reading the result
		if (DBG) System.err.println(getName()+" received "+c.getName());
		switch (res) {
		case C_OK: return true;
		case C_TimedOut: return false;
		default: throw new InterruptedException("Clock error: "+res);
		}
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    }
	}
	throw new UnsupportedOperationException("not connected");
    }

    protected boolean condWait(VirtualCondition c, long millis)
	throws InterruptedException
    {
	if (os != null) {
	    int res;
	    try {
		os.writeByte(C_TimedWait);
		os.writeInt(c.id);
		os.writeInt(c.lock.id);
		os.writeInt((int)(millis / 1000));
		os.writeInt((int)(millis % 1000 * 1000));
		os.flush();
		res = is.readInt(); // block on reading the result
		switch (res) {
		case C_OK: return true;
		case C_TimedOut: return false;
		}
		System.err.println("VirtualClock error: "+res);
		System.exit(-1);
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(-1);
	    }
	}
	throw new UnsupportedOperationException("not connected");
    }

    protected boolean condDelay(VirtualCondition c, long millis)
	throws InterruptedException
    {
	if (os != null) {
	    int res;
	    try {
		os.writeByte(C_Delay);
		os.writeInt(c.id);
		os.writeInt(c.lock.id);
		os.writeInt((int)(millis / 1000));
		os.writeInt((int)(millis % 1000 * 1000));
		os.flush();
		res = is.readInt(); // block on reading the result
		switch (res) {
		case C_OK: return true;
		case C_TimedOut: return false;
		}
		System.err.println("VirtualClock error: "+res);
		System.exit(-1);
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    }
	}
	throw new UnsupportedOperationException("not connected");
    }


    protected void condSignal(VirtualCondition c) {
	if (os != null) {
	    throw new UnsupportedOperationException("not implemented");
	}
	throw new UnsupportedOperationException("not connected");
    }

    protected void condBroadcast(VirtualCondition c)
    {
	if (os != null) {
	    try {
		os.writeByte(C_Broadcast);
		os.writeInt(c.id);
		os.flush();
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    }
	} else
	    throw new UnsupportedOperationException("not connected");
    }

    protected long getTime()
    {
	long millis = 0;
	if (os != null) {
	    try {
		os.writeByte(C_GetTime); os.flush();
		millis = is.readInt() * 1000; // seconds
		millis += is.readInt() / 1000; // micro seconds
		return millis;
	    } catch (IOException e) {
		System.err.println(e);
		System.exit(1);
	    }
	}
	throw new UnsupportedOperationException("not connected");
    }

    /**
     * Returns a number of milliseconds since beginning of epoch or 
     * start of virtual clock.
     * @return current time in milliseconds.
     */
    public static long getTimeMillis()
    {
	if (host == null) return System.currentTimeMillis();
	else {
	    VirtualThread orig = (VirtualThread)Thread.currentThread();
	    return orig.getTime();
	}
    }
}
