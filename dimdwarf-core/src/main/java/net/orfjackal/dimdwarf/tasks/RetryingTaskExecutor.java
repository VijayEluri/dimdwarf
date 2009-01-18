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

package net.orfjackal.dimdwarf.tasks;

import com.google.inject.*;
import org.slf4j.*;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Executor;

/**
 * @author Esko Luontola
 * @since 13.12.2008
 */
@Immutable
public class RetryingTaskExecutor implements Executor {
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
