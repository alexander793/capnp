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

public final class GeneratedClassSupport {

	public static SegmentReader decodeRawBytes(String s) {
		try {
			java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(s.getBytes("ISO_8859-1")).asReadOnlyBuffer();		// encodes the string to ISO_8859-1 and wraps the returned ByteArray to a read-only ByteBuffer
			buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);															// orders the bytes of the byte buffer to little endian
			return new SegmentReader(buffer, new ReaderArena(new java.nio.ByteBuffer[0], 0x7fffffffffffffffL));		// returns a SegmentReader how should be able to read the given bytes
		}
		catch (Exception e) {
			throw new Error("could not decode raw bytes from String");
		}
	}
}
