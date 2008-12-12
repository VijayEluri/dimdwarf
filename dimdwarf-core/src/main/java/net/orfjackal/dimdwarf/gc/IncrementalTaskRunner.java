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

package net.orfjackal.dimdwarf.gc;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.TaskScheduler;

import java.io.Serializable;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 10.12.2008
 */
public class IncrementalTaskRunner implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;

    @Inject public transient TaskScheduler scheduler;
    private final Queue<IncrementalTask> tasks = new LinkedList<IncrementalTask>();
    private final Runnable onFinished;

    public IncrementalTaskRunner(IncrementalTask task, Runnable onFinished) {
        this.tasks.add(task);
        this.onFinished = onFinished;
    }

    public void run() {
        IncrementalTask task = tasks.poll();
        if (task == null) {
            onFinished.run();
        } else {
            tasks.addAll(task.step());
            scheduler.submit(this);
            // TODO: Run incremental tasks without scheduling new tasks and as such creating new entities.
            // Submitting a new task will create a new entity, which is problematic because
            // that causes more work for the garbage collector while the collector is running.
        }
    }
}
