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


//the Far-/InterSegment-Pointer described by Cap'nProto
final class FarPointer {

	public static int getSegmentId(long ref) {
		/*
		 * This method reads the segment ID.
		 * The ID of the segment pointed to is specified in the higher 32 Bits of a
		 * Far-/InterSegment-Pointer.
		 */
		return WirePointer.upper32Bits(ref);
	}

	public static int positionInSegment(long ref) {
		/*
		 * This method gets the Offset(the position of the object pointed to) from the FarPointer.
		 * The >>> operator cuts off the lowest 3 Bits(specification of Type-> see CapnProto
		 * Encoding) and replaces the left side with zeros
		 */
		return WirePointer.offsetAndKind(ref) >>> 3;
	}

	public static boolean isDoubleFar(long ref) {
		/*
		 * This method checks, if the object pointed to is a FarPointer itself.
		 * The '>>> 2' shifts the result of offsetAndKind() 2 Bits to the lower direction, leaving
		 * the
		 * B section of the encoding at the beginning of the Bitflow.
		 * The '&1' checks if that B bit is 0 or 1
		 * If it is 0, than the object pointed to is a normal pointer, pointing to the actual
		 * object.
		 * If its 1, than the object pointed to is a FarPointer, pointing to another segment.
		 * (see CapnProto Encoding)
		 */
		return ((WirePointer.offsetAndKind(ref) >>> 2) & 1) != 0;
	}

	public static void setSegmentId(ByteBuffer buffer, int offset, int segmentId) {
		/*
		 * This method sets the ID of the segment pointed to, to the higher 32 Bits of the Buffer.
		 * putInt() writes four bytes containing the value of segmentID to the higher 32 Bits of the
		 * ByteBuffer.
		 * The 'offset' is the offset block of the FarPointer-segment; +4 jumps over the first 3
		 * Bits of the Segment, defining the kind of the far pointer.
		 */
		buffer.putInt(Constants.BYTES_PER_WORD * offset + 4, segmentId);
	}

	public static void set(ByteBuffer buffer, int offset, boolean isDoubleFar, int pos) {
		/*
		 * This method sets the offset and kind of the FarPointer to the given parameters.
		 * First it translates the 'isDoubleFar' to a 0 or 1.
		 * The third parameter specifies the 'offsetAndKind' attribute of the called method.
		 * The multiple OR operation creates a new bit string containing the information about
		 * the offset and kind of the FarPointer.
		 * '(pos << 3)' shifts the bits of 'pos' 3 times to the higher side, leaving the 3 lowest
		 * bits 0.
		 * '(idf << 2)' shifts the bits of 'idf' 2 times to the higher side, leaving the 2 lowest
		 * bits 0.
		 * WirePointer.FAR equals 2, so '10' in binary.
		 * If you now OR-join all these 3, no information is lost and a bit string containing all
		 * the needed information is created.
		 */
		int idf = isDoubleFar ? 1 : 0;
		WirePointer.setOffsetAndKind(buffer, offset, (pos << 3) | (idf << 2) | WirePointer.FAR);
	}
}
