// Copyright © 2008-2011 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.api;

import java.nio.ByteBuffer;

public interface Session {

    void send(ByteBuffer message);
}
