/*
 * Dimdwarf Application Server
 * Copyright (c) 2008, Esko Luontola
 * All Rights Reserved.
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

import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 20.8.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class RevisionMapSpec extends Specification<Object> {

    private RevisionMap<String, String> map = new RevisionMap<String, String>();

    public class RevisionsOfARevisionMap {

        public Object create() {
            return null;
        }

        public void startsFromFirstRevision() {
            specify(map.getCurrentRevision(), should.equal(0));
            specify(map.getOldestRevision(), should.equal(0));
        }

        public void increasesCurrentRevisionOnIncrement() {
            map.incrementRevision();
            specify(map.getCurrentRevision(), should.equal(1));
            specify(map.getOldestRevision(), should.equal(0));
        }

        public void increasesOldestRevisionOnPurge() {
            map.incrementRevision();
            map.purgeRevisionsOlderThan(1);
            specify(map.getCurrentRevision(), should.equal(1));
            specify(map.getOldestRevision(), should.equal(1));
        }

        public void oldestRevisionIsAtMostTheCurrentRevision() {
            map.incrementRevision();
            map.purgeRevisionsOlderThan(2);
            specify(map.getCurrentRevision(), should.equal(1));
            specify(map.getOldestRevision(), should.equal(1));
        }
    }

    public class AnEmptyRevisionMap {

        public Object create() {
            return null;
        }

        public void isEmpty() {
            specify(map.size(), should.equal(0));
        }

    }

    public class AfterPuttingAValueToARevisionMap {

        public Object create() {
            map.put("key", "value");
            return null;
        }

        public void itIsNotEmpty() {
            specify(map.size(), should.equal(1));
        }

        public void theValueExistsOnCurrentRevision() {
            long currentRevision = map.getCurrentRevision();
            specify(map.get("key", currentRevision), should.equal("value"));
        }
    }
}
