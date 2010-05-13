// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.EntityId;
import net.orfjackal.dimdwarf.db.Blob;
import net.orfjackal.dimdwarf.entities.dao.EntityDao;
import net.orfjackal.dimdwarf.serial.*;
import net.orfjackal.dimdwarf.tasks.TaskScoped;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Esko Luontola
 * @since 1.9.2008
 */
@TaskScoped
@NotThreadSafe
public class EntityRepositoryImpl implements EntitiesPersistedInDatabase {

    private final EntityDao entities;
    private final ObjectSerializer serializer;

    @Inject
    public EntityRepositoryImpl(EntityDao entities,
                                ObjectSerializer serializer) {
        this.entities = entities;
        this.serializer = serializer;
    }

    public boolean exists(EntityId id) {
        return entities.exists(id);
    }

    public Object read(EntityId id) {
        return readFromDatabase(id).getDeserializedObject();
    }

    private DeserializationResult readFromDatabase(EntityId id) {
        Blob bytes = entities.read(id);
        if (bytes.equals(Blob.EMPTY_BLOB)) {
            throw new EntityNotFoundException("id=" + id);
        }
        return serializer.deserialize(bytes);
    }

    public void update(EntityId id, Object entity) {
        SerializationResult newData = serializer.serialize(entity);
        if (hasBeenModified(id, newData)) {
            entities.update(id, newData.getSerializedBytes());
        }
    }

    private boolean hasBeenModified(EntityId id, SerializationResult newData) {
        // TODO: Compare with bytes from reserializing the original bytes, and not with the original bytes. See http://www.projectdarkstar.com/forum/?topic=1328.msg9107#msg9107
        Blob oldBytes = entities.read(id);
        Blob newBytes = newData.getSerializedBytes();
        return !oldBytes.equals(newBytes);
    }

    public void delete(EntityId id) {
        entities.delete(id);
    }

    public EntityId firstKey() {
        return entities.firstKey();
    }

    public EntityId nextKeyAfter(EntityId currentKey) {
        return entities.nextKeyAfter(currentKey);
    }
}
