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

import java.nio.ByteBuffer;


final class WirePointer {

	public static final byte STRUCT = 0;
	public static final byte LIST = 1;
	public static final byte FAR = 2;
	public static final byte OTHER = 3;

	public static boolean isNull(long wirePointer) {
		return wirePointer == 0;
	}

	public static int offsetAndKind(long wirePointer) {				// the Offset to the pointed object and the kind of the type(here Farpointer & BSpecification) are encoded in the first 32 Bits of the InterSegmentPointer
		return (int) (wirePointer & 0xffffffff);					// & 0xffffffff returns the first 32 Bits of the whole InterSegmentPointer
	}

	public static byte kind(long wirePointer) {						// gets the kind of the object (struct,list, farPointer, other)
		return (byte) (offsetAndKind(wirePointer) & 3);
	}

	public static int target(int offset, long wirePointer) {
		return offset + 1 + (offsetAndKind(wirePointer) >>> 2);
	}

	public static void setKindAndTarget(ByteBuffer buffer, int offset, byte kind, int targetOffset) {
		buffer.putInt(offset * Constants.BYTES_PER_WORD, (((targetOffset - offset) - 1) << 2) | kind);
	}

	public static void setKindWithZeroOffset(ByteBuffer buffer, int offset, byte kind) {
		buffer.putInt(offset * Constants.BYTES_PER_WORD, kind);
	}

	public static void setKindAndTargetForEmptyStruct(ByteBuffer buffer, int offset) {
		//# This pointer points at an empty struct. Assuming the
		//# WirePointer itself is in-bounds, we can set the target to
		//# point either at the WirePointer itself or immediately after
		//# it. The latter would cause the WirePointer to be "null"
		//# (since for an empty struct the upper 32 bits are going to
		//# be zero). So we set an offset of -1, as if the struct were
		//# allocated immediately before this pointer, to distinguish
		//# it from null.

		buffer.putInt(offset * Constants.BYTES_PER_WORD, 0xfffffffc);
	}

	public static void setOffsetAndKind(ByteBuffer buffer, int offset, int offsetAndKind) {		//writes the offset and kind of the FarPointer to the bytebuffer at the given offset
		buffer.putInt(offset * Constants.BYTES_PER_WORD, offsetAndKind);
	}

	public static int inlineCompositeListElementCount(long wirePointer) {
		return offsetAndKind(wirePointer) >>> 2;
	}

	public static void setKindAndInlineCompositeListElementCount(ByteBuffer buffer, int offset, byte kind, int elementCount) {
		buffer.putInt(offset * Constants.BYTES_PER_WORD, (elementCount << 2) | kind);
	}

	public static int upper32Bits(long wirePointer) {										// long types have 64 Bits. the >>> operator shifts the binary code of the long value 32 positions to the right.
		return (int) (wirePointer >>> 32);													// the left side is filled with zeros during the shifting
	}																						// which returns the value of the higher 32 bits of the original long variable
}
