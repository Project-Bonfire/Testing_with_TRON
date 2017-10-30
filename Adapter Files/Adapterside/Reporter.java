package com.uppaal.tron;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/**
 * Provides API to the tester via generic TCP/IP socket adapter.
 *<p>
 * Reporter maintains one connection to tester in the background, it
 * reports the output actions to tester, receives input actions and applies
 * them to Adapter. The connections are maintained automatically in the
 * background, one connection at most at a time.
 *<p>
 * Every connection goes through two phases: initialization and active testing.
 * During initialization, Adapter is given a chance to configure the
 * input/output interface through addInput(), addOutput(), addInpVar(),
 * addOutVar(), setTimeUnit() and setTimeout() methods. Depending on the
 * nature of tester the configuration can be optional, however the generic
 * TRON socket adapter requires at least time unit and time out to be set.
 *<p>
 * When configuration finished (Adapter.configuration() returns), active
 * testing phase starts. During active testing, no configuration is allowed,
 * only exchange of input and output actions is possible.
 *
 *@author Marius Mikucionis <marius@cs.aau.dk>
 *@see Adapter
 */
public class Reporter extends VirtualThread
{
    /**
     * Controls whether the debug information should be produced into err
     * stream. true enables and false disables debug output.
     * This variable can be set via environment variable DEBUG_REPORTER.
     */
    public static boolean DBG = (System.getenv("DEBUG_REPORTER")!=null);
    /**
     * Socket adapter command for requesting an integer encoding of
     * input channel.
     * The format of the command is the following:
     * send: [byte of command] [byte of following string length]
     * [byte-string of input channel name]
     * receive: [int32_t of result].
     * The positive result means channel identifier.
     * The negative result means error code.
     */
    public static final char SA_InpEnc=1;
    /**
     * Socket adapter command for requesting an integer encoding of
     * output channel.
     * The format of the command is the following:
     * send: [byte of command] [byte of following string length]
     * [byte-string of output channel name]
     * receive: [int32_t of result].
     * The positive result means channel identifier.
     * The negative result means error code.
     */
    public static final char SA_OutEnc=2;
    /**
     * Socket adapter command for adding variable value to input action.
     * The format of the command is the following:
     * send: [byte of command] [int32 of input channel id]
     * [byte of following string length] [byte-string of variable name]
     * receive: [int32_t of result].
     * The result equal zero means success.
     * The negative result means error code.
     */
    public static final char SA_VarToInp=3;
    /**
     * Socket adapter command for adding variable value to output action.
     * The format of the command is the following:
     * send: [byte of command] [int32 of output channel id]
     * [byte of following string length] [byte-string of variable name]
     * receive: [int32_t of result].
     * The result equal zero means success.
     * The negative result means error code.
     */
    public static final char SA_VarToOut=4;
    /**
     * Socket adapter command for setting the value of model time unit.
     * The format of the command is the following:
     * send: [byte of command] [int32 of seconds] [int32 of microseconds]
     * receive: [int32_t of result].
     * The result equal zero means success.
     * The negative result means error code.
     */
    public static final char SA_TimeUnit=5;
    /**
     * Socket adapter command for setting the amount of time for testing.
     * The format of the command is the following:
     * send: [byte of command] [int32 of seconds] [int32 of microseconds]
     * receive: [int32_t of result].
     * The result equal zero means success.
     * The negative result means error code.
     */
    public static final char SA_Timeout=6;
    /**
     * Socket adapter command for finishing configuration and start testing.
     * The format of the command is the following:
     * send: [byte of command]
     * receive: [byte of following string length].[byte-string of error
     * message]
     * Zero length result means success, otherwise.
     */
    public static final char SA_TestExec=64;
    /**
     * Socket adapter command for requesting explanation of tester's error
     * code.
     * The format of the command is the following:
     * send: [byte of command]
     * receive: [byte of following string length].[byte-string of error
     * message]
     */
    public static final char SA_GetError=127;
    /**
     * Socket adapter command bit for acknowlegding the I/O action transmition
     * (simtime only).
     * Action transmition acknowledgements must be sent for every action,
     * but only when simulated time framework is used, otherwise such commands
     * are ignored. To acknowledge n inputs received one has to send n input
     * acknowledgements, this can be done more efficiently by sending the value
     * (n | SA_Ack).
     */
    public static final int SA_Ack = (1 << 31);

    private DataInputStream is = null;
    private DataOutputStream os = null;

    private Adapter adapter = null;
    private boolean abort = false;
    private Object lock = new Object();
    private int acks = 0;

    private ServerSocket server = null;
    private String host = null;
    private int port = 0;

