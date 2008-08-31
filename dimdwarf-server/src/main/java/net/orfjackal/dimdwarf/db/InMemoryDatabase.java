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

package net.orfjackal.dimdwarf.db;

import static net.orfjackal.dimdwarf.db.Blob.EMPTY_BLOB;
import net.orfjackal.dimdwarf.tx.Transaction;
import net.orfjackal.dimdwarf.tx.TransactionParticipant;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Esko Luontola
 * @since 18.8.2008
 */
public class InMemoryDatabase {

    private final RevisionMap<Blob, Blob> revisions = new RevisionMap<Blob, Blob>();
    private final ConcurrentMap<Blob, Transaction> lockedForCommit = new ConcurrentHashMap<Blob, Transaction>();
    private final ConcurrentMap<Transaction, Long> openConnections = new ConcurrentHashMap<Transaction, Long>();
    private volatile long committedRevision = revisions.getCurrentRevision();

    public DatabaseConnection openConnection(Transaction tx) {
        if (openConnections.containsKey(tx)) {
            throw new IllegalArgumentException("Connection already open in this transaction");
        }
        DatabaseConnection con = new TransactionalDatabaseConnection(tx, committedRevision);
        openConnections.put(tx, committedRevision);
        return con;
    }

    private void closeConnection(Transaction tx) {
        openConnections.remove(tx);
        revisions.purgeRevisionsOlderThan(getOldestUncommittedRevision());
    }

    protected long getOldestUncommittedRevision() {
        long oldest = committedRevision;
        for (long revision : openConnections.values()) {
            oldest = Math.min(oldest, revision);
        }
        return oldest;
    }

    @TestOnly
    protected int getOpenConnections() {
        return openConnections.size();
    }

    @TestOnly
    protected long getCurrentRevision() {
        return committedRevision;
    }

    @TestOnly
    protected long getOldestStoredRevision() {
        return revisions.getOldestRevision();
    }

    private void prepareTransaction(Transaction tx, Map<Blob, Blob> modified, long revision) throws Exception {
        synchronized (lockedForCommit) {
            for (Map.Entry<Blob, Blob> e : modified.entrySet()) {
                long lastWrite = revisions.getLatestRevisionForKey(e.getKey());
                if (lastWrite > revision) {
                    throw new OptimisticLockException("Key " + e.getKey() + " already modified in revision " + lastWrite);
                }
            }
            lockKeysForCommit(tx, modified.keySet());
        }
    }

    private void commitTransaction(Transaction tx, Map<Blob, Blob> modified) {
        synchronized (lockedForCommit) {
            try {
                revisions.incrementRevision();
                for (Map.Entry<Blob, Blob> e : modified.entrySet()) {
                    revisions.put(e.getKey(), e.getValue());
                }
            } finally {
                committedRevision = revisions.getCurrentRevision();
                unlockKeysForCommit(tx, modified.keySet());
            }
        }
    }

    private void rollbackTransaction(Transaction tx, Map<Blob, Blob> modified) {
        synchronized (lockedForCommit) {
            unlockKeysForCommit(tx, modified.keySet());
        }
    }

    private void lockKeysForCommit(Transaction tx, Set<Blob> keys) {
        for (Blob key : keys) {
            Transaction alreadyLockedBy = lockedForCommit.putIfAbsent(key, tx);
            assert alreadyLockedBy == null : "key = " + key;
        }
    }

    private void unlockKeysForCommit(Transaction tx, Set<Blob> keys) {
        for (Blob key : keys) {
            if (lockedForCommit.containsKey(key)) {
                boolean wasLockedByMe = lockedForCommit.remove(key, tx);
                assert wasLockedByMe : "key = " + key;
            }
        }
    }


    private class TransactionalDatabaseConnection implements DatabaseConnection, TransactionParticipant {

        private final Map<Blob, Blob> updates = new ConcurrentHashMap<Blob, Blob>();
        private final long visibleRevision;
        private final Transaction tx;

        public TransactionalDatabaseConnection(Transaction tx, long visibleRevision) {
            this.tx = tx;
            this.visibleRevision = visibleRevision;
            tx.join(this);
        }

        public void joinedTransaction(Transaction tx) {
            assert this.tx == tx;
        }

        public void prepare(Transaction tx) throws Throwable {
            prepareTransaction(tx, updates, visibleRevision);
        }

        public void commit(Transaction tx) {
            try {
                commitTransaction(tx, updates);
            } finally {
                closeConnection(tx);
            }
        }

        public void rollback(Transaction tx) {
            try {
                rollbackTransaction(tx, updates);
            } finally {
                closeConnection(tx);
            }
        }

        public Blob read(Blob key) {
            tx.mustBeActive();
            Blob blob = updates.get(key);
            if (blob == null) {
                blob = revisions.get(key, visibleRevision);
            }
            if (blob == null) {
                blob = EMPTY_BLOB;
            }
            return blob;
        }

        public void update(Blob key, Blob value) {
            tx.mustBeActive();
            updates.put(key, value);
        }

        public void delete(Blob key) {
            tx.mustBeActive();
            updates.put(key, EMPTY_BLOB);
        }
    }
}
