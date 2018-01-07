package com.uppaal.tron;

import java.io.IOException;

/**
 * Provides an interface to implementation under test (IUT) for the tester.
 *
 * Reporter will call the method configure() when the new connection with
 * tester is established. The method perform() is called when an input from
 * the tester arrives.
 *
 * @see Reporter
 */

public interface Adapter
{
    /**
     * The method is called upon new connection with tester is established.
     *
     * During the call the implementing IUT should configure the input/output
     * testing interface via reporter.addInput/Output methods, set the
     * timeunit and timeout values. Testing begins when this method call
     * returns and no configuration error occures. At least the time unit and
     * time-out must be set on the reporter.
     *
     * @param reporter the reporter instance which has just established
     * a new connection with tester.
     * @see Reporter#addInput
     * @see Reporter#addOutput
     * @see Reporter#addVarToInput
     * @see Reporter#addVarToOutput
     * @see Reporter#setTimeUnit
     * @see Reporter#setTimeout
     */
    public void configure(Reporter reporter)
	throws TronException, IOException;

    /**
     * The method is called when the configured tester is offering an input.
     *
     * The method should not block (no conditional wait is allowed) and
     * return as soon as possible. The monitor locking is allowed provided
     * that the monitor is unlocked most of the time (locking used only to
     * ensure the action atomicity without time delay). The method should
     * avoid routines which produce an output for the tester, otherwise there
     * is a high risk of a deadlock in the adapter.
     *
     * It is recommended that the implementing class puts the event into the
     * (thread-protected) queue and use another thread to pull events and make
     * the actual calls without fear of producing output to the tester.
     *
     * @param chan the channel ID (from Reporter.addInput() configuration)
     * @param params the values bound to the action.
     * @see Reporter#addInput
     * @see Reporter#addOutput
     * @see Reporter#addVarToInput
     * @see Reporter#addVarToOutput
     */
    public void perform(int chan, int[] params);
}
