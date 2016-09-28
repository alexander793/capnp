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
import java.nio.channels.WritableByteChannel;


public final class BufferedOutputStreamWrapper implements BufferedOutputStream {

	private final WritableByteChannel inner;
	private final ByteBuffer buf;

	public BufferedOutputStreamWrapper(WritableByteChannel w) {
		this.inner = w;
		this.buf = ByteBuffer.allocate(8192);
	}

	public final int write(ByteBuffer src) throws IOException {
		int available = this.buf.remaining();						// number of free bytes in this.buffer
		int size = src.remaining();									// number of bytes to be written from the src buffer to this.buffer
		if (size <= available) {									// if there is enough space left in this.buffer for the bytes from the src buffer
			this.buf.put(src);										// writes the bytes from the src to this.buffer
		} else if (size <= this.buf.capacity()) {								//if this.buffer will be full with the additional bytes from the src buffer and must be written to the channel
			//# Too much for this buffer, but not a full buffer's worth,		// and the src buffer would fill a whole new buffer
			//# so we'll go ahead and copy.

			WireHelpers.writeSlice(src, available, this.buf);		// fills the free space of this.buffer with bytes from the src buffer

			this.buf.rewind();										// sets the position of this buffer to its beginning
			while (this.buf.hasRemaining()) {						// as long as there are bytes left in this.buffer to write on the channel
				this.inner.write(this.buf);							// writes this buffer to the channel
			}
			this.buf.rewind();										// all bytes are written, so the position is set back to the beginning of the buffer

			src.position(src.position() + available);				// updates the pointer of the src buffer to the next byte that has to be written
			this.buf.put(src);										// fills the fresh buffer with the rest of the src buffer
			// won't take the whole space of the buffer, because of the IF-statement
		} else {													// if the src buffer is bigger than this buffers capacity, it wouldn't make sense to copy all the data first to a buffer and then rite it to the channel
			// so we write all the bytes from the src buffer directly to the channel
			//# Writing so much data that we might as well write
			//# directly to avoid a copy.

			int pos = this.buf.position();							//saves the position of the last filled byte of the buffer
			this.buf.rewind();										// sets the position of the buffer to its beginning
			ByteBuffer slice = this.buf.slice();
			slice.limit(pos);										// creates a new buffer as big as the actual content of this.buffer -> empty space of this.buffer is cut off
			while (slice.hasRemaining()) {
				this.inner.write(slice);							// writes the filled bytes of this.buffer to the channel
			}
			while (src.hasRemaining()) {
				this.inner.write(src);								// writes the bytes from the src buffer to the chanel
			}
		}
		return size;												//returns the number of bytes of the source channel -> maybe it should be updated during the write process ?
	}

	public final ByteBuffer getWriteBuffer() {
		return this.buf;
	}

	public final void close() throws IOException {
		this.inner.close();
	}

	public final boolean isOpen() {
		return this.inner.isOpen();
	}

	public final void flush() throws IOException {					// writes all the filled bytes of this.buffer to the channel and clears the buffer
		int pos = this.buf.position();
		this.buf.rewind();
		this.buf.limit(pos);
		this.inner.write(this.buf);
		this.buf.clear();
	}
}
