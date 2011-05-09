// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.scheduler;

import net.orfjackal.dimdwarf.tasks.Task;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.*;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@ThreadSafe
public class TaskThreadPool {

    // FIXME: TaskThreadPool will be removed/refactored in new architecture

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskThreadPool.class);
    private final Logger logger;

    private final Executor taskContext;
    private final TaskProducer producer;
    private final Thread consumer;
    private final ExecutorService workers;
    private final Set<CountDownLatch> runningTasks = Collections.synchronizedSet(new HashSet<CountDownLatch>());
    private final AtomicInteger waitingForCurrentTasksToFinish = new AtomicInteger(0);
    private volatile boolean shutdown = false;

    @Inject
    public TaskThreadPool(@Task Executor taskContext, TaskProducer producer, ExecutorService threadPool) {
        this(taskContext, producer, threadPool, DEFAULT_LOGGER);
    }

    public TaskThreadPool(Executor taskContext, TaskProducer producer, ExecutorService threadPool, Logger logger) {
        this.taskContext = taskContext;
        this.producer = producer;
        this.consumer = new Thread(new TaskConsumer(), "Consume Scheduled Tasks");
        this.workers = threadPool;
        this.logger = logger;
    }

    public void start() {
        consumer.start();
    }

    public void shutdown() {
        logger.info("Shutting down {}...", getClass().getSimpleName());
        shutdownConsumer();
        shutdownWorkers();
        logger.info("{} has been shut down", getClass().getSimpleName());
    }

    private void shutdownConsumer() {
        shutdown = true;
        consumer.interrupt();
        try {
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while shutting down", e);
            throw new RuntimeException(e);
        }
    }

    private void shutdownWorkers() {
        workers.shutdown();
        try {
            awaitForCurrentTasksToFinish();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            workers.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while shutting down", e);
            throw new RuntimeException(e);
        }
    }

    public int getRunningTasks() {
        return runningTasks.size();
    }

    @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
    public void awaitForCurrentTasksToFinish() throws InterruptedException {
        // It would be dangerous to pass an array larger than 0 to the toArray() method,
        // because there is a small chance that between the calls to size() and toArray()
        // an entry is removed from the collection, and the returned array would be too
        // big and would contain a null entry (toArray() does not shrink the array parameter
        // if it's too big).
        CountDownLatch[] snapshotOfRunningTasks = runningTasks.toArray(new CountDownLatch[0]);
        waitingForCurrentTasksToFinish.incrementAndGet();
        try {
            for (CountDownLatch taskHasFinished : snapshotOfRunningTasks) {
                taskHasFinished.await();
            }
        } finally {
            waitingForCurrentTasksToFinish.decrementAndGet();
        }
    }

    @TestOnly
    int getWaitingForCurrentTasksToFinishCount() {
        return waitingForCurrentTasksToFinish.get();
    }


    private class TaskConsumer implements Runnable {
        public void run() {
            while (!shutdown) {
                try {
                    TaskBootstrap bootstrap = producer.takeNextTask();
                    workers.submit(new TaskContextSetup(new Bootstrapper(bootstrap)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Task consumer was interrupted", e);
                    return;
                }
            }
        }
    }

    private class TaskContextSetup implements Runnable {
        private final Runnable task;

        public TaskContextSetup(Runnable task) {
            this.task = task;
        }

        public void run() {
            CountDownLatch taskHasFinished = new CountDownLatch(1);
            try {
                runningTasks.add(taskHasFinished);
                taskContext.execute(task);
            } catch (Throwable t) {
                logger.error("Task threw an exception", t);
            } finally {
                runningTasks.remove(taskHasFinished);
                taskHasFinished.countDown();
            }
        }
    }

    private static class Bootstrapper implements Runnable {
        private final TaskBootstrap bootstrap;

        public Bootstrapper(TaskBootstrap bootstrap) {
            this.bootstrap = bootstrap;
        }

        public void run() {
            Runnable task = bootstrap.getTaskInsideTransaction();
            task.run();
        }
    }
}
