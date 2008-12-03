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

package net.orfjackal.dimdwarf.gc.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.internal.EntityReference;
import net.orfjackal.dimdwarf.db.Blob;
import net.orfjackal.dimdwarf.entities.dao.*;
import net.orfjackal.dimdwarf.gc.Graph;
import net.orfjackal.dimdwarf.serial.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 30.11.2008
 */
public class EntityGraph implements Graph<BigInteger> {

    private final EntityDao entities;
    private final BindingDao bindings;

    @Inject
    public EntityGraph(EntityDao entities, BindingDao bindings) {
        this.entities = entities;
        this.bindings = bindings;
    }

    public Iterable<BigInteger> getAllNodes() {
        ArrayList<BigInteger> nodes = new ArrayList<BigInteger>();
        for (BigInteger id = entities.firstKey(); id != null; id = entities.nextKeyAfter(id)) {
            nodes.add(id);
        }
        // TODO: return a dynamic iterator instead of building a complete list inside this method
        return nodes;
    }

    public Iterable<BigInteger> getRootNodes() {
        ArrayList<BigInteger> nodes = new ArrayList<BigInteger>();
        for (String binding = bindings.firstKey(); binding != null; binding = bindings.nextKeyAfter(binding)) {
            BigInteger id = bindings.read(binding);
            if (id != null) {
                nodes.add(id);
            }
        }
        // TODO: return a dynamic iterator instead of building a complete list inside this method
        return nodes;
    }

    public Iterable<BigInteger> getConnectedNodesOf(BigInteger node) {
        return getReferencedEntityIds(entities.read(node));
    }

    private static Iterable<BigInteger> getReferencedEntityIds(Blob entity) {
        EntityReferenceListener listener = new EntityReferenceListener();
        new ObjectSerializerImpl(
                new SerializationListener[]{listener},
                new SerializationReplacer[0]
        ).deserialize(entity);
        return listener.getReferences();
    }

    public void removeNode(BigInteger node) {
        entities.delete(node);
    }

    public long getStatus(BigInteger node) {
        Blob status = entities.readMetadata(node, "gc-status");
        if (status.length() == 0) {
            return 0L;
        }
        return status.getByteBuffer().asLongBuffer().get();
    }

    public void setStatus(BigInteger node, long status) {
        ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(8).putLong(status).flip();
        entities.updateMetadata(node, "gc-status", Blob.fromByteBuffer(buf));
    }

    // TODO: give direct access to metadata, allow any keys and bytes

    private static class EntityReferenceListener extends SerializationAdapter {

        private final List<BigInteger> references = new ArrayList<BigInteger>();

        public void afterDeserialize(Object obj) {
            if (obj instanceof EntityReference) {
                EntityReference<?> ref = (EntityReference<?>) obj;
                references.add(ref.getEntityId());
            }
        }

        public List<BigInteger> getReferences() {
            return Collections.unmodifiableList(references);
        }
    }
}
