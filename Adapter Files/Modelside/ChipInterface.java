package com.uppaal.chip;

import com.uppaal.tron.Reporter;

public interface ChipInterface
{
    public void start();

    public void waitForStart() throws InterruptedException;

    public void join() throws InterruptedException;

    // public void setChipListener(ChipListener listener);
    
    public void setReporter(Reporter r);

    public void handleMyInput1() throws InterruptedException;
    public void handleMyInput2() throws InterruptedException;
    public void handleMyInput3() throws InterruptedException;
    public void handleMyInput4() throws InterruptedException;
    
    
    
}
