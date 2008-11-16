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

package net.orfjackal.dimdwarf.db.inmemory;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class is NOT thread-safe.
 *
 * @author Esko Luontola
 * @since 12.9.2008
 */
public class RevisionMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {

    private final RevisionMap<K, V> map;
    private final long revision;
    private Map.Entry<K, V> fetchedNext;
    private K nextKey;

    public RevisionMapIterator(RevisionMap<K, V> map, long revision) {
        this.map = map;
        this.revision = revision;
        nextKey = map.firstKey();
    }

    public boolean hasNext() {
        fetchNext();
        return fetchedNext != null;
    }

    public Map.Entry<K, V> next() {
        fetchNext();
        return returnNext();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void fetchNext() {
        while (fetchedNext == null && nextKey != null) {
            K key = nextKey;
            V value = map.get(key, revision);
            nextKey = map.nextKeyAfter(key);
            if (value != null) {
                fetchedNext = new MyEntry<K, V>(key, value);
            }
        }
    }

    private Map.Entry<K, V> returnNext() {
        Map.Entry<K, V> next = fetchedNext;
        if (next == null) {
            throw new NoSuchElementException();
        }
        fetchedNext = null;
        return next;
    }

    /**
     * This class is immutable.
     */
    private static class MyEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;

        public MyEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }
}
