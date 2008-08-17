/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
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

package net.orfjackal.dimdwarf.tx;

import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 15.8.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class TransactionSpec extends Specification<Object> {

    private Transaction tx;
    private TransactionParticipant participant1;
    private TransactionParticipant participant2;

    public void create() throws Exception {
        tx = new Transaction();
        participant1 = mock(TransactionParticipant.class, "participant1");
        participant2 = mock(TransactionParticipant.class, "participant2");
    }

    private Expectations isNotifiedOnJoin(final TransactionParticipant participant) {
        return new Expectations() {{
            one(participant).joinedTransaction(tx);
        }};
    }


    public class WhenTransactionBegins {

        public Object create() {
            return null;
        }

        public void itIsActive() {
            specify(tx.getStatus(), should.equal(Transaction.Status.ACTIVE));
            tx.mustBeActive();
        }

        public void itHasNoParticipants() {
            specify(tx.getParticipants(), should.equal(0));
        }
    }

    public class WhenParticipantJoinsTransaction {

        public Object create() {
            checking(isNotifiedOnJoin(participant1));
            tx.join(participant1);
            return null;
        }

        public void itHasParticipants() {
            specify(tx.getParticipants(), should.equal(1));
        }

        public void otherParticipantsMayJoinTheSameTransaction() {
            checking(isNotifiedOnJoin(participant2));
            tx.join(participant2);
            specify(tx.getParticipants(), should.equal(2));
        }

        public void theSameParticipantCanNotJoinTwise() {
            tx.join(participant1);
            specify(tx.getParticipants(), should.equal(1));
        }
    }
}
