package com.uppaal.Sample;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

import com.uppaal.tron.Reporter;
import com.uppaal.tron.VirtualThread;
import com.uppaal.tron.VirtualLock;
import com.uppaal.tron.VirtualCondition;

public class Chip extends VirtualThread implements ChipInterface {
	private enum Loc {
		Wait, Pass
	}

	private Loc location;

	Lock lock = null;
	Condition cond = null;
	boolean started = false;
	ChipListener listener = null;

	public Chip(int mutant) {
		super("Chip");
		location = Loc.Wait;
	}

	public void setChipListener(ChipListener listener) {
		this.listener = listener;
	}

	public synchronized void waitForStart() throws InterruptedException {
		while (!started)
			wait();
	}

	public void run() {
		try {
			execute();
		} catch (InterruptedException e) {
		}
		listener.disconnect();
		lock.unlock();
	}

	protected void execute() throws InterruptedException {
		lock = new VirtualLock("ChipLock");
		cond = lock.newCondition();
		lock.lock();
		// notify that Chip is ready:
		synchronized (this) {
			started = true;
			notifyAll();
		}

		while (started) {
			switch (location) {
			case Wait:
				cond.await();
				break;
			case Pass:
				if (!cond.await(1000, TimeUnit.MILLISECONDS)) {
					listener.reportMyOutput();
					location = Loc.Wait;
				} // else state is already updated
				break;
			}
		}
	}

	public void handleMyInput1(int uInput) throws InterruptedException {
		lock.lock();
		switch (location) {
		case Wait:
			location = Loc.Pass;
			ModSimInputGen.writeFile(uInput);
			cond.signalAll();
			break;
		}
		lock.unlock();
	}

	// public void handleMyInput2(int uInput) throws InterruptedException
	// {
	// lock.lock();
	// switch (location) {
	// case Pass:
	// location = Loc.Wait;
	// CsvWriterAppend.writeFile(uInput);
	// cond.signalAll();
	// break;
	// }
	// lock.unlock();
	// }
}
