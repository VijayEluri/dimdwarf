/*
 * This file is part of Dimdwarf Application Server <http://dimdwarf.sourceforge.net/>
 *
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
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

import net.orfjackal.dimdwarf.serial.*;

import javax.annotation.concurrent.Immutable;

/**
 * @author Esko Luontola
 * @since 4.9.2008
 */
@Immutable
public class CheckInnerClassSerialized extends SerializationAdapter {

    @Override
    public void beforeSerialize(Object rootObject, Object obj, MetadataBuilder meta) {
        // Serializing anonymous and local classes is dangerous, because their class names are generated
        // automatically ($1, $2, $1Local, $2Local etc.), which means that the next time that such an
        // object is deserialized from database, the class name might have changed because of
        // a change to the enclosing class, and the system might end up in an unspecified state.
        // Also "Java Object Serialization Specification 6.0" strongly discourages serialization of
        // inner classes (http://java.sun.com/javase/6/docs/platform/serialization/spec/serial-arch.html#7182).
        // It is safer to serialize only top-level classes and member classes.
        Class<?> cls = obj.getClass();
        if (cls.isAnonymousClass()) {
            throw new IllegalArgumentException("Tried to serialize an anonymous class: " + cls.getName());
        }
        if (cls.isLocalClass()) {
            throw new IllegalArgumentException("Tried to serialize a local class: " + cls.getName());
        }
    }
}
