package com.uppaal.chiporiginal;

import com.uppaal.tron.Reporter;

public interface ChipInterface
{
    public void start();

    public void waitForStart() throws InterruptedException;

    public void join() throws InterruptedException;

    // public void setChipListener(ChipListener listener);
    
    public void setReporter(Reporter r);

    public void handleMyInput1(int sourceNode, int destinationNode) throws InterruptedException;
    public void handleMyInput2(int sourceNode, int destinationNode) throws InterruptedException;
    public void handleMyInput3(int sourceNode, int destinationNode) throws InterruptedException;
    public void handleMyInput4(int sourceNode, int destinationNode) throws InterruptedException;
    
    
    
}
