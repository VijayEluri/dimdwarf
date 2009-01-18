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

package net.orfjackal.dimdwarf.tasks.util;

import java.io.Serializable;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 10.12.2008
 */
public class MultiStepIncrementalTask implements IncrementalTask, Serializable {
    private static final long serialVersionUID = 1L;

    // TODO: this class is not used in production code

    private final Queue<IncrementalTask> tasks = new LinkedList<IncrementalTask>();
    private final int stepsPerTask;

    public MultiStepIncrementalTask(IncrementalTask task, int stepsPerTask) {
        this.tasks.add(task);
        this.stepsPerTask = stepsPerTask;
    }

    public Collection<? extends IncrementalTask> step() {
        for (int i = 0; i < stepsPerTask && !tasks.isEmpty(); i++) {
            IncrementalTask task = tasks.poll();
            tasks.addAll(task.step());
        }
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(this);
    }
}
