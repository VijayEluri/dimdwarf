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

import com.google.inject.Provider;
import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.context.*;
import net.orfjackal.dimdwarf.tasks.*;
import net.orfjackal.dimdwarf.util.StubProvider;
import org.jmock.Expectations;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.util.concurrent.*;

/**
 * @author Esko Luontola
 * @since 26.11.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class TaskThreadPoolSpec extends Specification<Object> {

    private Context taskContext;
    private BlockingQueue<TaskBootstrap> taskQueue;
    private Logger logger;
    private TaskThreadPool pool;

    public void create() throws Exception {
        taskContext = mock(Context.class);
        TaskExecutor executor = new TaskExecutor(
                new StubProvider<Context>(taskContext),
                new Provider<FilterChain>() {
                    public FilterChain get() {
                        return new FilterChain(new Filter[0]);
                    }
                });

        taskQueue = new LinkedBlockingDeque<TaskBootstrap>();
        TaskProducer producer = new TaskProducer() {
            public TaskBootstrap takeNextTask() throws InterruptedException {
                return taskQueue.take();
            }
        };
        logger = mock(Logger.class);

        pool = new TaskThreadPool(executor, producer, Executors.newCachedThreadPool(), logger);
        pool.start();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void destroy() throws Exception {
        checking(new Expectations() {{
            allowing(logger).info(with(any(String.class)));
            allowing(logger).info(with(any(String.class)), with(any(Throwable.class)));
        }});
        pool.shutdown();
    }

    private static void executeAfterCurrentThreadIsNotRunning(final Runnable command) {
        final Thread currentThread = Thread.currentThread();
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.State state = currentThread.getState();
                if (state.equals(Thread.State.RUNNABLE)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                command.run();
            }
        });
        t.start();
    }


    public class WhenTasksAreAddedToTheQueue {

        private CountDownLatch end = new CountDownLatch(1);

        private volatile boolean taskWasExecuted = false;
        private volatile boolean bootstrapWasInsideTaskContext = false;
        private volatile boolean executionWasInsideTaskContext = false;

        public void create() throws InterruptedException {
            final Runnable task = new Runnable() {
                public void run() {
                    executionWasInsideTaskContext = (ThreadContext.getCurrentContext() == taskContext);
                    taskWasExecuted = true;
                    end.countDown();
                }
            };
            taskQueue.add(new TaskBootstrap() {
                public Runnable getTaskInsideTransaction() {
                    bootstrapWasInsideTaskContext = (ThreadContext.getCurrentContext() == taskContext);
                    return task;
                }
            });
            end.await();
        }

        public void theyAreExecuted() throws InterruptedException {
            specify(taskWasExecuted);
        }

        public void theyAreBootstrappedInsideTaskContext() {
            specify(bootstrapWasInsideTaskContext);
        }

        public void theyAreExecutedInsideTaskContext() throws InterruptedException {
            specify(executionWasInsideTaskContext);
        }
    }

    public class WhenManyTasksAreAddedToTheQueueConcurrently {

        private CountDownLatch step1 = new CountDownLatch(1);
        private CountDownLatch step2 = new CountDownLatch(1);
        private CountDownLatch step3 = new CountDownLatch(1);

        private volatile Integer runningTasks0 = null;
        private volatile Integer runningTasks1 = null;
        private volatile Integer runningTasks2 = null;
        private volatile Integer runningTasksEnd = null;

        public void create() throws InterruptedException {
            Runnable task1 = new Runnable() {
                public void run() {
                    try {
                        runningTasks1 = pool.getRunningTasks();
                        step1.countDown();
                        step2.await();
                        step3.countDown();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Runnable task2 = new Runnable() {
                public void run() {
                    try {
                        step1.await();
                        runningTasks2 = pool.getRunningTasks();
                        step2.countDown();
                        step3.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            runningTasks0 = pool.getRunningTasks();
            taskQueue.add(new SimpleTaskBootstrap(task1));

            step1.await();
            taskQueue.add(new SimpleTaskBootstrap(task2));

            step3.await();
            pool.awaitForCurrentTasksToFinish();
            runningTasksEnd = pool.getRunningTasks();
        }

        public void theyAreExecutedInParallel() throws InterruptedException {
            step1.await();
            step2.await();
            step3.await();
            specify(step1.getCount(), should.equal(0));
            specify(step2.getCount(), should.equal(0));
            specify(step3.getCount(), should.equal(0));
        }

        public void thePoolKnowsTheNumberOfRunningTasks() {
            specify(runningTasks0, should.equal(0));
            specify(runningTasks1, should.equal(1));
            specify(runningTasks2, should.equal(2));
            specify(runningTasksEnd, should.equal(0));
        }
    }

    public class WhenAClientWaitsForTheCurrentlyExecutingTasksToFinish {

        private CountDownLatch firstTaskIsExecuting = new CountDownLatch(1);
        private CountDownLatch clientIsWaitingForTasksToFinish = new CountDownLatch(1);
        private CountDownLatch secondTaskIsExecuting = new CountDownLatch(1);
        private CountDownLatch testHasEnded = new CountDownLatch(1);

        private volatile boolean aNewTaskIsRunning = false;

        public void create() throws InterruptedException {
            final Runnable task2 = new Runnable() {
                public void run() {
                    try {
                        aNewTaskIsRunning = true;
                        secondTaskIsExecuting.countDown();
                        testHasEnded.await();
                        aNewTaskIsRunning = false;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Runnable task1 = new Runnable() {
                public void run() {
                    try {
                        firstTaskIsExecuting.countDown();
                        clientIsWaitingForTasksToFinish.await();
                        taskQueue.add(new SimpleTaskBootstrap(task2));
                        secondTaskIsExecuting.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            taskQueue.add(new SimpleTaskBootstrap(task1));
            firstTaskIsExecuting.await();

            executeAfterCurrentThreadIsNotRunning(new Runnable() {
                public void run() {
                    // Let's hope that this gets executed *after* the client begins waiting.
                    // There is no guarantee that this thread won't be executed first...
                    clientIsWaitingForTasksToFinish.countDown();
                }
            });
            pool.awaitForCurrentTasksToFinish();
        }

        public void destroy() {
            testHasEnded.countDown();
        }

        public void afterWaitingAllThePreviouslyExecutingTasksHaveFinished() {
            if (aNewTaskIsRunning) {
                specify(pool.getRunningTasks(), should.equal(1));
            } else {
                specify(pool.getRunningTasks(), should.equal(0));
            }
        }

        public void afterWaitingOtherNewTasksMayBeExecuting() {
            specify(aNewTaskIsRunning);
        }
    }

    public class WhenATaskFails {

        private CountDownLatch end = new CountDownLatch(1);
        private RuntimeException exception = new RuntimeException("Dummy exception");

        public void create() throws InterruptedException {
            checking(theExceptionIsLogged());
            Runnable task = new Runnable() {
                public void run() {
                    end.countDown();
                    throw exception;
                }
            };
            taskQueue.add(new SimpleTaskBootstrap(task));
            end.await();
            pool.awaitForCurrentTasksToFinish();
        }

        public Expectations theExceptionIsLogged() {
            return new Expectations() {{
                one(logger).error("Task threw an exception", exception);
            }};
        }

        public void theNumberOfRunningTasksIsDecrementedCorrectly() {
            specify(pool.getRunningTasks(), should.equal(0));
        }
    }

    public class WhenThePoolIsShutDown {

        public void create() throws InterruptedException {
            checking(theShutdownIsLogged());
            pool.shutdown();
            taskQueue.add(new SimpleTaskBootstrap(new Runnable() {
                public void run() {
                    specify(false);
                }
            }));
            Thread.yield();
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        private Expectations theShutdownIsLogged() {
            return new Expectations() {{
                one(logger).info("Shutting down...");
                allowing(logger).info(with(equal("Task consumer was interrupted")), with(aNonNull(InterruptedException.class)));
                one(logger).info("Shutdown finished");
            }};
        }

        public void noMoreTasksAreTakenFromTheQueue() {
            specify(taskQueue.size(), should.equal(1));
        }
    }


    private static class SimpleTaskBootstrap implements TaskBootstrap {
        private final Runnable task;

        public SimpleTaskBootstrap(Runnable task) {
            this.task = task;
        }

        public Runnable getTaskInsideTransaction() {
            return task;
        }
    }

    // TODO: give access to the current task's ScheduledFuture?
}
