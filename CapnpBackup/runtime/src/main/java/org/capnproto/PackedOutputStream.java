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


public final class PackedOutputStream implements WritableByteChannel {

	final BufferedOutputStream inner;

	public PackedOutputStream(BufferedOutputStream output) {
		this.inner = output;
	}

	public int write(ByteBuffer inBuf) throws IOException {						// packs bytes from a byte buffer and writes it to an output stream
		int length = inBuf.remaining();											// gets the number of bytes remaining in the buffer to write from
		ByteBuffer out = this.inner.getWriteBuffer();							// creates a new buffer for output

		ByteBuffer slowBuffer = ByteBuffer.allocate(20);						// creates a new buffer with at most 20 bytes

		int inPtr = inBuf.position();											// gets the number of the next byte to be written
		int inEnd = inPtr + length;												// gets the number of the last byte to be written
		while (inPtr < inEnd) {													// while there are still some bytes remaining in the input buffer
			if (out.remaining() < 10) {											// if there are less than 10 bytes left in the output buffer
				//# Oops, we're out of space. We need at least 10
				//# bytes for the fast path, since we don't
				//# bounds-check on every byte.

				if (out == slowBuffer) {
					int oldLimit = out.limit();
					out.limit(out.position());
					out.rewind();
					this.inner.write(out);
					out.limit(oldLimit);
				}

				out = slowBuffer;
				out.rewind();
			}

			int tagPos = out.position();										// sets the position of the tag word used in packed format
			out.position(tagPos + 1);											// sets the position of the output buffer behind the tag word

			byte curByte;

			curByte = inBuf.get(inPtr);											// sets the current, to be packed byte, to the first byte of the input buffer
			byte bit0 = (curByte != 0) ? (byte) 1 : (byte) 0;					// sets the first bit of the tag zero or one, depending on if the current input byte is zero or not
			out.put(curByte);													// writes the current byte of the input buffer to the output
			out.position(out.position() + bit0 - 1);							// if the written byte was a zero, then the position of the output buffer is decremented 
			inPtr += 1;															// increments the imaginal pointer of the input buffer 

			curByte = inBuf.get(inPtr);											// 
			byte bit1 = (curByte != 0) ? (byte) 1 : (byte) 0;					// checks, if the next byte of the input buffer is zero or not
			out.put(curByte);
			out.position(out.position() + bit1 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit2 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit2 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit3 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit3 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit4 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit4 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit5 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit5 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit6 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit6 - 1);
			inPtr += 1;

			curByte = inBuf.get(inPtr);
			byte bit7 = (curByte != 0) ? (byte) 1 : (byte) 0;
			out.put(curByte);
			out.position(out.position() + bit7 - 1);
			inPtr += 1;

			byte tag = (byte) ((bit0 << 0) | (bit1 << 1) | (bit2 << 2) | (bit3 << 3) | (bit4 << 4) | (bit5 << 5) | (bit6 << 6) | (bit7 << 7));			// creates the tag from the calculated bits

			out.put(tagPos, tag); 																				//puts the tag at the reserved position

			if (tag == 0) {																						// if the tag is the special 0x00 tag
				//# An all-zero word is followed by a count of
				//# consecutive zero words (not including the first
				//# one).
				int runStart = inPtr;
				int limit = inEnd;
				if (limit - inPtr > 255 * 8) {																	// if there are more than 8 bytes to write in the input buffer, 
					limit = inPtr + 255 * 8;																	// the write limit is set to 8 byte; because of the special packed encoding, a maximum of 8 zero words can follow the zero tag
				}
				while (inPtr < limit && inBuf.getLong(inPtr) == 0) {											// jumps over the next zero words, with a maximum of 8 zero words
					inPtr += 8;
				}
				out.put((byte) ((inPtr - runStart) / 8));														// calculates the number of zero words, that were jumped over and puts it behind the zero tag 

			} else if (tag == (byte) 0xff) {																	// if the tag is the special 0x00 tag
				//# An all-nonzero word is followed by a count of
				//# consecutive uncompressed words, followed by the
				//# uncompressed words themselves.

				//# Count the number of consecutive words in the input
				//# which have no more than a single zero-byte. We look
				//# for at least two zeros because that's the point
				//# where our compression scheme becomes a net win.

				int runStart = inPtr;
				int limit = inEnd;
				if (limit - inPtr > 255 * 8) {																	// if the number of to be written bytes of the input is bigger than 8 bytes 
					limit = inPtr + 255 * 8;																	// the limit is set to 8 bytes, which could be copied directly to the output
				}

				while (inPtr < limit) {																			// iterates through the next 8 bytes and checks, if there are at least 2 zero bytes
					byte c = 0;
					for (int ii = 0; ii < 8; ++ii) {
						c += (inBuf.get(inPtr) == 0 ? 1 : 0);
						inPtr += 1;
					}
					if (c >= 2) {																				// if there are at least two zero bytes in this word, we leave the loop and compress these two
						//# Un-read the word with multiple zeros, since
						//# we'll want to compress that one.
						inPtr -= 8;
						break;
					}
				}

				int count = inPtr - runStart;																	// calculates the number of words which should not be compressed
				out.put((byte) (count / 8));																	// puts that number behind the 0xff tag

				if (count <= out.remaining()) {																	// if there is as least as much space left on the output buffer as the number of uncompressed words
					//# There's enough space to memcpy.
					inBuf.position(runStart);
					ByteBuffer slice = inBuf.slice();
					slice.limit(count);
					out.put(slice);																				// copies the calculated number of words from the input buffer directly to the output buffer
				} else {																						// if there is not enough space left in the output buffer to take all the uncompressed words
					//# Input overruns the output buffer. We'll give it
					//# to the output stream in one chunk and let it
					//# decide what to do.

					if (out == slowBuffer) {																	// no idea
						int oldLimit = out.limit();
						out.limit(out.position());
						out.rewind();
						this.inner.write(out);
						out.limit(oldLimit);
					}

					inBuf.position(runStart);																	// writes the uncompressed words directly to the output stream
					ByteBuffer slice = inBuf.slice();
					slice.limit(count);
					while (slice.hasRemaining()) {
						this.inner.write(slice);
					}

					out = this.inner.getWriteBuffer();															// fills the output buffer with new bytes
				}
			}
		}

		if (out == slowBuffer) {																				// no idea
			out.limit(out.position());
			out.rewind();
			this.inner.write(out);
		}

		inBuf.position(inPtr);																					// sets the position of the input buffer to the next to be written byte
		return length;																							// returns the number of written bytes
	}

	public void close() throws IOException {
		this.inner.close();
	}

	public boolean isOpen() {
		return this.inner.isOpen();
	}
}
