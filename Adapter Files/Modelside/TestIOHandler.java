package com.uppaal.chiporiginal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

import com.uppaal.chiporiginal.ReadModelSimOutput;
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
    LinkedList<Integer> outputBuffer = new LinkedList<Integer>();

    ArrayList<Integer> channelVariable = new ArrayList<Integer>();
    ArrayList<Integer> outputChannelVariable = new ArrayList<Integer>();

    int myInput1 = 0;  // channel identifier for MyInput1
    int myInput2 = 0;// channel identifier for MyInput2
    int myInput3 = 0;  // channel identifier for MyInput1
    int myInput4 = 0;// channel identifier for MyInput2
    
    
    int myOutput1 = 0; // channel identifier for MyOutput
    int myOutput2 = 0; // channel identifier for MyOutput
    int myOutput3 = 0; // channel identifier for MyOutput
    int myOutput4 = 0; // channel identifier for MyOutput
    int destinationNode=0;
    int outputSource=0;
    int UNKNOWN=0000;
    int[] outModelvalue = new int[1]; // array of one for level value passing.

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
	myInput1 = reporter.addInput("i_ch_i1");
	myInput2 = reporter.addInput("i_ch_i2");
	myInput3 = reporter.addInput("i_ch_i3");
	myInput4 = reporter.addInput("i_ch_i4");	
	
	
	myOutput1 = reporter.addOutput("o_ch_o1");
	myOutput2 = reporter.addOutput("o_ch_o2");
	myOutput3 = reporter.addOutput("o_ch_o3");
	myOutput4 = reporter.addOutput("o_ch_o4");
	
	// Router 1 can forward packet to router 2,3,4. 
	reporter.addVarToInput(myInput1, "i1_Val");	
	reporter.addVarToInput(myInput2, "i2_Val");
	reporter.addVarToInput(myInput3, "i3_Val");
	reporter.addVarToInput(myInput4, "i4_Val");
	
	reporter.addVarToOutput(myOutput1, "rcv_Val1");
	reporter.addVarToOutput(myOutput2, "rcv_Val2");
	reporter.addVarToOutput(myOutput3, "rcv_Val3");
	reporter.addVarToOutput(myOutput4, "rcv_Val4");
	

	reporter.setTimeUnit(1000);
	reporter.setTimeout(1000000);
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
	for(int p:params){
		// channelVariable.clear();
		channelVariable.add(p);
		destinationNode = p;
		  // System.out.println("First: " + p);
		}
	// System.out.println("channel value:" + chan);
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
	int sourceNode;
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
	  sourceNode = inputBuffer.poll().intValue();
	  lock.unlock();// allow buffer to be filled again
	  if (sourceNode == myInput1) {
	      System.out.println("i_ch_i1"); 
	   System.out.println("channel value:" + "1");
	   sourceNode = 1;
	      chip.handleMyInput1(sourceNode, destinationNode);
	  } else if (sourceNode == myInput2) {
	      System.out.println("i_ch_i2");
	   System.out.println("channel value:" + "2");
	   sourceNode = 2;
	      chip.handleMyInput2(sourceNode, destinationNode);
	  } else if (sourceNode == myInput3) {
	      System.out.println("i_ch_i3");
	   System.out.println("channel value:" + "3");
	   sourceNode = 3;
	      chip.handleMyInput3(sourceNode, destinationNode);
	  } else if (sourceNode == myInput4) {
	      System.out.println("i_ch_i4");
	   System.out.println("channel value:" + "4");
	   sourceNode = 4;
	      chip.handleMyInput4(sourceNode, destinationNode);
	  } else {
	      System.err.println("IOHandler: UNKNOWN INPUT");
	  }
	     }
	 
