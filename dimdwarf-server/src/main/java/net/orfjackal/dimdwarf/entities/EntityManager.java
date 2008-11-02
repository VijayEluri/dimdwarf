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

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.internal.Entities;
import net.orfjackal.dimdwarf.api.internal.EntityObject;
import net.orfjackal.dimdwarf.api.internal.EntityReference;
import net.orfjackal.dimdwarf.scopes.TaskScoped;
import net.orfjackal.dimdwarf.tx.Transaction;
import net.orfjackal.dimdwarf.tx.TransactionListener;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.*;

/**
 * This class is NOT thread-safe.
 *
 * @author Esko Luontola
 * @since 25.8.2008
 */
@TaskScoped
public class EntityManager implements ReferenceFactory, EntityLoader, TransactionListener {

    // TODO: separate responsibilities:
    // - keeping track of in-memory entities (LoadedEntitiesRegistry)
    // - loading entities from database (merge with above?)
    // - flushing entities to database (merge with above?)
    // - creating references (ReferenceFactoryImpl)

    private final Map<EntityObject, BigInteger> entities = new IdentityHashMap<EntityObject, BigInteger>();
    private final Map<BigInteger, EntityObject> entitiesById = new HashMap<BigInteger, EntityObject>();
    private final Queue<EntityObject> flushQueue = new ArrayDeque<EntityObject>();
    private final EntityIdFactory idFactory;
    private final EntityStorage storage;
    private State state = State.ACTIVE;

    @Inject
    public EntityManager(EntityIdFactory idFactory, EntityStorage storage, Transaction tx) {
        this.idFactory = idFactory;
        this.storage = storage;
        tx.addTransactionListener(this);
    }

    @TestOnly
    int getRegisteredEntities() {
        return entities.size();
    }

    // ReferenceFactory

    public <T> EntityReference<T> createReference(T entity) {
        checkStateIs(State.ACTIVE, State.FLUSHING);
        checkIsEntity(entity);
        BigInteger id = getEntityId((EntityObject) entity);
        return new EntityReferenceImpl<T>(id, entity);
    }

    private static void checkIsEntity(Object obj) {
        if (!Entities.isEntity(obj)) {
            throw new IllegalArgumentException("Not an entity: " + obj);
        }
    }

    private BigInteger getEntityId(EntityObject entity) {
        BigInteger id = getIdOfLoadedEntity(entity);
        if (id == null) {
            id = createIdForNewEntity(entity);
        }
        return id;
    }

    private BigInteger getIdOfLoadedEntity(EntityObject entity) {
        return entities.get(entity);
    }

    private BigInteger createIdForNewEntity(EntityObject entity) {
        BigInteger id = idFactory.newId();
        register(entity, id);
        return id;
    }

    // EntityLoader

    public Object loadEntity(BigInteger id) {
        checkStateIs(State.ACTIVE);
        EntityObject entity = getLoadedEntity(id);
        if (entity == null) {
            entity = loadEntityFromDatabase(id);
        }
        return entity;
    }

    private EntityObject getLoadedEntity(BigInteger id) {
        return entitiesById.get(id);
    }

    private EntityObject loadEntityFromDatabase(BigInteger id) {
        EntityObject entity = (EntityObject) storage.read(id);
        register(entity, id);
        return entity;
    }

    private void register(EntityObject entity, BigInteger id) {
        if (state == State.FLUSHING) {
            flushQueue.add(entity);
        }
        Object previous1 = entities.put(entity, id);
        Object previous2 = entitiesById.put(id, entity);
        assert previous1 == null && previous2 == null : "Registered an entity twise: " + entity + ", " + id;
    }

    // IterableKeys

    public BigInteger firstKey() {
        checkStateIs(State.ACTIVE);
        return storage.firstKey();
    }

    public BigInteger nextKeyAfter(BigInteger currentKey) {
        checkStateIs(State.ACTIVE);
        return storage.nextKeyAfter(currentKey);
    }

    public void transactionWillDeactivate(Transaction tx) {
        flushAllEntities();
    }

    public void flushAllEntities() {
        beginFlush();
        flush();
        endFlush();
    }

    private void beginFlush() {
        checkStateIs(State.ACTIVE);
        state = State.FLUSHING;
        assert flushQueue.isEmpty();
        flushQueue.addAll(entities.keySet());
    }

    private void flush() {
        EntityObject entity;
        while ((entity = flushQueue.poll()) != null) {
            BigInteger id = entities.get(entity);
            storage.update(id, entity);
        }
    }

    private void endFlush() {
        checkStateIs(State.FLUSHING);
        state = State.CLOSED;
        assert flushQueue.isEmpty();
    }

    private void checkStateIs(State... expectedStates) {
        for (State expected : expectedStates) {
            if (state == expected) {
                return;
            }
        }
        throw new IllegalStateException("Expected state " + Arrays.toString(expectedStates) + " but was " + state);
    }

    private enum State {
        ACTIVE, FLUSHING, CLOSED
    }
}
