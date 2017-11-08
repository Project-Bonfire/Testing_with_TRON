package com.uppaal.Sample;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

import com.uppaal.tron.Adapter;
import com.uppaal.tron.TronException;
import com.uppaal.tron.Reporter;
import com.uppaal.tron.VirtualThread;
import com.uppaal.tron.VirtualLock;
import com.uppaal.tron.VirtualCondition;

/**
 * TestIOHandler is an adapter stub translating abstract input events from 
 * tester into method LampInterface calls and LevelListener method calls into 
 * abstract output events to tester.
 */
public class TestIOHandler extends VirtualThread 
    implements Adapter, ChipListener
{
    /**
     * Controls whether the debug information should be produced into err
     * stream. true enables and false disables debug output.
     * This variable can be set via environment variable DEBUG_LC.
     */
    public static boolean DBG = (System.getenv("DEBUG_LC")!=null);

    Lock lock = null;
    Condition cond = null;
    LinkedList<Integer> inputBuffer = new LinkedList<Integer>();

    int myInput1 = 0;  // channel identifier for MyInput1
  //  int myInput2 = 0;// channel identifier for MyInput2
    int myOutput = 0; // channel identifier for MyOutput

    Reporter reporter; // tester proxy for adapter.

    ChipInterface chip = null;

    /**
     * @param lamp object to receive inputs as method calls.
     */
    public TestIOHandler(ChipInterface chip)
    {
	super("TestInput");
	this.chip = chip;
	start();
	if (DBG) System.err.println("IOHandler: wait for thread to start");
	synchronized (this) {
	    try {  while(lock==null) wait(); }
	    catch(InterruptedException e){}
	}
	if (DBG) System.err.println("IOHandler: thread is started");
    }
    
    /**
     * Adapter method: configures the test interface for incoming connection.
     * The method is normally a called by reporter when connection with tester 
     * is being established.
     * @param reporter provides a configuration and output reporting interface 
     * to the tester.
     */
    public void configure(Reporter reporter)
	throws TronException, IOException
    {
	myInput1 = reporter.addInput("start");
	//myInput2 = reporter.addInput("myInput2");
	myOutput = reporter.addOutput("stop");
	reporter.setTimeUnit(200);
	reporter.setTimeout(100000);
	this.reporter = reporter;
	if (DBG) System.err.println("IOHandler: waiting for others");
	// wait until lamp object is initialized:
	try { chip.waitForStart(); }
	catch (InterruptedException ex) {}
	if (DBG) System.err.println("IOHandler: starting test");
    }

    /**
     * Adapter method: handles abstract inputs encoded as channels with 
     * parameters. Called by Reporter when input is received.
     * @param chan is an input channel identifier.
     * @param params is an array of variable values bound to a channel.
     */
    public void perform(int chan, int[] params)
    {// No virtual wait is allowed in this method
	if (DBG) System.err.println("IOHandler: arrived");
	lock.lock();
	inputBuffer.add(new Integer(chan));
	cond.signalAll();
	lock.unlock();
	if (DBG) System.err.println("IOHandler: left");
    } /* perform() */
    
    /**
     * Adapter method processing the incoming queue of inputs.
     */
    public void run()
    {
	int msg;
	synchronized (this) {
	    lock = new VirtualLock("InputQueue");
	    cond = lock.newCondition();
	    notifyAll();
	}
	try {
	    if (DBG) System.err.println("IOHandler: waiting for inputs");
	    while (true) {
		lock.lock(); // lock operations on input buffer
		while (inputBuffer.isEmpty()) 
		    cond.await();
		msg = inputBuffer.poll().intValue();
		lock.unlock();// allow buffer to be filled again
		if (msg == myInput1) {
		    System.out.println("Start@");
		    chip.handleMyInput1(msg);
		} 
		//else if (msg == myInput2) {
		//    System.out.println("MyInput2");
		//    chip.handleMyInput2(msg);
		//}
		else {
		    System.err.println("IOHandler: UNKNOWN INPUT");
		}
	    }
	} catch(InterruptedException e) { 
	    System.err.println(e); 
	} finally { lock.unlock(); }
	System.err.println("IOHandler: stopped listening for inputs");
    }

    /**
     * Adapter method: reports changes encoded into output channel.
     */
    public void reportMyOutput()
    {
	if (reporter != null) {
	    System.out.println("Stop!!");
	    reporter.report(myOutput);
	}
    }
    public void disconnect()
    {
	if (reporter != null) {
	    System.out.println("(disconnect)");
	    reporter.disconnect();
	}
    }
}
 // Run the below from command prompt
// tron.exe -P 10,400 -F 300 -I SocketAdapter -v 9 modelSIM.xml -- localhost 9999
//tron.exe -P 10,400 -F 300 -I SocketAdapter -v 9 Chip5.xml -- localhost 9999

//tron.exe -Q 6521 -P 10,200 -F 300 -I SocketAdapter -v 9 Chip5.xml -- localhost 9999