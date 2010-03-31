/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndian;

/**
 * Title: Uncalced Record
 * <P>
 * If this record occurs in the Worksheet Substream, it indicates that the formulas have not 
 * been recalculated before the document was saved.
 * 
 * @author Olivier Leprince
 */

public class UncalcedRecord extends Record 
{
	public final static short sid = 0x5E;

	/**
	 * Default constructor
	 */
	public UncalcedRecord() {
	}

	public short getSid() {
		return sid;
	}

	public UncalcedRecord(RecordInputStream in) {
		in.readShort(); // unused
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[UNCALCED]\n");
		buffer.append("[/UNCALCED]\n");
		return buffer.toString();
	}

	public int serialize(int offset, byte[] data) {
		LittleEndian.putUShort(data, 0 + offset, sid);
		LittleEndian.putUShort(data, 2 + offset, 2);
		LittleEndian.putUShort(data, 4 + offset, 0); // unused
		return getRecordSize();
	}

	public int getRecordSize() {
		return UncalcedRecord.getStaticRecordSize();
	}

	public static int getStaticRecordSize() {
		return 6;
	}
}