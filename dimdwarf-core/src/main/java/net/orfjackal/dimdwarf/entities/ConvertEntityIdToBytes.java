// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.db.*;

import javax.annotation.Nullable;

/**
 * @author Esko Luontola
 * @since 13.8.2009
 */
public class ConvertEntityIdToBytes implements Converter<ObjectIdMigration, Blob> {

    private final ConvertEntityIdToBigInteger keys1 = new ConvertEntityIdToBigInteger();
    private final ConvertBigIntegerToBytes keys2 = new ConvertBigIntegerToBytes();

    @Nullable
    public ObjectIdMigration back(@Nullable Blob value) {
        return keys1.back(keys2.back(value));
    }

    @Nullable
    public Blob forth(@Nullable ObjectIdMigration value) {
        return keys2.forth(keys1.forth(value));
    }
}
