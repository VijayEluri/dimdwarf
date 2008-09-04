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

package net.orfjackal.dimdwarf.tref;

import net.orfjackal.dimdwarf.api.Entity;

/**
 * For transparent references to work correctly, all subclasses of {@link Entity} should
 * define their {@link #equals(Object)} and {@link #hashCode()} methods as follows:
 * <pre><code>
 * public boolean equals(Object obj) {
 *     return ManagedIdentity.equals(this, obj);
 * }
 * public int hashCode() {
 *     return ManagedIdentity.hashCode(this);
 * }
 * </code></pre>
 *
 * @author Esko Luontola
 * @since 1.2.2008
 */
public final class ManagedIdentity {

    private ManagedIdentity() {
    }

    public static boolean equals(Object obj1, Object obj2) {
        Object id1 = getManagedIdentity(obj1);
        Object id2 = getManagedIdentity(obj2);
        return safeEquals(id1, id2);
    }

    public static int hashCode(Object obj) {
        Object id = getManagedIdentity(obj);
        return id.hashCode();
    }

    private static Object getManagedIdentity(Object obj) {
        if (obj instanceof TransparentReference) {
            return ((TransparentReference) obj).getManagedReference();
        } else if (obj instanceof Entity) {
            return AppContext.getDataManager().createReference(obj);
        } else {
            return obj;
        }
    }

    private static boolean safeEquals(Object x, Object y) {
        return x == y || (x != null && x.equals(y));
    }
}
