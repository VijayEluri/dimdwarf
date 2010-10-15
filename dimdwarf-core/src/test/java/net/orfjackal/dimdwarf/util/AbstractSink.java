// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.util;

import org.hamcrest.Matcher;

public abstract class AbstractSink<T> {

    private final Object lock = new Object();
    private final long timeout;

    public AbstractSink(long timeout) {
        this.timeout = timeout;
    }

    public final void append(T event) {
        synchronized (lock) {
            doAppend(event);
            lock.notifyAll();
        }
    }

    protected abstract void doAppend(T event);

    public final boolean matches(Matcher<?> matcher) {
        Timeout timeout = new Timeout(this.timeout);

        synchronized (lock) {
            while (!doMatch(matcher)) {
                if (timeout.hasTimedOut()) {
                    return false;
                }
                timeout.waitUntilTimeout(lock);
            }
            return true;
        }
    }

    // TODO: change type to Matcher<T>
    protected abstract boolean doMatch(Matcher<?> matcher);
}
