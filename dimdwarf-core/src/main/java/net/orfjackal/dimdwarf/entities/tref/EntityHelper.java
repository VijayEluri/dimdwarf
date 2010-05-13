// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities.tref;

import net.orfjackal.dimdwarf.api.internal.*;
import net.orfjackal.dimdwarf.context.ThreadContext;
import net.orfjackal.dimdwarf.entities.EntityReferenceFactory;
import net.orfjackal.dimdwarf.util.Objects;

import javax.annotation.Nullable;

/**
 * For transparent references to work correctly, all subclasses of {@link EntityObject} should
 * define their {@link #equals(Object)} and {@link #hashCode()} methods as follows:
 * <pre><code>
 * public boolean equals(Object obj) {
 *     return EntityHelper.equals(this, obj);
 * }
 * public int hashCode() {
 *     return EntityHelper.hashCode(this);
 * }
 * </code></pre>
 */
public class EntityHelper {

    private static final EntityApi entityApi = new DimdwarfEntityApi();

    private EntityHelper() {
    }

    public static boolean equals(@Nullable Object obj1, @Nullable Object obj2) {
        Object id1 = getReference(obj1);
        Object id2 = getReference(obj2);
        return Objects.safeEquals(id1, id2);
    }

    public static int hashCode(@Nullable Object obj) {
        Object id = getReference(obj);
        return id.hashCode();
    }

    @Nullable
    private static EntityReference<?> getReference(@Nullable Object obj) {
        if (entityApi.isTransparentReference(obj)) {
            return ((TransparentReference) obj).getEntityReference$TREF();
        } else if (entityApi.isEntity(obj)) {
            return ThreadContext.get(EntityReferenceFactory.class).createReference(obj);
        } else {
            return null;
        }
    }
}
