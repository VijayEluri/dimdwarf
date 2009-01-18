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

package net.orfjackal.dimdwarf.entities;

import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.internal.*;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 25.8.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class CreatingEntityReferencesSpec extends Specification<Object> {

    private EntityIdFactory idFactory;
    private EntityRepository repository;
    private EntityManagerImpl manager;
    private ReferenceFactory refFactory;
    private EntityObject entity;

    public void create() throws Exception {
        idFactory = mock(EntityIdFactory.class);
        repository = mock(EntityRepository.class);
        manager = new EntityManagerImpl(idFactory, repository);
        refFactory = new ReferenceFactoryImpl(manager);
        entity = new DummyEntity();
    }


    public class WhenNoReferencesHaveBeenCreated {

        public void noEntitiesAreRegistered() {
            specify(manager.getRegisteredEntities(), should.equal(0));
        }
    }

    public class WhenAReferenceIsCreated {

        private EntityReference<EntityObject> ref;

        public void create() {
            checking(new Expectations() {{
                one(idFactory).newId(); will(returnValue(BigInteger.valueOf(42)));
            }});
            ref = refFactory.createReference(entity);
        }

        public void theReferenceIsCreated() {
            specify(ref, should.not().equal(null));
        }

        public void theEntityIsRegistered() {
            specify(manager.getRegisteredEntities(), should.equal(1));
        }

        public void theEntityGetsAnId() {
            specify(ref.getEntityId(), should.equal(BigInteger.valueOf(42)));
        }

        public void onMultipleCallsAllReferencesToTheSameObjectAreEqual() {
            EntityReference<EntityObject> ref2 = refFactory.createReference(entity);
            specify(ref2 != ref);
            specify(ref2, should.equal(ref));
        }

        public void onMultipleCallsTheEntityIsRegisteredOnlyOnce() {
            refFactory.createReference(entity);
            specify(manager.getRegisteredEntities(), should.equal(1));
        }
    }

    public class WhenReferencesToManyEntitiesAreCreated {

        private EntityReference<EntityObject> ref1;
        private EntityReference<DummyEntity> ref2;

        public void create() {
            checking(new Expectations() {{
                one(idFactory).newId(); will(returnValue(BigInteger.valueOf(1)));
                one(idFactory).newId(); will(returnValue(BigInteger.valueOf(2)));
            }});
            ref1 = refFactory.createReference(entity);
            ref2 = refFactory.createReference(new DummyEntity());
        }

        public void allTheEntitiesAreRegistered() {
            specify(manager.getRegisteredEntities(), should.equal(2));
        }

        public void eachEntityGetsItsOwnReference() {
            specify(ref1, should.not().equal(ref2));
        }

        public void eachEntityGetsItsOwnId() {
            specify(ref1.getEntityId(), should.equal(BigInteger.valueOf(1)));
            specify(ref2.getEntityId(), should.equal(BigInteger.valueOf(2)));
        }
    }
}