    /**
     * Constructor for listening on ServerSocket port and accepting incoming
     * tester connections. Reporter accepts a single connection at a time.
     * Another connection is accepted if the previous connection is broken.
     *
     * @param adapter a test adapter capable of receiving and delivering input
     * actions.
     * @param port the port number to listen for connections.
     * @see Adapter
     */
    public Reporter(Adapter adapter, int port)
    {
	super("TR.Receiver");
	this.adapter = adapter;
	try { server = new ServerSocket(port); }
	catch (IOException e) {
	    System.err.println(e);
	    System.exit(-1);
	}
	start();
    }
    /**
     * Constructor for connecting to tester process running on specified host
     * and listening to specified TCP/IP port.
     *
     * @param adapter a test adapter capable of receiving and delivering input
     * actions.
     * @param host a machine address where tester process is running.
     * @param port the port number where tester listens for connections.
     * @see Adapter
     */
    public Reporter(Adapter adapter, String host, int port)
    {
	super("TR.Receiver");
	this.adapter = adapter;
	this.host = new String(host);
	this.port = port;
	start();
    }

    private String readString() throws IOException
    {
	int len = is.readByte();
	if (len > 0) {
	    byte[] buffer = new byte[len];
	    is.read(buffer, 0, len);
	    return new String(buffer);
	} else return null;
    }

