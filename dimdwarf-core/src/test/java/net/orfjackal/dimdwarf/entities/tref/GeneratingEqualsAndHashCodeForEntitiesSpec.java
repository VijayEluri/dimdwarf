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

package net.orfjackal.dimdwarf.entities.tref;

import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.aop.*;
import net.orfjackal.dimdwarf.aop.conf.AbstractTransformationChain;
import net.orfjackal.dimdwarf.api.internal.*;
import net.orfjackal.dimdwarf.context.*;
import net.orfjackal.dimdwarf.entities.*;
import org.junit.runner.RunWith;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 9.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class GeneratingEqualsAndHashCodeForEntitiesSpec extends Specification<Object> {

    private int referencesCreated = 0;
    private Object target;

    public void create() throws Exception {
        ReferenceFactory factory = new ReferenceFactory() {
            public <T> EntityReference<T> createReference(T entity) {
                referencesCreated++;
                return new EntityReferenceImpl<T>(BigInteger.ONE, entity);
            }
        };
        ThreadContext.setUp(new FakeContext().with(ReferenceFactory.class, factory));
    }

    public void destroy() throws Exception {
        ThreadContext.tearDown();
    }

    private static Object newInstrumentedInstance(Class<?> cls) throws Exception {
        ClassLoader loader = new TransformationTestClassLoader(cls.getName(), new AbstractTransformationChain() {
            protected ClassVisitor getAdapters(ClassVisitor cv) {
                cv = new CheckClassAdapter(cv);
                cv = new AddEqualsAndHashCodeMethodsForEntities(cv);
                return cv;
            }
        });
        return loader.loadClass(cls.getName()).newInstance();
    }


    public class AnInstrumentedNormalObject {

        public void create() throws Exception {
            target = newInstrumentedInstance(DummyObject.class);
        }

        public void doesNotDelegateItsEqualsMethod() {
            target.equals(new Object());
            specify(referencesCreated, should.equal(0));
        }

        public void doesNotDelegateItsHashCodeMethod() {
            target.hashCode();
            specify(referencesCreated, should.equal(0));
        }
    }

    public class AnInstrumentedEntityWithNoEqualsAndHashCodeMethods {

        public void create() throws Exception {
            target = newInstrumentedInstance(DummyEntity.class);
        }

        public void delegatesItsEqualsMethodToEntityIdentity() {
            target.equals(new Object());
            specify(referencesCreated, should.equal(1));
        }

        public void delegatesItsHashCodeMethodToEntityIdentity() {
            target.hashCode();
            specify(referencesCreated, should.equal(1));
        }
    }

    public class AnInstrumentedEntityWithACustomEqualsMethod {

        public void create() throws Exception {
            target = newInstrumentedInstance(EntityWithEquals.class);
        }

        public void doesNotDelegateItsEqualsMethod() {
            target.equals(new Object());
            specify(referencesCreated, should.equal(0));
        }

        public void delegatesItsHashCodeMethodToEntityIdentity() {
            target.hashCode();
            specify(referencesCreated, should.equal(1));
        }
    }

    public class AnInstrumentedEntityWithACustomHashCodeMethod {

        public void create() throws Exception {
            target = newInstrumentedInstance(EntityWithHashCode.class);
        }

        public void delegatesItsEqualsMethodToEntityIdentity() {
            target.equals(new Object());
            specify(referencesCreated, should.equal(1));
        }

        public void doesNotDelegateItsHashCodeMethod() {
            target.hashCode();
            specify(referencesCreated, should.equal(0));
        }
    }


    public static class EntityWithEquals implements EntityObject {
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    public static class EntityWithHashCode implements EntityObject {
        public int hashCode() {
            return super.hashCode();
        }
    }
}
