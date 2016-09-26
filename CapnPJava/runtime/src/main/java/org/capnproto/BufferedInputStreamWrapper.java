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
		this.buf = ByteBuffer.allocate(8192);  						// allocates a new ByteBuffer with a limit of 8192 Bits
		this.buf.limit(0);											// sets the limit of the Buffer to 0, saying that there is nothing to read yet
	}

	public final int read(ByteBuffer dst) throws IOException {		// should be called readToByteBuffer or something like this
		int numBytes = dst.remaining();								// number of bytes left to read in the ByteBuffer 
		if (numBytes < this.buf.remaining()) {						// if the destination buffer has not enough room to take the bytes of the this.Buffer
			//# Serve from the current buffer.
			ByteBuffer slice = this.buf.slice();					//copies as many bytes from this.ByteBuffer to the destination ByteBuffer as available in the destination Buffer
			slice.limit(numBytes);
			dst.put(slice);
			this.buf.position(this.buf.position() + numBytes);		//sets new position to the Bytes not read yet
			return numBytes;										// returns number of bytes read to the destination
		} else {													// if the destination has enough space left to take all the bytes of thisBuffer
			//# Copy current available into destination.
			int fromFirstBuffer = this.buf.remaining();

			ByteBuffer slice = this.buf.slice();
			slice.limit(fromFirstBuffer);
			dst.put(slice);										// copies all the bytes of this.Buffer to the destination

			numBytes -= fromFirstBuffer;							//updates the number of remaining bytes in the destination
			if (numBytes <= this.buf.capacity()) {					// if the amount of remaining bytes in the destination is smaller than the capacity of this.Buffer 
				//# Read the next buffer-full.
				this.buf.clear();
				int n = readAtLeast(this.inner, this.buf, numBytes);	//read as many bytes from this.Channel to this.Buffer as there are bytes left in the destination Buffer

				this.buf.rewind();									// sets the pointer to the start of the buffer, because the content is all new
				ByteBuffer bigSlice = this.buf.slice();
				bigSlice.limit(numBytes);
				dst.put(bigSlice);										// puts as many bytes from this.Buffer to the destination buffer as there is space left

				this.buf.limit(n);									// sets the limit of this.buffer to the number of bytes read in total
				this.buf.position(numBytes);						// sets the position of this.buffer to the first byte, which has not been read to the destination buffer
				return fromFirstBuffer + numBytes;					// returns the number of bytes read into the destination  
			} else {												// if the destination buffer would have more space left than this.buffers capacity
				//# Forward large read to the underlying stream.
				this.buf.clear();									// clears this.buffer 
				this.buf.limit(0);
				return fromFirstBuffer + readAtLeast(this.inner, dst, numBytes);		// reads as many bytes from this.channel directly to the destination buffer as there is space left in the destination buffer
			}
		}
	}

	public final ByteBuffer getReadBuffer() throws IOException {
		if (this.buf.remaining() == 0) {							// if all the bytes of this.buffer are already read
			this.buf.clear();										// clears the buffer
			int n = readAtLeast(this.inner, this.buf, 1);			// reads at least one byte from this.channel to the fresh buffer
			this.buf.rewind();										// sets the position the zero
			this.buf.limit(n);										// and the limit the number of read bytes
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
			int res = reader.read(buf);				//the number of bytes read from the channel to the buffer 
			if (res < 0) { throw new Error("premature EOF"); }
			numRead += res;							// updates the number of read bytes, makes sure that at least minBytes bytes are read to the buffer
		}
		return numRead;								// returns the number of bytes read in total
	}
}
