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


final class FarPointer {										//should be the Far-/InterSegment-Pointer described by Cap'nProto

	public static int getSegmentId(long ref) {					// reads the segment ID 
		return WirePointer.upper32Bits(ref);					// the ID of the segment pointed to is specified in the higher 32 Bits of a Far-/InterSegment-Pointer
	}

	public static int positionInSegment(long ref) {				// gets the Offset(the position of the object pointed to) from the FarPointer
		return WirePointer.offsetAndKind(ref) >>> 3;			// the >>> operator cuts the lowest 3 Bits(specification of Type-> see CapnProto Encoding) off and replaces the left side with zeros
	}

	public static boolean isDoubleFar(long ref) {						// checks, if the object pointed to is a FarPointer itself
		return ((WirePointer.offsetAndKind(ref) >>> 2) & 1) != 0;		// the >>>2 shifts the result of offsetAndKind() 2 Bits to the lower direction, leaving the B section of the encoding at the beginning of the Bitflow
																	// &1 checks if that Bit is 0 or 1
																	// if it is 0, than the object pointed to is another pointer, pointing to the actual object
																	// if its 1, than the object pointed to is a FarPointer, pointing to another segment(see CapnProto Encoding)
	}

	public static void setSegmentId(ByteBuffer buffer, int offset, int segmentId) {		// sets the ID of the segment pointed to the higher 32 Bits of the Buffer 
		buffer.putInt(Constants.BYTES_PER_WORD * offset + 4, segmentId);				// putInt() writes four bytes containing the value of segmentID to the higher 32 Bits of the ByteBuffer
	}		// the 'offset' is the offset block of the FarPointer-segment; +4 jumps over the first 3 Bits of the Segment, defining the kind of the far pointer

	public static void set(ByteBuffer buffer, int offset, boolean isDoubleFar, int pos) {			// sets the Offset and kind of the FarPointer to the given parameters
		int idf = isDoubleFar ? 1 : 0;																// translates the isDoubleFar to a 0 or 1
		WirePointer.setOffsetAndKind(buffer, offset, (pos << 3) | (idf << 2) | WirePointer.FAR);	// the third parameter specifies the offsetAndKind attribute of the called method
																									// the multiple OR operation creates a new Bitflow containing information about the Offset and Kind of the FarPointer
																									// (pos << 3) shifts the Bits of pos 3 times to the higher side, leaving the 3 lowest Bits 0
																									// (idf << 2) shifts the Bits of idf 2 times to the higher side, leaving the 2 lowest Bits 0
																									// WirePointer.FAR equals 2, so 10 in binary
																									// if you now OR-join all these 3, no information is lost and a Bitflow containing all the needed information is created
	}
}
