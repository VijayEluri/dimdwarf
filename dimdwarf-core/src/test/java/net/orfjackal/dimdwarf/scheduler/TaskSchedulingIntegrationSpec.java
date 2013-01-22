// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.scheduler;

import com.google.inject.*;
import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.TaskScheduler;
import net.orfjackal.dimdwarf.db.inmemory.InMemoryDatabaseManager;
import net.orfjackal.dimdwarf.entities.EntityIdFactory;
import net.orfjackal.dimdwarf.modules.CommonModules;
import net.orfjackal.dimdwarf.server.TestServer;
import net.orfjackal.dimdwarf.tasks.TaskExecutor;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.*;

@RunWith(JDaveRunner.class)
@Group({"fast"})
public class TaskSchedulingIntegrationSpec extends Specification<Object> {

    // FIXME: task scheduling will be removed/refactored in new architecture

    private Injector injector;
    private Executor taskContext;
    private Provider<TaskScheduler> scheduler;
    private TestSpy spy;
    private TestServer server;

    public void create() throws Exception {
        startupTheServer(
                new CommonModules()
        );
    }

    public void destroy() throws Exception {
        shutdownTheServer();
    }

    private void startupTheServer(Module... modules) {
        server = new TestServer(modules);
        server.hideStartupShutdownLogs();
        server.start();

        injector = server.getInjector();
        taskContext = injector.getInstance(TaskExecutor.class);
        scheduler = injector.getProvider(TaskScheduler.class);
        spy = injector.getInstance(TestSpy.class);
    }

    private void shutdownTheServer() {
        server.shutdownIfRunning();
    }

    private void restartTheServer() {
        // XXX: add support for storing the database to a file (or byte array in here), and do not recycle these object instances
        final InMemoryDatabaseManager dbBackup = injector.getInstance(InMemoryDatabaseManager.class);
        final EntityIdFactory idFactoryBackup = injector.getInstance(EntityIdFactory.class);
        startupTheServer(
                new CommonModules(),
                new AbstractModule() {
                    protected void configure() {
                        bind(InMemoryDatabaseManager.class).toInstance(dbBackup);
                        bind(EntityIdFactory.class).toInstance(idFactoryBackup);
                    }
                });
    }


    public class WhenAOneTimeTaskIsScheduled {

        public void create() throws InterruptedException {
            taskContext.execute(new Runnable() {
                public void run() {
                    scheduler.get().submit(new ExecutionLoggingTask("A"));
                }
            });
            spy.executionCount.acquire(1);
        }

        public void itIsExecutedOnce() {
            specify(spy.executions, should.containInOrder("A:1"));
        }
    }

    public class WhenARepeatedTaskIsScheduled {

        public void create() throws InterruptedException {
            taskContext.execute(new Runnable() {
                public void run() {
                    scheduler.get().scheduleAtFixedRate(new ExecutionLoggingTask("A"), 0, 0, TimeUnit.MILLISECONDS);
                }
            });
            spy.executionCount.acquire(2);
        }

        public void itIsExecutedManyTimes() {
            specify(spy.executions, should.containAll("A:1", "A:2"));
        }

        public void afterShuttingDownItIsNoMoreExecuted() throws InterruptedException {
            shutdownTheServer();
            spy.executions.clear();
            Thread.sleep(10);
            specify(spy.executions, should.containExactly());
        }

        public void afterRestartTheExecutionIsContinuedFromWhereItWasLeft() throws InterruptedException {
            shutdownTheServer();

            List<String> executedBeforeRestart = new ArrayList<>(spy.executions);
            specify(executedBeforeRestart, executedBeforeRestart.size() >= 2);

            restartTheServer();
            spy.executionCount.acquire(1);

            // If there were some executions and none of the two first executions are included
            // (i.e. the execution count was correctly reset on shutdown), then the server must
            // have continued execution after restart.
            specify(spy.executions, spy.executions.size() >= 1);
            specify(spy.executions, should.not().containAny(executedBeforeRestart));

            // If the sequence of execution numbers has no holes, then the server must have
            // continued execution exactly from where it was left.
            String lastBeforeRestart = executedBeforeRestart.get(executedBeforeRestart.size() - 1);
            int lastCountBeforeRestart = Integer.parseInt(lastBeforeRestart.split(":")[1]);
            String firstAfterRestart = spy.executions.get(0);
            specify(firstAfterRestart, should.equal("A:" + (lastCountBeforeRestart + 1)));
        }
    }


    @Singleton
    public static class TestSpy {

        public final Semaphore executionCount = new Semaphore(0);
        public final List<String> executions = Collections.synchronizedList(new ArrayList<String>());

        public void logExecution(String dummyId, int count) {
            executions.add(dummyId + ":" + count);
            executionCount.release();
        }
    }

    private static class ExecutionLoggingTask extends DummyTask {

        @Inject public transient TestSpy spy;
        private int myExecutionCount = 0;

        public ExecutionLoggingTask(String dummyId) {
            super(dummyId);
        }

        public void run() {
            myExecutionCount++;
            spy.logExecution(getDummyId(), myExecutionCount);
        }
    }
}
