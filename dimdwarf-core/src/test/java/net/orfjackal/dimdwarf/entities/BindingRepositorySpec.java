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
import net.orfjackal.dimdwarf.db.*;
import net.orfjackal.dimdwarf.db.inmemory.InMemoryDatabaseManager;
import net.orfjackal.dimdwarf.entities.dao.*;
import net.orfjackal.dimdwarf.entities.tref.EntityInfoImpl;
import net.orfjackal.dimdwarf.gc.entities.*;
import net.orfjackal.dimdwarf.modules.FakeGarbageCollectionModule;
import net.orfjackal.dimdwarf.serial.ObjectSerializerImpl;
import net.orfjackal.dimdwarf.tx.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 12.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class BindingRepositorySpec extends Specification<Object> {

    private BindingRepository repository;
    private DatabaseManager dbms;
    private TransactionCoordinator tx;
    private EntityManagerImpl entityManager;
    private Logger txLogger;

    public void create() throws Exception {
        dbms = new InMemoryDatabaseManager();
        txLogger = mock(Logger.class);
    }

    private void beginTask() {
        tx = new TransactionImpl(txLogger);
        // TODO: use guice instead of manual setup

        Database<Blob, Blob> db = dbms.openConnection(tx.getTransaction());
        DatabaseTable<Blob, Blob> bindingsTable = db.openTable("bindings");
        DatabaseTableWithMetadata<Blob, Blob> entitiesTable = new DatabaseTableWithMetadataImpl<Blob, Blob>(db, "entities");

        entityManager =
                new EntityManagerImpl(
                        new EntityIdFactoryImpl(BigInteger.ZERO),
                        new GcAwareEntityRepository(
                                new EntityDao(
                                        entitiesTable,
                                        new ConvertBigIntegerToBytes(),
                                        new NoConversion<Blob>()),
                                new ConvertEntityToBytes(
                                        new ObjectSerializerImpl()),
                                new FakeGarbageCollectionModule.NullMutatorListener(),
                                new EntityReferenceUtil()));

        repository =
                new GcAwareBindingRepository(
                        new BindingDao(
                                bindingsTable,
                                new ConvertStringToBytes(),
                                new ConvertBigIntegerToBytes()),
                        new ConvertEntityToEntityId(
                                entityManager,
                                new EntityInfoImpl(entityManager)),
                        new FakeGarbageCollectionModule.NullMutatorListener());
    }

    private void endTask() {
        entityManager.flushAllEntitiesToDatabase();
        tx.prepareAndCommit();
    }


    public class BindingLifecycle {

        public void create() {
            beginTask();
        }

        public void whenBindingHasNotBeenCreatedItDoesNotExist() {
            specify(repository.read("foo"), should.equal(null));
        }

        public void whenBindingIsCreatedItDoesExist() {
            repository.update("foo", new DummyEntity());
            specify(repository.read("foo"), should.not().equal(null));
        }

        public void whenBindingIsUpdatedItIsChanged() {
            DummyEntity d1 = new DummyEntity("1");
            DummyEntity d2 = new DummyEntity("2");
            repository.update("foo", d1);
            repository.update("foo", d2);
            specify(repository.read("foo"), should.equal(d2));
        }

        public void whenBindingIsDeletedItDoesNotExist() {
            repository.update("foo", new DummyEntity());
            repository.delete("foo");
            specify(repository.read("foo"), should.equal(null));
        }
    }

    public class BrowsingBindings {

        public void create() {
            beginTask();
            DummyEntity foo = new DummyEntity();
            foo.setOther("foo");
            repository.update("foo", foo);
            repository.update("foo.2", new DummyEntity());
            repository.update("foo.1", new DummyEntity());
            repository.update("bar.x", new DummyEntity());
            repository.update("bar.y", new DummyEntity());
            endTask();
            beginTask();
        }

        public void bindingsAreInAlphabeticalOrder() {
            specify(repository.firstKey(), should.equal("bar.x"));
        }

        public void whenBindingsHaveTheSamePrefixTheShortestBindingIsFirst() {
            specify(repository.nextKeyAfter("foo"), should.equal("foo.1"));
            specify(repository.nextKeyAfter("foo.1"), should.equal("foo.2"));
            specify(repository.nextKeyAfter("foo.2"), should.equal(null));
        }

        public void entitiesCanBeAccessedByTheBindingName() {
            DummyEntity entity = (DummyEntity) repository.read("foo");
            specify(entity.getOther(), should.equal("foo"));
        }
    }
}
