/*
 * This file is part of Dimdwarf Application Server <http://dimdwarf.sourceforge.net/>
 *
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *     * Neither the name of the copyright holder nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.dimdwarf.db.inmemory;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * @author Esko Luontola
 * @since 19.11.2008
 */
@ThreadSafe
public class GroupLock<T extends Comparable<T>> {

    private final SortedSet<T> lockedKeys = new TreeSet<T>();

    private final ReentrantLock myLock = new ReentrantLock();
    private final Condition someKeyWasUnlocked = myLock.newCondition();

    @CheckReturnValue
    public LockHandle lockAll(T... keys) {
        return lockAll(Arrays.asList(keys));
    }

    @CheckReturnValue
    public LockHandle lockAll(Collection<T> keys) {
        myLock.lock();
        try {
            SortedSet<T> sortedKeys = new TreeSet<T>(keys);
            for (T key : sortedKeys) {
                awaitAndLock(key);
            }
            return new MyLockHandle(sortedKeys);
        } finally {
            myLock.unlock();
        }
    }

    private void awaitAndLock(T key) {
        while (isLocked(key)) {
            someKeyWasUnlocked.awaitUninterruptibly();
        }
        lockedKeys.add(key);
    }

    public boolean isLocked(T key) {
        myLock.lock();
        try {
            return lockedKeys.contains(key);
        } finally {
            myLock.unlock();
        }
    }

    public int getLockCount() {
        myLock.lock();
        try {
            return lockedKeys.size();
        } finally {
            myLock.unlock();
        }
    }


    @ThreadSafe
    private class MyLockHandle implements LockHandle {

        private Collection<T> keys;

        public MyLockHandle(Collection<T> keys) {
            this.keys = keys;
        }

        public void unlock() {
            myLock.lock();
            try {
                if (keys == null) {
                    throw new IllegalStateException("Keys have already been unlocked: " + keys);
                }
                lockedKeys.removeAll(keys);
                keys = null;
                someKeyWasUnlocked.signalAll();
            } finally {
                myLock.unlock();
            }
        }
    }
}