//	try {
//	    if (DBG) System.err.println("IOHandler: waiting for inputs");
//	    while (true) {
//		lock.lock(); // lock operations on input buffer
//		while (inputBuffer.isEmpty()) 
//		    cond.await();
//		sourceNode = inputBuffer.poll().intValue();
//		lock.unlock();// allow buffer to be filled again
//		if (sourceNode == myInput1) {
//		    System.out.println("i_ch_i1");		    
//		    chip.handleMyInput1(sourceNode, destinationNode);
//		} else if (sourceNode == myInput2) {
//		    System.out.println("i_ch_i2");
//		    chip.handleMyInput2(sourceNode, destinationNode);
//		} else if (sourceNode == myInput3) {
//		    System.out.println("i_ch_i3");
//		    chip.handleMyInput3(sourceNode, destinationNode);
//		} else if (sourceNode == myInput4) {
//		    System.out.println("i_ch_i4");
//		    chip.handleMyInput4(sourceNode, destinationNode);
//		} else {
//		    System.err.println("IOHandler: UNKNOWN INPUT");
//		}
//	    }
	} catch(InterruptedException e) { 
	    System.err.println(e); 
	} finally { lock.unlock(); }
	System.err.println("IOHandler: stopped listening for inputs");
    }

//    /**
//     * Adapter method: reports changes encoded into output channel.
//     */
//    public void reportMyOutput()
//    {
//	if (reporter != null) {
//		
////	    System.out.println(myOutput1);
////	    reporter.report(myOutput1);
//		
//		if (reporter.equals(myOutput1)) {
//		    System.out.println(myOutput1);
//	    	ReadModelSimOutput readOutput = new ReadModelSimOutput();
//		    ArrayList<Integer> source = readOutput.readModelSimOutput();
//		    outModelvalue[0] = source.get(0);
//		    System.out.println("Expected Source : " + source.get(0));			
//		    reporter.report(myOutput1, outModelvalue);
//		} else if (reporter.equals(myOutput2)) {
//		    System.out.println(myOutput2);
//	    	ReadModelSimOutput readOutput = new ReadModelSimOutput();
//		    ArrayList<Integer> source = readOutput.readModelSimOutput();
//		    outModelvalue[0] = source.get(0);
//		    System.out.println("Expected Source : " + source.get(0));			
//		    reporter.report(myOutput2, outModelvalue);
//		} else if (reporter.equals(myOutput3)) {
//		    System.out.println(myOutput3);
//	    	ReadModelSimOutput readOutput = new ReadModelSimOutput();
//		    ArrayList<Integer> source = readOutput.readModelSimOutput();
//		    outModelvalue[0] = source.get(0);
//		    System.out.println("Expected Source : " + source.get(0));			
//		    reporter.report(myOutput3, outModelvalue);
//		} else if (reporter.equals(myOutput4)) {
//		    System.out.println(myOutput4);
//	    	ReadModelSimOutput readOutput = new ReadModelSimOutput();
//		    ArrayList<Integer> source = readOutput.readModelSimOutput();
//		    outModelvalue[0] = source.get(0);
//		    System.out.println("Expected Source : " + source.get(0));			
//		    reporter.report(myOutput4, outModelvalue);
//		} else {
//		    System.err.println("IOHandler: UNKNOWN OUTPUT");
//		}
//
//	    
//	}
//    }
    
	/**
	 * Adapter method: reports changes encoded into output channel.
	 */
	public void reportMyOutput() {
		if (reporter != null) {

			if (reporter.toString().equals(myOutput1)) {
				System.out.println(myOutput1);
				reporter.report(myOutput1);
			} else if (reporter.toString().equals(myOutput2)) {
				System.out.println(myOutput2);
				reporter.report(myOutput2);
			} else if (reporter.toString().equals(myOutput3)) {
				System.out.println(myOutput3);
				reporter.report(myOutput3);
			} else if (reporter.toString().equals(myOutput4)) {
				System.out.println(myOutput4);
				reporter.report(myOutput4);
			} else {
				System.err.println("IOHandler: UNKNOWN OUTPUT");
			}

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

// tron.exe -P 10,400 -F 300 -I SocketAdapter -v 9 Chip5Junparameterized.xml -- localhost 9999