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

package net.orfjackal.dimdwarf.scheduler;

import net.orfjackal.dimdwarf.api.Entity;
import net.orfjackal.dimdwarf.api.internal.EntityObject;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author Esko Luontola
 * @since 25.11.2008
 */
@Entity
public class SchedulingControlImpl implements EntityObject, Serializable, SchedulingControl {
    private static final long serialVersionUID = 1L;

    // TODO: Change SchedulingControl to be the top-level object, so that the bindings in TaskSchedulerImpl point to it.
    // This should avoid the need to create new entities on every run.
    // Have implementations of AbstractScheduledTask be contained inside SchedulingControl.
    // Rename ScheduledTask to Run - one run instance of a series of scheduled runs. Make it a value object.
    // Rename SchedulingControl to ScheduledTask.

    private final Runnable task;
    private SchedulingStrategy nextRun;
    private boolean cancelled = false;

    public SchedulingControlImpl(Runnable task, SchedulingStrategy nextRun) {
        this.task = task;
        this.nextRun = nextRun;
    }

    public long getDelay(TimeUnit unit) {
        return nextRun.getDelay(unit);
    }

    public boolean isDone() {
        return nextRun == null || cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled() {
        cancelled = true;
    }

    public long getScheduledTime() {
        return nextRun.getScheduledTime();
    }

    public Runnable getTask() {
        return task;
    }

    public void beginNewRun() {
        nextRun = nextRun.nextRepeatedRun();
    }

    public boolean willRepeatAfterCurrentRun() {
        return nextRun != null;
    }
}
