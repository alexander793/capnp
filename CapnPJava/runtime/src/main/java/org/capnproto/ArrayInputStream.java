// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;


public final class ArrayInputStream implements BufferedInputStream {

	public final ByteBuffer buf;

	public ArrayInputStream(ByteBuffer buf) {
		// creates a read-only copy of the ByteBuffer
		this.buf = buf.asReadOnlyBuffer();
	}

	public final int read(ByteBuffer dst) throws IOException {
		//number of bytes remaining in the destination buffer
		int size = dst.remaining();

		// fills the destination buffer with bytes from this.buffer
		WireHelpers.writeSlice(this.buf, size, dst);

		//updates the position of the not read bytes
		this.buf.position(this.buf.position() + size);
		// returns number of bytes read to the destination buffer
		return size;
	}

	public final ByteBuffer getReadBuffer() {
		return this.buf;
	}

	public final void close() throws IOException {
		return;
	}

	public final boolean isOpen() {
		return true;
	}
}
