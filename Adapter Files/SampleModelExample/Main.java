package com.uppaal.Sample;

import com.uppaal.tron.Reporter;
import com.uppaal.tron.VirtualThread;

public class Main
{
    protected ChipInterface chip = null;

    Reporter reporter = null; // sends output
    TestIOHandler testIOHandler = null;// receives and delivers inputs

    protected int mutant = 0;

    public Main(String args[])
    {
	handleArguments(args);
	initialize();
	initializeIO();
    }

    protected void handleArguments(String args[]) 
    {
	int i = 0;
	while (i<args.length) {
	    if ("-M".equals(args[i])){
		if (i+1<args.length) mutant = Integer.parseInt(args[i+1]);
		else {
		    System.err.println("Specify mutant id.");
		    return ;
		}
		i += 2 ;
	    } else if ("-C".equals(args[i])) {
		if (i+2<args.length) {
		    int port = Integer.parseInt(args[i+2]);
		    if (port <= 0) {
			System.err.println("The specified port ("+args[i+2]+
					   ") is not in valid range.");
			return;
		    }
		    VirtualThread.setRemoteClock(args[i+1], port);
		    i += 3;
		} else {
		    System.err.println("Specify virtual clock, like: "+
				       "-C localhost 6521");
		    return ;
		}
	    } else {
		System.err.println("Uninterpreted option: "+args[i]);
		i++;
	    }
	}
    }

    protected void initialize()
    {
	chip = new Chip(mutant);
    }

    protected void initializeIO()
    {
	testIOHandler = new TestIOHandler(chip);
	reporter = new Reporter(testIOHandler, 9999);
	chip.setChipListener(testIOHandler);
    }

    public void play(){
	chip.start();
	System.out.println("Chip started");
	try { chip.join(); }
	catch (InterruptedException e) {}
    }

    public static void main(String args[])
    {
	Main main = new Main(args);
	main.play();
	System.out.println("Chip terminated");
    }
}
