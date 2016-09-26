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

public class ListReader {

	public interface Factory<T> {

		T constructReader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize,
				short structPointerCount, int nestingLimit);
	}

	final SegmentReader segment;
	final int ptr; // byte offset to front of list		// offset to the first element of list
	final int elementCount;								// amount of elements in the list
	final int step; // in bits							// probably the size of each element
	final int structDataSize; // in bits				// size of the actual data of the List
	final short structPointerCount;						// number of pointers in the list
	final int nestingLimit;								// maybe the max size of the list ?

	public ListReader() {
		this.segment = null;
		this.ptr = 0;
		this.elementCount = 0;
		this.step = 0;
		this.structDataSize = 0;
		this.structPointerCount = 0;
		this.nestingLimit = 0x7fffffff;
	}

	public ListReader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount,
			int nestingLimit) {
		this.segment = segment;
		this.ptr = ptr;
		this.elementCount = elementCount;
		this.step = step;
		this.structDataSize = structDataSize;
		this.structPointerCount = structPointerCount;
		this.nestingLimit = nestingLimit;

	}

	public int size() {
		return this.elementCount;
	}

	protected boolean _getBooleanElement(int index) {														//checks if a specified bit of a list element is set or not
		int bitIndex = Math.multiplyExact(index, this.step);
		byte b = this.segment.buffer.get(this.ptr + bitIndex / Constants.BITS_PER_BYTE);
		return (b & (1 << (bitIndex % Constants.BITS_PER_BYTE))) != 0;
	}

	protected byte _getByteElement(int index) {																					// reads the bytes of the element at the given index
		return this.segment.buffer.get(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected short _getShortElement(int index) {																				// reads the bytes of the element at the given index
		return this.segment.buffer.getShort(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);				// and transforms them into a short value
	}

	protected int _getIntElement(int index) {																					// reads the bytes of the element at the given index
		return this.segment.buffer.getInt(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);					// and transforms them into an int value
	}

	protected long _getLongElement(int index) {																					// reads the bytes of the element at the given index
		return this.segment.buffer.getLong(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);				// and transforms them into a long value
	}

	protected float _getFloatElement(int index) {																				// reads the bytes of the element at the given index
		return this.segment.buffer.getFloat(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);				// and transforms them into a float value
	}

	protected double _getDoubleElement(int index) {																				// reads the bytes of the element at the given index
		return this.segment.buffer.getDouble(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);				// and transforms them into a double value
	}

	protected <T> T _getStructElement(StructReader.Factory<T> factory, int index) {												//creates a new Reader for a struct object
		// TODO check nesting limit
		int structData = this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE;					// beginning of the DataSection of the struct
		int structPointers = (structData + (this.structDataSize / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD;		// beginning of the PointerSection of the struct

		return factory.constructReader(this.segment, structData, structPointers, this.structDataSize, this.structPointerCount,
				this.nestingLimit - 1);
	}

	protected <T> T _getPointerElement(FromPointerReader<T> factory, int index) {												//creates a new Reader for a pointer Object
		return factory.fromPointerReader(this.segment,
				(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE) / Constants.BYTES_PER_WORD,
				this.nestingLimit);
	}

	protected <T> T _getPointerElement(FromPointerReaderBlobDefault<T> factory, int index,										//creates a new Reader for a pointer Object
			java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
		return factory.fromPointerReaderBlobDefault(this.segment, (this.ptr + Math.multiplyExact(index, this.step)
				/ Constants.BITS_PER_BYTE)
				/ Constants.BYTES_PER_WORD, defaultBuffer, defaultOffset, defaultSize);
	}

}
