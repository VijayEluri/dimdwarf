// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.modules;

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.EntityInfo;
import net.orfjackal.dimdwarf.api.internal.*;
import net.orfjackal.dimdwarf.entities.*;
import net.orfjackal.dimdwarf.entities.dao.*;
import net.orfjackal.dimdwarf.entities.tref.*;
import net.orfjackal.dimdwarf.serial.*;

import static net.orfjackal.dimdwarf.modules.DatabaseModule.*;

public class EntityModule extends AbstractModule {

    protected void configure() {
        bind(EntityApi.class).to(DimdwarfEntityApi.class);

        bind(AllEntities.class).to(EntityManager.class);
        bind(EntitiesLoadedInMemory.class).to(EntityManager.class);
        bind(EntitiesPersistedInDatabase.class).to(EntityRepository.class);
        bind(EntityReferenceFactory.class).to(EntityReferenceFactoryImpl.class);
        bind(EntityInfo.class).to(TrefAwareEntityInfo.class);

        bind(Long.class)
                .annotatedWith(MaxEntityId.class)
                .toInstance(0L); // TODO: import from database

        bind(databaseTableConnection())
                .annotatedWith(EntitiesTable.class)
                .toProvider(databaseTable("entities"));
        bind(databaseTableConnection())
                .annotatedWith(BindingsTable.class)
                .toProvider(databaseTable("bindings"));
    }

    @Provides
    SerializationListener[] serializationListeners(CheckInnerClassSerialized listener1,
                                                   CheckDirectlyReferredEntitySerialized listener2,
                                                   InjectObjectsOnDeserialization listener3,
                                                   EntityIdSerializationListener listener4) {
        return new SerializationListener[]{listener1, listener2, listener3, listener4};
    }

    @Provides
    SerializationReplacer[] serializationReplacers(ReplaceEntitiesWithTransparentReferences replacer1) {
        return new SerializationReplacer[]{replacer1};
    }
}
