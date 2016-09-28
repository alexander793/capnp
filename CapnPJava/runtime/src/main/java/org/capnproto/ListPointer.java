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


// class for the CapnProto ListPointer Encoding object
final class ListPointer {

	public static byte elementSize(long ref) {
		/*
		 * This method gives the size of an element in the list.
		 * It takes the higher 32 Bits of the ListPointer and &-joins them with 7 (=1110),
		 * which returns the value of the 33.,34.,35. bit -> encoded size of each element.
		 */
		return (byte) (WirePointer.upper32Bits(ref) & 7);
	}

	public static int elementCount(long ref) {
		/*
		 * This method gets the higher 32 bits of the list encoding and
		 * shifts it 3 bits to the lower direction, cutting the size information off.
		 * It returns the number of elements in the list.
		 */
		return WirePointer.upper32Bits(ref) >>> 3;
	}

	public static int inlineCompositeWordCount(long ref) {
		// if the elements of the list are structs
		return elementCount(ref);
	}

	public static void set(ByteBuffer buffer, int offset, byte elementSize, int elementCount) {
		/*
		 * This method sets new values for the number of elements and their size.
		 * It multiplies with Bytes_per_word, because the offset in the ListPointer is defined in
		 * words; +4 to jump over the 'kind' value
		 * TODO length assertion
		 */
		buffer.putInt(Constants.BYTES_PER_WORD * offset + 4, (elementCount << 3) | elementSize);
	}

	public static void setInlineComposite(ByteBuffer buffer, int offset, int wordCount) {
		/*
		 * If the elements of the list are structs, this method writes a 7 in the C Block(size
		 * specification) and the number of words in the list to the D Block.
		 * TODO length assertion
		 */
		buffer.putInt(Constants.BYTES_PER_WORD * offset + 4, (wordCount << 3) | ElementSize.INLINE_COMPOSITE);
	}
}
