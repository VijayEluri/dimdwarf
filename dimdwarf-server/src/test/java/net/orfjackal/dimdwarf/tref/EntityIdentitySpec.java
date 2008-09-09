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

package net.orfjackal.dimdwarf.tref;

import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.internal.Entity;
import net.orfjackal.dimdwarf.api.internal.EntityManager;
import net.orfjackal.dimdwarf.api.internal.TransparentReference;
import net.orfjackal.dimdwarf.context.ContextImpl;
import net.orfjackal.dimdwarf.context.ThreadContext;
import net.orfjackal.dimdwarf.entities.DummyEntity;
import net.orfjackal.dimdwarf.entities.EntityReferenceImpl;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 5.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class EntityIdentitySpec extends Specification<Object> {

    private EntityManager entityManager;
    protected TransparentReferenceFactory factory;
    private Entity ent1;
    private Entity ent2;
    private TransparentReference tref1;
    private TransparentReference tref1b;
    private TransparentReference tref2;
    private Object obj;

    public void create() throws Exception {
        entityManager = mock(EntityManager.class);
        factory = new TransparentReferenceFactoryImpl(entityManager);
        ent1 = new DummyEntity();
        ent2 = new DummyEntity();
        checking(referencesMayBeCreatedFor(ent1, BigInteger.valueOf(1)));
        checking(referencesMayBeCreatedFor(ent2, BigInteger.valueOf(2)));
        tref1 = factory.createTransparentReference(ent1);
        tref1b = factory.createTransparentReference(ent1);
        tref2 = factory.createTransparentReference(ent2);
        obj = new Object();
        ThreadContext.setUp(new ContextImpl(entityManager));
    }

    public void destroy() throws Exception {
        ThreadContext.tearDown();
    }

    private Expectations referencesMayBeCreatedFor(final Entity entity, final BigInteger id) {
        return new Expectations() {{
            allowing(entityManager).createReference(entity); will(returnValue(new EntityReferenceImpl<Entity>(id, entity)));
        }};
    }


    public class EntityIdentityContractsWhenUsingTransparentReferences {

        public void entityEqualsTheSameEntity() {
            specify(EntityIdentity.equals(ent1, ent1));
            specify(EntityIdentity.equals(ent1, ent2), should.equal(false));
        }

        public void entityEqualsTransparentReferenceForTheSameEntity() {
            specify(EntityIdentity.equals(ent1, tref1));
            specify(EntityIdentity.equals(tref1, ent1));
            specify(EntityIdentity.equals(ent1, tref2), should.equal(false));
            specify(EntityIdentity.equals(tref2, ent1), should.equal(false));
        }

        public void transparentReferenceEqualsTransparentReferenceForTheSameEntity() {
            specify(tref1 != tref1b);
            specify(EntityIdentity.equals(tref1, tref1));
            specify(EntityIdentity.equals(tref1, tref1b));
            specify(EntityIdentity.equals(tref1b, tref1));
            specify(EntityIdentity.equals(tref1, tref2), should.equal(false));
        }

        public void entityDoesNotEqualOtherObjects() {
            specify(EntityIdentity.equals(ent1, obj), should.equal(false));
            specify(EntityIdentity.equals(obj, ent1), should.equal(false));
        }

        public void transparentReferenceDoesNotEqualOtherObjects() {
            specify(EntityIdentity.equals(tref1, obj), should.equal(false));
            specify(EntityIdentity.equals(obj, tref1), should.equal(false));
        }

        public void entityDoesNotEqualNull() {
            specify(EntityIdentity.equals(ent1, null), should.equal(false));
            specify(EntityIdentity.equals(null, ent1), should.equal(false));
        }

        public void transparentReferenceDoesNotEqualNull() {
            specify(EntityIdentity.equals(tref1, null), should.equal(false));
            specify(EntityIdentity.equals(null, tref1), should.equal(false));
        }

        public void differentEntitiesHaveDifferentHashCodes() {
            int hc1 = EntityIdentity.hashCode(ent1);
            int hc2 = EntityIdentity.hashCode(ent2);
            specify(hc1, should.not().equal(hc2));
        }

        public void transparentReferencesForDifferentEntitiesHaveDifferentHashCodes() {
            int hc1 = EntityIdentity.hashCode(tref1);
            int hc2 = EntityIdentity.hashCode(tref2);
            specify(hc1, should.not().equal(hc2));
        }

        public void transparentReferencesForTheSameEntityHaveTheSameHashCode() {
            int hc1 = EntityIdentity.hashCode(tref1);
            int hc1b = EntityIdentity.hashCode(tref1b);
            specify(hc1, should.equal(hc1b));
        }

        public void entitiesAndTheirTransparentReferencesHaveTheSameHashCode() {
            specify(EntityIdentity.hashCode(ent1), EntityIdentity.hashCode(tref1));
            specify(EntityIdentity.hashCode(ent2), EntityIdentity.hashCode(tref2));
        }

        public void equalsMethodOnProxyWillNotDelegateToEntity() {
            final Entity entity = mock(Entity.class);
            checking(referencesMayBeCreatedFor(entity, BigInteger.valueOf(3)));
            TransparentReference proxy = factory.createTransparentReference(entity);
            proxy.equals(entity);
        }

        public void hashCodeMethodOnProxyWillNotDelegateToEntity() {
            final Entity entity = mock(Entity.class);
            checking(referencesMayBeCreatedFor(entity, BigInteger.valueOf(3)));
            TransparentReference proxy = factory.createTransparentReference(entity);
            proxy.hashCode();
        }
    }
}
