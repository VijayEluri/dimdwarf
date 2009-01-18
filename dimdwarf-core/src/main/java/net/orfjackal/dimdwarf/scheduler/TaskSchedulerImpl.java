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

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.TaskScheduler;
import net.orfjackal.dimdwarf.tasks.TaskContext;
import net.orfjackal.dimdwarf.tx.*;
import net.orfjackal.dimdwarf.util.Clock;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import javax.annotation.concurrent.*;
import java.util.concurrent.*;

/**
 * @author Esko Luontola
 * @since 24.11.2008
 */
@Singleton
@ThreadSafe
public class TaskSchedulerImpl implements TaskScheduler, TaskProducer {

    private static final String TASKS_PREFIX = TaskSchedulerImpl.class.getName() + ".tasks";

    private final BlockingQueue<ScheduledTaskHolder> scheduledTasks = new DelayQueue<ScheduledTaskHolder>();
    private final RecoverableSet<ScheduledTask> persistedTasks;

    private final Provider<Transaction> tx;
    private final Clock clock;
    private final Executor taskContext;

    @Inject
    public TaskSchedulerImpl(Provider<Transaction> tx,
                             Clock clock,
                             @TaskContext Executor taskContext,
                             RecoverableSetFactory rsf) {
        this.tx = tx;
        this.clock = clock;
        this.taskContext = taskContext;
        this.persistedTasks = rsf.create(TASKS_PREFIX);
    }

    public void start() {
        recoverTasksFromDatabase();
    }

    private void recoverTasksFromDatabase() {
        // TODO: move recovery of scheduled tasks to a new class
        taskContext.execute(new Runnable() {
            public void run() {
                for (ScheduledTask st : persistedTasks.getAll()) {
                    String binding = persistedTasks.put(st);
                    long scheduledTime = st.getScheduledTime();
                    scheduledTasks.add(new ScheduledTaskHolder(binding, scheduledTime));
                }
            }
        });
    }

    public Future<?> submit(Runnable task) {
        return schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        delay = unit.toMillis(delay);
        ScheduledTask st = new ScheduledTaskImpl(task, ScheduledOneTimeRun.create(delay, clock));
        addToExecutionQueue(st);
        return new SchedulingFuture(st);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        initialDelay = unit.toMillis(initialDelay);
        period = unit.toMillis(period);
        ScheduledTask st = new ScheduledTaskImpl(task, ScheduledAtFixedRate.create(initialDelay, period, clock));
        addToExecutionQueue(st);
        return new SchedulingFuture(st);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        initialDelay = unit.toMillis(initialDelay);
        delay = unit.toMillis(delay);
        ScheduledTask st = new ScheduledTaskImpl(task, ScheduledWithFixedDelay.create(initialDelay, delay, clock));
        addToExecutionQueue(st);
        return new SchedulingFuture(st);
    }

    private void addToExecutionQueue(ScheduledTask st) {
        ScheduledTaskHolder h = saveToDatabase(st);
        enqueueOnCommit(h);
    }

    private ScheduledTaskHolder saveToDatabase(ScheduledTask st) {
        String binding = persistedTasks.put(st);
        long scheduledTime = st.getScheduledTime();
        return new ScheduledTaskHolder(binding, scheduledTime);
    }

    private void enqueueOnCommit(final ScheduledTaskHolder holder) {
        tx.get().join(new TransactionParticipant() {
            public void prepare() throws Throwable {
            }

            public void commit() {
                scheduledTasks.add(holder);
            }

            public void rollback() {
            }
        });
    }

    public TaskBootstrap takeNextTask() throws InterruptedException {
        return scheduledTasks.take();
    }

    public TaskBootstrap pollNextTask() {
        return scheduledTasks.poll();
    }

    @Nullable
    private Runnable getTaskInsideTransaction0(ScheduledTaskHolder holder) {
        cancelTakeOnRollback(holder);
        ScheduledTask task = takeFromDatabase(holder);
        if (task.isDone()) {
            return null;
        }
        Runnable run = task.startScheduledRun();
        if (!task.isDone()) {
            addToExecutionQueue(task);
        }
        return run;
    }

    private ScheduledTask takeFromDatabase(ScheduledTaskHolder holder) {
        return persistedTasks.remove(holder.getBinding());
    }

    private void cancelTakeOnRollback(final ScheduledTaskHolder holder) {
        // FIXME: If the task fails and the retry limit is reached, the task should be removed from the database
        // or cancelled, so that it will not be rescheduled when the system is restarted. 
        tx.get().join(new TransactionParticipant() {
            public void prepare() throws Throwable {
            }

            public void commit() {
            }

            public void rollback() {
                scheduledTasks.add(holder);
            }
        });
    }

    @TestOnly
    int getQueuedTasks() {
        return scheduledTasks.size();
    }


    @Immutable
    private class ScheduledTaskHolder implements Delayed, TaskBootstrap {

        private final String binding;
        private final long scheduledTime;

        public ScheduledTaskHolder(String binding, long scheduledTime) {
            this.binding = binding;
            this.scheduledTime = scheduledTime;
        }

        @Nullable
        public Runnable getTaskInsideTransaction() {
            return getTaskInsideTransaction0(this);
        }

        public String getBinding() {
            return binding;
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(scheduledTime - clock.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        public int compareTo(Delayed o) {
            ScheduledTaskHolder other = (ScheduledTaskHolder) o;
            return (int) (this.scheduledTime - other.scheduledTime);
        }
    }
}
