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

package net.orfjackal.dimdwarf.context;

import jdave.Block;
import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.entities.EntityManager;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 5.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class ThreadContextSpec extends Specification<Object> {

    private Context myContext;

    public void create() throws Exception {
        myContext = new DummyContext();
    }

    public void destroy() throws Exception {
        if (ThreadContext.get() != null) {
            ThreadContext.tearDown();
        }
    }


    public class AThreadContext {

        private boolean wasExecuted;

        public Object create() {
            return null;
        }

        public void isEmptyBeforeSetup() {
            specify(ThreadContext.get(), should.equal(null));
        }

        public void isActiveAfterSetup() {
            ThreadContext.setUp(myContext);
            specify(ThreadContext.get(), should.equal(myContext));
        }

        public void isEmptyAfterTearingDown() {
            ThreadContext.setUp(myContext);
            ThreadContext.tearDown();
            specify(ThreadContext.get(), should.equal(null));
        }

        public void canNotBeSetUpTwise() {
            ThreadContext.setUp(myContext);
            specify(new Block() {
                public void run() throws Throwable {
                    ThreadContext.setUp(new DummyContext());
                }
            }, should.raise(IllegalStateException.class));
            specify(ThreadContext.get(), should.equal(myContext));
        }

        public void canNotBeTornDownTwise() {
            ThreadContext.setUp(myContext);
            ThreadContext.tearDown();
            specify(new Block() {
                public void run() throws Throwable {
                    ThreadContext.tearDown();
                }
            }, should.raise(IllegalStateException.class));
            specify(ThreadContext.get(), should.equal(null));
        }

        public void isSpecificToTheCurrentThread() throws InterruptedException {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    ThreadContext.setUp(myContext);
                }
            });
            t.start();
            t.join();
            specify(ThreadContext.get(), should.equal(null));
        }


        public void providesAHelperMethodWithAutomaticSetUpAndTearDown() {
            wasExecuted = false;
            ThreadContext.runInContext(myContext, new Runnable() {
                public void run() {
                    wasExecuted = true;
                    specify(ThreadContext.get(), should.equal(myContext));
                }
            });
            specify(wasExecuted);
            specify(ThreadContext.get(), should.equal(null));
        }

        public void theHelperMethodDoesTearDownEvenWhenExceptionsAreThrown() {
            final Runnable maliciousTask = new Runnable() {
                public void run() {
                    throw new InternalError("dummy exception");
                }
            };
            specify(new Block() {
                public void run() throws Throwable {
                    ThreadContext.runInContext(myContext, maliciousTask);
                }
            }, should.raise(InternalError.class, "dummy exception"));
            specify(ThreadContext.get(), should.equal(null));
        }
    }


    private static class DummyContext implements Context {
        public EntityManager getEntityManager() {
            return null;
        }
    }
}
