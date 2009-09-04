// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.api;

/**
 * @author Esko Luontola
 * @since 2.11.2008
 */
public interface EntityInfo {

    // TODO: replace BigInteger with a class EntityId to avoid primitive obsession 

    /**
     * Returns a unique ID for the specified entity. The parameter may be an entity
     * or a transparent reference proxy of an entity.
     *
     * @throws IllegalArgumentException if the object is not an entity.
     */
    net.orfjackal.dimdwarf.api.internal.ObjectIdMigration getEntityId(Object entity);
}
