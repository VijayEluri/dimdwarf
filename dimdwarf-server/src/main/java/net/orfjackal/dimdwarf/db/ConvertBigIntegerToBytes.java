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

import java.math.BigInteger;

/**
 * This class is immutable.
 *
 * @author Esko Luontola
 * @since 12.9.2008
 */
public class ConvertBigIntegerToBytes implements Converter<BigInteger, Blob> {

    public BigInteger back(Blob value) {
        if (value == null) {
            return null;
        }
        return new BigInteger(1, unpack(value.getByteArray()));
    }

    public Blob forth(BigInteger value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Negative values are not allowed: " + value);
        }
        return Blob.fromBytes(pack(value.toByteArray()));
    }

    private static byte[] unpack(byte[] packed) {
        int significantBytes = packed[0];
        byte[] bytes = new byte[significantBytes];
        System.arraycopy(packed, 1, bytes, 0, significantBytes);
        return bytes;
    }

    private static byte[] pack(byte[] bytes) {
        int leadingNullBytes = getLeadingNullBytes(bytes);
        int significantBytes = bytes.length - leadingNullBytes;
        byte[] packed = new byte[significantBytes + 1];
        packed[0] = (byte) significantBytes;
        System.arraycopy(bytes, leadingNullBytes, packed, 1, significantBytes);
        return packed;
    }

    private static int getLeadingNullBytes(byte[] bytes) {
        int leadingNullBytes = 0;
        for (int i = 0; i < bytes.length && bytes[i] == 0x00; i++) {
            leadingNullBytes++;
        }
        return leadingNullBytes;
    }
}