    private void writeString(String s) throws IOException
    {
	os.writeByte(s.length());
	os.writeBytes(s);
    }
    /**
     * Adds an input channel to the testing interface and returns a channel
     * identifier. Channel identifier is a positive integer, the value of the
     * identifier is bound by the total number of channels used in the
     * specification.
     *
     * @param channel the input channel name
     * @return the integer which encodes the given channel.
     * @see Reporter#report
     * @see Adapter#perform
     */
    public int addInput(String channel) throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_InpEnc);
	writeString(channel);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("addInput: " + getErrorMessage(res));
	return res;
    }

    /**
     * Adds an output channel to the testing interface and returns a channel
     * identifier. Channel identifier is a positive integer, the value of the
     * identifier is bound by the total number of channels used in the
     * specification.
     *
     * @param channel the input channel name
     * @return the channel identifier.
     * @see Reporter#report
     * @see Adapter#perform
     */
    public int addOutput(String channel) throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_OutEnc);
	writeString(channel);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("addOutput: " + getErrorMessage(res));
	return res;
    }

    /**
     * Binds a variable to the specified input channel for the variable value
     * to be attached as parameters to an input action on the channel.
     *
     * @param channel the input channel name
     * @param variable the (integer) variable name to be bound to action
     * @see Reporter#report
     * @see Adapter#perform
     */
    public void addVarToInput(int channel, String variable)
	throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_VarToInp);
	os.writeInt(channel);
	writeString(variable);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("addVarToInput: " + getErrorMessage(res));
    }
    /**
     * Binds a variable to the specified output channel for the variable value
     * to be attached as parameters to an output action on the channel.
     *
     * @param channel the input channel name
     * @param variable the (integer) variable name to be bound to action
     * @see Reporter#report
     * @see Adapter#perform
     */
    public void addVarToOutput(int channel, String variable)
	throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_VarToOut);
	os.writeInt(channel);
	writeString(variable);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("addVarToOutput: " + getErrorMessage(res));
    }

    /**
     * Sets the length of model time units in real world units.
     * @param microsecs the value of one model time unit in microsconds.
     */
    public void setTimeUnit(long microsecs)
	throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_TimeUnit);
	os.writeLong(microsecs);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("setTimeUnit: " + getErrorMessage(res));
    }

    /**
     * Sets the amount of time units to be allocated for testing.
     * @param timeout_in_units the number of time units before testing timeout.
     */
    public void setTimeout(int timeout_in_units)
	throws TronException, IOException
    {
	if (connected)
	    throw new TronException("Testing already in progress");
	os.writeByte(SA_Timeout);
	os.writeInt(timeout_in_units);
	os.flush();
	int res = is.readInt();
	if (res < 0)
	    throw new TronException("setTimeout: " + getErrorMessage(res));
    }

    private boolean connected = false;
    /**
     * Checks whether the tester is connected.
     * @return true if tester is connected, false if not.
     */
    public boolean isConnected() { return connected; }

    /**
     * Disconnects the connected tester.
     */
    public void disconnect()
    {
	try {
	    connected = false;
	    if (os != null) os.close();
	    if (os != null) is.close();
	} catch (IOException e){}
    }
    /**
     * Disconnects, interrupts the serving threads and releases connection
     * resources.
     */
    public void shutdown()
    {
	abort = true;
	disconnect();
    }

    private void accept() throws IOException
    {
	try {
	    Socket socket = server.accept();
	    socket.setTcpNoDelay(true);
	    is = new DataInputStream(socket.getInputStream());
	    os = new DataOutputStream(socket.getOutputStream());
	} catch (IOException e) {
	    System.err.println(e);
	}
    }

    private void connect()
	throws UnknownHostException, ConnectException, IOException
    {
	Socket socket = new Socket(host, port);
	socket.setTcpNoDelay(true);
	is = new DataInputStream(socket.getInputStream());
	os = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * The reporter thread is listening for inputs in this method, no external
     * or additional thread creation is required, reporter creates its own
     * thread.
     */
    public void run()
    {
	deactivate();
	while (!abort) try {
	    if (server != null) accept();
	    else connect();
	    adapter.configure(this);
	    os.writeByte(SA_TestExec);
	    os.flush();
	    String res = readString();
	    if (res != null)
		throw new TronException("TRON configure problem: "+res);
	    connected = true;
	    int chan;
	    while (!abort) {
		if (DBG) System.err.println("TR waiting for incomming inputs");
		chan = is.readInt();
		if ((chan & SA_Ack) != 0) {
		    chan &= ~SA_Ack;
		    if (DBG) System.err.println("TR got ack");
		    if (virtualtime())// acknowledge for virtual
			synchronized (lock) { // ensure write consistency
			    ++acks;
			    lock.notifyAll();
			}
		} else {
		    if (DBG) System.err.println("TR perform "+chan);
		    short n = is.readShort();
		    int[] data = new int[n];
		    for (int i=0; i<n; ++i)
			data[i] = is.readInt();
		    adapter.perform(chan, data);
		    if (virtualtime()) {
			synchronized (lock) { // ensure write consistency
			    os.writeInt(SA_Ack | 1);
			    os.flush();
			}
		    }
		}
	    }
	} catch (TronException e) {
	    System.err.println(e);
	    System.exit(-1);
	} catch (UnknownHostException e) {
	    System.err.println(e);
	    System.exit(-1);
	} catch (ConnectException e) {
	    System.err.println(e);
	    shutdown();
	    /* try { Thread.sleep(2000); } // let's retry
	    catch(InterruptedException ex){} */
	} catch (IOException e) {
	    System.err.println("TR.Receiver: "+e);
	    shutdown();
	}
	activate();
	quit();
	System.exit(0);
    }

    /**
     * Retrieves the tester error message for the specified error code.
     * @param error_code the error code from tester.
     * @return error message.
     */
    public String getErrorMessage(int error_code) throws IOException
    {
	if (DBG) System.err.println("Reporter.getErrorMessage");
	os.writeByte(SA_GetError);
	os.writeInt(error_code);
	os.flush();
	return readString();
    }

    /**
     * Reports the output action without parameters to the tester.
     * @param chan the channel identifier
     * @see Reporter#report(int, int[])
     */
    public void report(int chan)
    {
	if (connected) {
	    try {
		if (DBG) System.err.println("TR.report transmitting");
		synchronized (lock) { // ensure socket write consistency
		    os.writeInt(chan);
		    os.writeShort(0);
		    os.flush();
		    if (virtualtime()) try { // block the virtual time
			if (DBG) System.err.println("TR.report wait for ack");
			while (acks == 0) lock.wait();// wait for ack
			--acks;
		    } catch (InterruptedException e) {
			System.err.println(e);
		    }
		}
		if (DBG) System.err.println("TR.report success");
	    } catch (IOException e) {
		System.err.println(e);
		disconnect();
	    }
	}
    } /* report() */

    /**
     * Reports the output action to the tester.
     * @param chan the channel identifier
     * @param params the variable values attached to the output action.
     * @see Reporter#report(int)
     */
    public void report(int chan, int[] params)
    {
	if (connected) {
	    try {
		if (DBG) System.err.println("TR.report transmitting");
		synchronized (lock) { // ensure socket write consistency
		    os.writeInt(chan);
		    os.writeShort(params.length);
		    for (int p: params) os.writeInt(p);
		    os.flush();
		    if (virtualtime()) try { // block the virtual time
			if (DBG) System.err.println("TR.report wait for ack");
			while (acks == 0) lock.wait();// wait for ack
			--acks;
		    } catch (InterruptedException e) {
			System.err.println(e);
		    }
		}
		if (DBG) System.err.println("TR.report success");
	    } catch (IOException e) {
		System.err.println(e);
		disconnect();
	    }
	}
    } /* report() */
} /* class Reporter */
