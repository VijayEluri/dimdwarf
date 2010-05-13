// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.aop;

import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.aop.conf.*;
import net.orfjackal.dimdwarf.api.Entity;
import net.orfjackal.dimdwarf.api.internal.*;
import net.orfjackal.dimdwarf.entities.DummyObject;
import org.junit.runner.RunWith;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

@RunWith(JDaveRunner.class)
@Group({"fast"})
public class AddMarkerInterfaceForEntitiesSpec extends Specification<Object> {

    private Object target;
    private EntityApi entityApi = new DimdwarfEntityApi();

    private static Object newInstrumentedInstance(Class<?> cls) throws Exception {
        ClassLoader loader = new TransformationTestClassLoader(cls.getName(), new AbstractTransformationChain() {
            protected ClassVisitor getAdapters(ClassVisitor cv) {
                cv = new CheckClassAdapter(cv);
                cv = new AddMarkerInterfaceForEntities(new DimdwarfAopApi(), cv);
                return cv;
            }
        });
        return loader.loadClass(cls.getName()).newInstance();
    }


    public class AClassWithNoAnnotations {

        public void create() throws Exception {
            target = newInstrumentedInstance(DummyObject.class);
        }

        public void isNotTransformed() {
            specify(entityApi.isEntity(target), should.equal(false));
        }
    }

    public class AClassWithTheEntityAnnotation {

        public void create() throws Exception {
            target = newInstrumentedInstance(AnnotatedEntity.class);
        }

        public void isTransformedToAnEntity() {
            specify(entityApi.isEntity(target));
        }
    }

    public class AClassWithTheEntityAnnotationAndMarkerInterface {

        public void doesNotHaveTheSameInterfaceAddedTwise() throws Exception {
            // The class loader will throw ClassFormatError if the same interface is declared twise.
            newInstrumentedInstance(AnnotatedEntityWithMarkerInterface.class);
        }
    }


    @Entity
    public static class AnnotatedEntity {
    }

    @Entity
    public static class AnnotatedEntityWithMarkerInterface implements EntityObject {
    }
}
