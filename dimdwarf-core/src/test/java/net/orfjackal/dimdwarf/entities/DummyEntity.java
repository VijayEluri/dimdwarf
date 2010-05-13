// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import net.orfjackal.dimdwarf.api.Entity;
import net.orfjackal.dimdwarf.api.internal.EntityObject;

import java.io.Serializable;

@Entity
public class DummyEntity implements DummyInterface, EntityObject, Serializable {
    private static final long serialVersionUID = 1L;

    public Object other;

    public DummyEntity() {
        this(null);
    }

    public DummyEntity(Object other) {
        this.other = other;
    }

    public Object getOther() {
        return other;
    }

    public void setOther(Object other) {
        this.other = other;
    }
}
