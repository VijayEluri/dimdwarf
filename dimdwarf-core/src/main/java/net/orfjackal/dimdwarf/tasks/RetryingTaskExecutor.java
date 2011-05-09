// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.tasks;

import org.slf4j.*;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.*;
import java.util.concurrent.Executor;

@ThreadSafe
public class RetryingTaskExecutor implements Executor {

    // FIXME: RetryingTaskExecutor will be removed/refactored in new architecture

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RetryingTaskExecutor.class);
    private final Logger logger;

    private final Executor taskContext;
    private final Provider<RetryPolicy> retryPolicy;

    @Inject
    public RetryingTaskExecutor(@PlainTaskContext Executor taskContext, Provider<RetryPolicy> retryPolicy) {
        this(taskContext, retryPolicy, DEFAULT_LOGGER);
    }

    public RetryingTaskExecutor(Executor taskContext, Provider<RetryPolicy> retryPolicy, Logger logger) {
        this.taskContext = taskContext;
        this.retryPolicy = retryPolicy;
        this.logger = logger;
    }

    public void execute(Runnable command) {
        RetryPolicy policy = retryPolicy.get();
        while (true) {
            try {
                taskContext.execute(command);
                return;
            } catch (Throwable t) {
                if (shouldRetry(policy, t)) {
                    logger.info("Retrying a failed task");
                    continue;
                }
                throw new GivenUpOnTaskException("Not retrying the failed task", t);
            }
        }
    }

    private static boolean shouldRetry(RetryPolicy policy, Throwable t) {
        policy.taskHasFailed(t);
        return policy.shouldRetry();
    }
}
