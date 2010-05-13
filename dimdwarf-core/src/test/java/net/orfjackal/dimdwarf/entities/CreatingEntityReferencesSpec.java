// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.internal.*;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 25.8.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class CreatingEntityReferencesSpec extends Specification<Object> {

    private static final EntitiesPersistedInDatabase UNUSED_DATABASE = null;

    private EntityIdFactory idFactory;
    private EntityManager manager;
    private EntityReferenceFactory refFactory;
    private EntityObject entity;

    public void create() throws Exception {
        idFactory = mock(EntityIdFactory.class);
        manager = new EntityManager(idFactory, UNUSED_DATABASE, new DimdwarfEntityApi());
        refFactory = new EntityReferenceFactoryImpl(manager);
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
                one(idFactory).newId(); will(returnValue(new EntityObjectId(42)));
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
            specify(ref.getEntityId(), should.equal(new EntityObjectId(42)));
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
                one(idFactory).newId(); will(returnValue(new EntityObjectId(1)));
                one(idFactory).newId(); will(returnValue(new EntityObjectId(2)));
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
            specify(ref1.getEntityId(), should.equal(new EntityObjectId(1)));
            specify(ref2.getEntityId(), should.equal(new EntityObjectId(2)));
        }
    }
}
