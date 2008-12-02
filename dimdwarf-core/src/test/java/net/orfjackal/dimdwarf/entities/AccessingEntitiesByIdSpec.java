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

package net.orfjackal.dimdwarf.entities;

import jdave.*;
import jdave.junit4.JDaveRunner;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 12.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class AccessingEntitiesByIdSpec extends Specification<Object> {

    private EntityRepository repository;
    private EntityManager manager;

    private DummyEntity entity1 = new DummyEntity();
    private BigInteger id1 = BigInteger.valueOf(1);
    private BigInteger id2 = BigInteger.valueOf(2);

    public void create() throws Exception {
        repository = mock(EntityRepository.class);
        manager = new EntityManagerImpl(mock(EntityIdFactory.class), repository);
    }

    private Expectations loadsFromRepository(final BigInteger id, final DummyEntity entity) {
        return new Expectations() {{
            one(repository).read(id); will(returnValue(entity));
        }};
    }


    public class WhenThereAreEntitiesInTheDatabase {

        public void entitiesCanBeLoadedById() {
            checking(loadsFromRepository(id1, entity1));
            specify(manager.getEntityById(id1), should.equal(entity1));
        }

        public void entitiesAreRegisteredOnLoadById() {
            checking(loadsFromRepository(id1, entity1));
            Object load1 = manager.getEntityById(id1);
            Object load2 = manager.getEntityById(id1);
            specify(load1 == load2);
        }

        public void entitiesCanBeIteratedById() {
            checking(new Expectations() {{
                one(repository).firstKey(); will(returnValue(id1));
                one(repository).nextKeyAfter(id1); will(returnValue(id2));
            }});
            specify(manager.firstKey(), should.equal(id1));
            specify(manager.nextKeyAfter(id1), should.equal(id2));
        }
    }
}
