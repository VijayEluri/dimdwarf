// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.gc.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.entities.*;
import net.orfjackal.dimdwarf.entities.dao.BindingDao;
import net.orfjackal.dimdwarf.gc.MutatorListener;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 12.9.2008
 */
@Immutable
public class GcAwareBindingRepository implements BindingRepository {

    private final BindingDao bindings;
    private final ConvertEntityToEntityId entityToId;
    private final MutatorListener<BigInteger> listener;

    @Inject
    public GcAwareBindingRepository(BindingDao bindings,
                                    ConvertEntityToEntityId entityToId,
                                    MutatorListener<BigInteger> listener) {
        this.bindings = bindings;
        this.entityToId = entityToId;
        this.listener = listener;
    }

    public boolean exists(String binding) {
        return bindings.exists(binding);
    }

    public Object read(String binding) {
        BigInteger oldTarget = bindings.read(binding);
        return entityToId.back(oldTarget);
    }

    public void update(String binding, Object entity) {
        BigInteger oldTarget = bindings.read(binding);
        BigInteger newTarget = entityToId.forth(entity);
        bindings.update(binding, newTarget);
        fireBindingUpdated(oldTarget, newTarget);
    }

    private void fireBindingUpdated(@Nullable BigInteger oldTarget, @Nullable BigInteger newTarget) {
        if (oldTarget != null) {
            listener.onReferenceRemoved(null, oldTarget);
        }
        if (newTarget != null) {
            listener.onReferenceCreated(null, newTarget);
        }
    }

    public void delete(String binding) {
        BigInteger oldTarget = bindings.read(binding);
        bindings.delete(binding);
        fireBindingDeleted(oldTarget);
    }

    private void fireBindingDeleted(BigInteger oldTarget) {
        fireBindingUpdated(oldTarget, null);
    }

    public String firstKey() {
        return bindings.firstKey();
    }

    public String nextKeyAfter(String currentKey) {
        return bindings.nextKeyAfter(currentKey);
    }
}
