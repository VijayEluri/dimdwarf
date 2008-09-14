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

package net.orfjackal.dimdwarf.context;

/**
 * This class is thread-safe. The contexts themselves do not need to be thread-safe,
 * because this class will make sure that they are used in only one thread.
 *
 * @author Esko Luontola
 * @since 5.9.2008
 */
public class ThreadContext {

    private static final ThreadLocal<Context> THREAD_LOCAL = new ThreadLocal<Context>();

    private ThreadContext() {
    }

    public static Context currentContext() {
        return THREAD_LOCAL.get();
    }

    public static void setUp(Context context) {
        if (THREAD_LOCAL.get() != null) {
            throw new IllegalStateException("Already set up");
        }
        THREAD_LOCAL.set(context);
    }

    public static <T> T get(Class<T> service) {
        Context context = THREAD_LOCAL.get();
        if (context == null) {
            throw new IllegalStateException("Not set up");
        }
        return context.get(service);
    }

    public static void tearDown() {
        if (THREAD_LOCAL.get() == null) {
            throw new IllegalStateException("Already torn down");
        }
        THREAD_LOCAL.set(null);
    }

    public static void runInContext(Context context, Runnable runnable) {
        setUp(context);
        try {
            runnable.run();
        } finally {
            tearDown();
        }
    }
}
