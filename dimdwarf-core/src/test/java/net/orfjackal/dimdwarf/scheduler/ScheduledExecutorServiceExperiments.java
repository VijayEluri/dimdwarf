/*
 * This file is part of Dimdwarf Application Server <http://dimdwarf.sourceforge.net/>
 *
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
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

package net.orfjackal.dimdwarf.scheduler;

import java.util.concurrent.*;

/**
 * @author Esko Luontola
 * @since 25.11.2008
 */
public class ScheduledExecutorServiceExperiments {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
            public int i;

            public void run() {
                System.out.println(i);
                i++;
            }
        }, 0, 1, TimeUnit.SECONDS);

        Thread.sleep(3100);

        System.out.println("future.isDone() = " + future.isDone()); // false
        System.out.println("future.isCancelled() = " + future.isCancelled()); // false
        System.out.println("future.getDelay(TimeUnit.MILLISECONDS) = "
                + future.getDelay(TimeUnit.MILLISECONDS)); // milliseconds until the next execution

        //System.out.println("future.get() = " + future.get()); // blocks indefinitely
        future.cancel(false); // cancels all the repeats

        System.out.println("\nafter cancel:");
        System.out.println("future.isDone() = " + future.isDone()); // true
        System.out.println("future.isCancelled() = " + future.isCancelled()); // true
        System.out.println("future.getDelay(TimeUnit.MILLISECONDS) = "
                + future.getDelay(TimeUnit.MILLISECONDS)); // the same as above

        Thread.sleep(3000);

        System.out.println("\nafter wait:");
        System.out.println("future.isDone() = " + future.isDone()); // true
        System.out.println("future.isCancelled() = " + future.isCancelled()); // true
        System.out.println("future.getDelay(TimeUnit.MILLISECONDS) = "
                + future.getDelay(TimeUnit.MILLISECONDS)); // negative - the time when the next execution would have been 

        scheduler.shutdown();
    }
}
