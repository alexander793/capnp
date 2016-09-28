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
import java.nio.channels.ReadableByteChannel;


public final class BufferedInputStreamWrapper implements BufferedInputStream {

	private final ReadableByteChannel inner;
	private final ByteBuffer buf;

	public BufferedInputStreamWrapper(ReadableByteChannel chan) {
		this.inner = chan;
		// allocates a new ByteBuffer with a limit of 8192 Bits
		this.buf = ByteBuffer.allocate(8192);
		// sets the limit of the Buffer to 0, saying that there is nothing to read yet
		this.buf.limit(0);
	}

	public final int read(ByteBuffer dst) throws IOException {
		// reads bytes of an input stream to a buffer
		// number of bytes left to read in the ByteBuffer 
		int numBytes = dst.remaining();

		/*
		 * If the destination buffer has not enough room to take the bytes of the this.Buffer,
		 * copy as many bytes from this.ByteBuffer to the destination ByteBuffer as available in the
		 * destination Buffer.
		 * Then set the new position to the Bytes not read yet and
		 * return number of bytes read to the destination.
		 */
		if (numBytes < this.buf.remaining()) {
			WireHelpers.writeSlice(this.buf, numBytes, dst);
			this.buf.position(this.buf.position() + numBytes);
			return numBytes;
		} else {
			/*
			 * If the destination has enough space left to take all the bytes of thisBuffer,
			 * copy all the bytes of this.Buffer to the destination and
			 * update the number of remaining bytes in the destination.
			 */
			int fromFirstBuffer = this.buf.remaining();
			WireHelpers.writeSlice(this.buf, fromFirstBuffer, dst);
			numBytes -= fromFirstBuffer;

			if (numBytes <= this.buf.capacity()) {
				/*
				 * If the amount of remaining bytes in the destination is smaller than the capacity
				 * of this.Buffer,
				 * read as many bytes from this.Channel to this.Buffer as there are bytes left in
				 * the destination Buffer.
				 * Then set the pointer to the start of the buffer, because the content is all new.
				 * Put as many bytes from this.Buffer to the destination buffer as there is space
				 * left.
				 * Set the limit of this.buffer to the number of bytes read in total.
				 * Update the position of this.buffer to the first byte, which has not been read
				 * to the destination buffer and
				 * return the number of bytes read into the destination.
				 */
				this.buf.clear();
				int n = readAtLeast(this.inner, this.buf, numBytes);
				this.buf.rewind();
				WireHelpers.writeSlice(this.buf, numBytes, dst);
				this.buf.limit(n);
				this.buf.position(numBytes);
				return fromFirstBuffer + numBytes;
			} else {
				/*
				 * If the destination buffer would have more space left than this.buffers capacity,
				 * clear this.buffer and
				 * read as many bytes from this.channel directly to the destination buffer as there
				 * is space left in the destination buffer
				 */
				this.buf.clear();
				this.buf.limit(0);
				return fromFirstBuffer + readAtLeast(this.inner, dst, numBytes);
			}
		}
	}

	public final ByteBuffer getReadBuffer() throws IOException {
		/*
		 * If all the bytes of this.buffer are already read,
		 * clear the buffer and
		 * read at least one byte from this.channel to the fresh buffer.
		 * Then set the position to zero and
		 * the limit to the number of read bytes.
		 */
		if (this.buf.remaining() == 0) {
			this.buf.clear();
			int n = readAtLeast(this.inner, this.buf, 1);
			this.buf.rewind();
			this.buf.limit(n);
		}
		return this.buf;
	}

	public final void close() throws IOException {
		this.inner.close();
	}

	public final boolean isOpen() {
		return this.inner.isOpen();
	}

	public static int readAtLeast(ReadableByteChannel reader, ByteBuffer buf, int minBytes) throws IOException {
		int numRead = 0;
		while (numRead < minBytes) {
			//the number of bytes read from the channel to the buffer 
			int res = reader.read(buf);
			if (res < 0) { throw new Error("premature EOF"); }
			// updates the number of read bytes, to make sure that at least minBytes bytes are read to the buffer
			numRead += res;
		}
		// returns the number of bytes read in total
		return numRead;
	}
}
