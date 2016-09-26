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

public class ListBuilder {

	public interface Factory<T> {

		T constructBuilder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize,
				short structPointerCount);
	}

	final SegmentBuilder segment;
	final int ptr; // byte offset to front of list
	final int elementCount;
	final int step; // in bits
	final int structDataSize; // in bits				// if the List contains a struct, this variable defines the size of its data block
	final short structPointerCount;

	public ListBuilder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount) {
		this.segment = segment;
		this.ptr = ptr;
		this.elementCount = elementCount;
		this.step = step;
		this.structDataSize = structDataSize;
		this.structPointerCount = structPointerCount;
	}

	public int size() {
		return this.elementCount;
	}

	protected boolean _getBooleanElement(int index) {
		int bitIndex = Math.multiplyExact(index, this.step);
		byte b = this.segment.buffer.get(this.ptr + bitIndex / Constants.BITS_PER_BYTE); // jumps to the to be checked byte
		return (b & (1 << (bitIndex % Constants.BITS_PER_BYTE))) != 0;
	}

	protected byte _getByteElement(int index) {
		return this.segment.buffer.get(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected short _getShortElement(int index) {
		return this.segment.buffer.getShort(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected int _getIntElement(int index) {
		return this.segment.buffer.getInt(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected long _getLongElement(int index) {
		return this.segment.buffer.getLong(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected float _getFloatElement(int index) {
		return this.segment.buffer.getFloat(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected double _getDoubleElement(int index) {
		return this.segment.buffer.getDouble(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE);
	}

	protected void _setBooleanElement(int index, boolean value) {
		int bitOffset = Math.multiplyExact(index, this.step);										// offset to the to be set Bit
		byte bitnum = (byte) (bitOffset % Constants.BITS_PER_BYTE);				// the number of the Bit in the byte it is in
		int position = (this.ptr + bitOffset / Constants.BITS_PER_BYTE);// position of the byte in the data section
		byte oldValue = this.segment.buffer.get(position);
		this.segment.buffer.put(position, (byte) ((oldValue & (~(1 << bitnum))) | ((value ? 1 : 0) << bitnum)));	// the left side of the | zeros the selected bit; the right side sets the new value
	}

	protected void _setByteElement(int index, byte value) {
		this.segment.buffer.put(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected void _setShortElement(int index, short value) {
		this.segment.buffer.putShort(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected void _setIntElement(int index, int value) {
		this.segment.buffer.putInt(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected void _setLongElement(int index, long value) {
		this.segment.buffer.putLong(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected void _setFloatElement(int index, float value) {
		this.segment.buffer.putFloat(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected void _setDoubleElement(int index, double value) {
		this.segment.buffer.putDouble(this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE, value);
	}

	protected final <T> T _getStructElement(StructBuilder.Factory<T> factory, int index) {
		int structData = this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE;		// gets the start of the struct data section
		int structPointers = (structData + (this.structDataSize / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD;	// gets the start of the pointer section
		//devides by Constants.Bytes_per_word, because the structData value is given in bytes but CapnProto defines this value in words

		return factory.constructBuilder(this.segment, structData, structPointers, this.structDataSize, this.structPointerCount);
	}

	protected final <T> T _getPointerElement(FromPointerBuilder<T> factory, int index) {
		return factory.fromPointerBuilder(this.segment, (this.ptr + Math.multiplyExact(index, this.step)
				/ Constants.BITS_PER_BYTE)
				/ Constants.BYTES_PER_WORD);
	}

	protected final <T> T _initPointerElement(FromPointerBuilder<T> factory, int index, int elementCount) {
		return factory.initFromPointerBuilder(this.segment, (this.ptr + Math.multiplyExact(index, this.step)
				/ Constants.BITS_PER_BYTE)
				/ Constants.BYTES_PER_WORD, elementCount);
	}

	protected final <Builder, Reader> void _setPointerElement(SetPointerBuilder<Builder, Reader> factory, int index, Reader value) {
		factory.setPointerBuilder(this.segment, (this.ptr + Math.multiplyExact(index, this.step) / Constants.BITS_PER_BYTE)
				/ Constants.BYTES_PER_WORD, value);
	}

}
