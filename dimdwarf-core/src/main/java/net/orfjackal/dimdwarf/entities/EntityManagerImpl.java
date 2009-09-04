// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.EntityId;
import net.orfjackal.dimdwarf.api.internal.*;
import net.orfjackal.dimdwarf.scopes.TaskScoped;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 25.8.2008
 */
@TaskScoped
@NotThreadSafe
public class EntityManagerImpl implements EntityManager {

    private final EntityIdFactory idFactory;
    private final EntityRepository repository;
    private final EntityApi entityApi;

    private final Map<EntityObject, EntityId> entities = new IdentityHashMap<EntityObject, EntityId>();
    private final Map<EntityId, EntityObject> entitiesById = new HashMap<EntityId, EntityObject>();
    private final Queue<EntityObject> flushQueue = new ArrayDeque<EntityObject>();
    private State state = State.ACTIVE;

    @Inject
    public EntityManagerImpl(EntityIdFactory idFactory, EntityRepository repository, EntityApi entityApi) {
        this.idFactory = idFactory;
        this.repository = repository;
        this.entityApi = entityApi;
    }

    @TestOnly
    int getRegisteredEntities() {
        return entities.size();
    }

    public EntityId getEntityId(EntityObject entity) {
        checkStateIs(State.ACTIVE, State.FLUSHING);
        checkIsEntity(entity);
        EntityId id = getIdOfLoadedEntity(entity);
        if (id == null) {
            id = createIdForNewEntity(entity);
        }
        return id;
    }

    private void checkIsEntity(EntityObject entity) {
        if (!entityApi.isEntity(entity)) {
            throw new IllegalArgumentException("Not an entity: " + entity);
        }
    }

    private EntityId getIdOfLoadedEntity(EntityObject entity) {
        return entities.get(entity);
    }

    private EntityId createIdForNewEntity(EntityObject entity) {
        EntityId id = idFactory.newId();
        register(entity, id);
        return id;
    }

    public EntityObject getEntityById(EntityId id) {
        checkStateIs(State.ACTIVE);
        EntityObject entity = getLoadedEntity(id);
        if (entity == null) {
            entity = loadEntityFromDatabase(id);
        }
        return entity;
    }

    @Nullable
    private EntityObject getLoadedEntity(EntityId id) {
        return entitiesById.get(id);
    }

    private EntityObject loadEntityFromDatabase(EntityId id) {
        EntityObject entity = (EntityObject) repository.read(id);
        register(entity, id);
        return entity;
    }

    private void register(EntityObject entity, EntityId id) {
        if (state == State.FLUSHING) {
            flushQueue.add(entity);
        }
        EntityId prevIdOfSameEntity = entities.put(entity, id);
        EntityObject prevEntityWithSameId = entitiesById.put(id, entity);
        assert prevIdOfSameEntity == null && prevEntityWithSameId == null : ""
                + "Registered an entity twise: " + entity + ", " + id
                + " (Previous was: " + prevEntityWithSameId + ", " + prevIdOfSameEntity + ")";
    }

    public EntityId firstKey() {
        checkStateIs(State.ACTIVE);
        return repository.firstKey();
    }

    public EntityId nextKeyAfter(EntityId currentKey) {
        checkStateIs(State.ACTIVE);
        return repository.nextKeyAfter(currentKey);
    }

    /**
     * Must be called before transaction deactivates, or the changes to entities will not be persisted.
     */
    public void flushAllEntitiesToDatabase() {
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
            EntityId id = entities.get(entity);
            repository.update(id, entity);
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
