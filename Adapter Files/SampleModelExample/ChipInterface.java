package com.uppaal.Sample;

import com.uppaal.tron.Reporter;

public interface ChipInterface {
	public void start();

	public void waitForStart() throws InterruptedException;

	public void join() throws InterruptedException;

	public void setChipListener(ChipListener listener);

	public void handleMyInput1(int msg) throws InterruptedException;
	// public void handleMyInput2(int msg) throws InterruptedException;
	// public void handleMyInput3(int msg) throws InterruptedException;
	// public void handleMyInput4(int msg) throws InterruptedException;
}
