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


public final class PackedInputStream implements ReadableByteChannel {

	final BufferedInputStream inner;

	public PackedInputStream(BufferedInputStream input) {
		this.inner = input;
	}

	public int read(ByteBuffer outBuf) throws IOException {
		// -> see CapnProto - packed encoding

		// number of free bytes left on the byte buffer to be read on
		int len = outBuf.remaining();
		// if there is no more space left, return that 0 bytes were written
		if (len == 0) { return 0; }

		// if an unusual number of bytes is left on the bytebuffer, means a number of bytes, which can't be aligned to word boundaries 
		// -> makes sure, that the output of this method is aligned to word boundaries
		if (len % Constants.BYTES_PER_WORD != 0) { throw new Error("PackedInputStream reads must be word-aligned"); }

		// position of the next free byte of the output buffer
		int outPtr = outBuf.position();
		// position of the last free byte of the output buffer
		int outEnd = outPtr + len;
		// gets the buffer of this InputStream, means the buffered bytes of the actual input
		ByteBuffer inBuf = this.inner.getReadBuffer();

		while (true) {

			byte tag = 0;

			if (inBuf.remaining() < 10) {
				// If there are less than 10 bytes remaining in the Input buffer, probably the last word
				if (outBuf.remaining() == 0) { return len; }
				// if there is no more space in the output buffer, return the number of read bytes

				if (inBuf.remaining() == 0) {
					// if the buffer of the input channel is full read, it will be cleared and filled with new bytes from the channel
					inBuf = this.inner.getReadBuffer();
					continue;
				}

				//# We have at least 1, but not 10, bytes available. We need to read
				//# slowly, doing a bounds check on each byte.

				// gets the Tag of the next word
				tag = inBuf.get();

				for (int i = 0; i < Constants.BITS_PER_BYTE; ++i) {
					/*
					 * Iterates through the tag byte and checks, which bits are set.
					 * If it is set:
					 * ___If there are no bytes left to read from the input buffer,
					 * ___it will be filled wit new bytes from the channel.
					 * ___The next byte of the input buffer is written to the output buffer.
					 * If it isn't set:
					 * ___A zero-byte is written to the output buffer.
					 */
					if ((tag & (1 << i)) != 0) {
						if (inBuf.remaining() == 0) {
							inBuf = this.inner.getReadBuffer();
						}
						outBuf.put(inBuf.get());
					} else {
						outBuf.put((byte) 0);
					}
				}

				if (inBuf.remaining() == 0 && (tag == 0 || tag == (byte) 0xff)) {
					/*
					 * If the input buffer is all read, but the previous tag is one of the special
					 * ones (indicating, that there must be more information on the channel).
					 * Then the input buffer is filled with new bytes from input channel.
					 */
					inBuf = this.inner.getReadBuffer();
				}
			} else {
				// If there are more than 10 bytes available to read from the BufferedInput,
				// it reads the next byte as a Tag-byte(see description of Capnp).			
				tag = inBuf.get();
				for (int n = 0; n < Constants.BITS_PER_BYTE; ++n) {
					/*
					 * Iterates through the tag byte and checks, which bits are set.
					 * The '1<<n' shifts the 1 through an 8Bit blob and checks, if that specific bit
					 * in the tag is set or not.
					 * If it is set:
					 * ___It writes the next byte from the input buffer to the output buffer
					 * If it isn't set:
					 * ___A zero-byte is written to the output buffer.
					 */
					if ((tag & (1 << n)) != 0) {
						outBuf.put(inBuf.get());
					} else {
						outBuf.put((byte) 0);
					}
				}
			}

			// checks, if the read tag is the special zero-tag
			if (tag == 0) {
				// at this point, the input must always contain more information, because of the capnp packing-description
				if (inBuf.remaining() == 0) { throw new Error("Should always have non-empty buffer here."); }
				// checks, how many zero-words will follow the 00-tag, from the byte right after the tag
				int runLength = inBuf.get() * Constants.BYTES_PER_WORD;
				// if there are more zero-words to be written than there is space in the output buffer
				if (runLength > outEnd - outPtr) { throw new Error("Packed input did not end cleanly on a segment boundary"); }

				// writes the given number of zero-words to the output buffer
				for (int i = 0; i < runLength; ++i) {
					outBuf.put((byte) 0);
				}
				// checks, if the read tag is the special ff-tag
			} else if (tag == (byte) 0xff) {

				// checks how many words, following this tag should be interpreted as unpacked
				int runLength = inBuf.get() * Constants.BYTES_PER_WORD;

				if (inBuf.remaining() >= runLength) {
					/*
					 * #FastPath
					 * If there are more words to be written, than the unpacked ones,
					 * it writes the next number of bytes(specified by runLength) straight to the
					 * output buffer.
					 */
					WireHelpers.writeSlice(inBuf, runLength, outBuf);
					inBuf.position(inBuf.position() + runLength);
				} else {
					/*
					 * # Copy over the first buffer, then do one big read for the rest.
					 * If there are more unpacked words, than words remaining in this input buffer.
					 * First it checks, how many unpacked words are saved in the current input
					 * buffer and writes these words to the output buffer.
					 * Then it creates a slice of the output buffer, sets it limit to the number of
					 * unpacked words left and
					 * reads the rest of the unpacked words form the input stream to the slice
					 * buffer, whose content is shared with the output buffer
					 * Finally it updates the position of the output buffer.
					 */
					runLength -= inBuf.remaining();
					outBuf.put(inBuf);

					ByteBuffer slice = outBuf.slice();
					slice.limit(runLength);

					this.inner.read(slice);
					outBuf.position(outBuf.position() + runLength);

					if (outBuf.remaining() == 0) {
						// if there is no more space left on the output buffer, return the number of written bytes
						return len;
					} else {
						// if there is still space left, read new bytes from the channel to the input buffer
						inBuf = this.inner.getReadBuffer();
						continue;
					}
				}
			}
			// if there is no more space left on the output buffer, return the number of written bytes
			if (outBuf.remaining() == 0) { return len; }
		}
	}

	public void close() throws IOException {
		inner.close();
	}

	public boolean isOpen() {
		return inner.isOpen();
	}
}
