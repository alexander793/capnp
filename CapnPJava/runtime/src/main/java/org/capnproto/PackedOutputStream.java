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

	public int write(ByteBuffer inBuf) throws IOException {
		// packs bytes from a byte buffer and writes it to an output stream
		// gets the number of bytes remaining in the buffer to write from
		int length = inBuf.remaining();
		// creates a new buffer for output
		ByteBuffer out = this.inner.getWriteBuffer();

		// creates a new buffer with at most 20 bytes
		ByteBuffer slowBuffer = ByteBuffer.allocate(20);

		// gets the number of the next byte to be written
		int inPtr = inBuf.position();
		// gets the number of the last byte to be written
		int inEnd = inPtr + length;

		while (inPtr < inEnd) {
			// while there are still some bytes remaining in the input buffer

			if (out.remaining() < 10) {
				// if there are less than 10 bytes left in the output buffer
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

			// sets the position of the tag word used in packed format
			int tagPos = out.position();
			// sets the position of the output buffer behind the tag word
			out.position(tagPos + 1);

			// the final tag
			byte tag = 0;
			// a bit of the tag
			byte signalBit = 0;
			// the current, to be zero-checked byte
			byte curByte;

			for (int ii = 0; ii < Constants.BITS_PER_BYTE; ++ii) {
				/*
				 * This loop iterates through the next 8 bytes.
				 * First it reads the next byte from the input buffer and
				 * resets the signal bit for the current byte to zero.
				 * If the current byte is nonzero, it sets the signalBit to 1 and
				 * writes the current byte to the output buffer.
				 * If the current byte is zero, nothing happens.
				 * Finally the input pointer is incremented to jump to the next byte and
				 * the tag is updated with the shifted signalBit.
				 */
				curByte = inBuf.get(inPtr);
				signalBit = 0;
				if (curByte != 0) {
					signalBit = 1;
					out.put(curByte);
				}
				inPtr++;
				tag = (byte) (tag | (signalBit << ii));
			}

			//puts the tag at the reserved position
			out.put(tagPos, tag);

			if (tag == 0) {
				// if the tag is the special 0x00 tag
				//# An all-zero word is followed by a count of
				//# consecutive zero words (not including the first
				//# one).
				int runStart = inPtr;
				int limit = inEnd;
				/*
				 * If there are more than 8 bytes to write from the input buffer,
				 * the write limit is set to 8 bytes; because of the special packed encoding,
				 * a maximum of 8 zero words can follow the zero tag.
				 */
				if (limit - inPtr > 255 * Constants.BYTES_PER_WORD) {
					limit = inPtr + 255 * Constants.BYTES_PER_WORD;
				}

				while (inPtr < limit && inBuf.getLong(inPtr) == 0) {
					// jumps over the next zero words, with a maximum of 8 zero words
					inPtr += Constants.BYTES_PER_WORD;
				}

				// calculates the number of zero words, that were jumped over and puts it behind the zero tag 
				out.put((byte) ((inPtr - runStart) / Constants.BYTES_PER_WORD));

			} else if (tag == (byte) 0xff) {
				// if the tag is the special 0xff tag
				//# An all-nonzero word is followed by a count of
				//# consecutive uncompressed words, followed by the
				//# uncompressed words themselves.

				//# Count the number of consecutive words in the input
				//# which have no more than a single zero-byte. We look
				//# for at least two zeros because that's the point
				//# where our compression scheme becomes a net win.

				int runStart = inPtr;
				int limit = inEnd;

				/*
				 * If there are more than 8 bytes to write from the input buffer,
				 * the write limit is set to 8 bytes, which could be copied directly to the output
				 */
				if (limit - inPtr > 255 * Constants.BYTES_PER_WORD) {
					limit = inPtr + 255 * Constants.BYTES_PER_WORD;
				}

				while (inPtr < limit) {
					// iterates through the words from the beginning of the buffer to its limit
					byte c = 0;
					for (int ii = 0; ii < Constants.BYTES_PER_WORD; ++ii) {
						// iterates through the word and checks, if there are at least 2 zero bytes					
						c += (inBuf.get(inPtr) == 0 ? 1 : 0);
						inPtr++;
					}
					if (c >= 2) {
						// if there are at least two zero bytes in this word, we leave the loop and 
						// un-read the word, since we'll want to compress that one.
						inPtr -= Constants.BYTES_PER_WORD;
						break;
					}
				}

				// calculates the number of words which should not be compressed
				int count = inPtr - runStart;
				// puts that number behind the 0xff tag
				out.put((byte) (count / Constants.BYTES_PER_WORD));

				if (count <= out.remaining()) {

					/*
					 * # There's enough space to memcpy.
					 * If there is at least as much space left on the output buffer as the number of
					 * uncompressed words,
					 * it copies the calculated number of words from the input buffer directly to
					 * the output buffer.
					 */
					inBuf.position(runStart);
					WireHelpers.writeSlice(inBuf, count, out);
				} else {
					// if there is not enough space left in the output buffer to take all the uncompressed words
					//# Input overruns the output buffer. We'll give it
					//# to the output stream in one chunk and let it
					//# decide what to do.

					if (out == slowBuffer) {// no idea																	
						// save the current limit of the buffer
						int oldLimit = out.limit();
						out.limit(out.position());
						out.rewind();
						// writes the bytes that are already in the buffer to the output stream
						this.inner.write(out);
						// after that, we set the limit of the buffer back to the old one
						out.limit(oldLimit);
					}

					// because there is no more space left in the output buffer
					inBuf.position(runStart);
					ByteBuffer slice = inBuf.slice();
					slice.limit(count);
					while (slice.hasRemaining()) {
						// we write the uncompressed words directly to the output stream
						this.inner.write(slice);
					}

					// fill the output buffer with new bytes
					out = this.inner.getWriteBuffer();
				}
			}
		}

		if (out == slowBuffer) {// no idea		
			// if there are finally no more bytes left on the input stream 
			// write the rest of the buffered bytes to the output stream
			out.limit(out.position());
			out.rewind();
			this.inner.write(out);
		}

		// sets the position of the input buffer to the next to be written byte
		inBuf.position(inPtr);
		// returns the number of written bytes
		return length;
	}

	public void close() throws IOException {
		this.inner.close();
	}

	public boolean isOpen() {
		return this.inner.isOpen();
	}
}
