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

package org.apache.poi.hssf.usermodel;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.RowRecord;

/**
 * High level representation of a row of a spreadsheet.
 *
 * Only rows that have cells should be added to a Sheet.
 * @version 1.0-pre
 * @author  Andrew C. Oliver (acoliver at apache dot org)
 * @author Glen Stampoultzis (glens at apache.org)
 */
public final class HSSFRow implements Comparable {

    // used for collections
    public final static int INITIAL_CAPACITY = 5;

    private int rowNum;
    private HSSFCell[] cells=new HSSFCell[INITIAL_CAPACITY];

    /**
     * reference to low level representation
     */
    private RowRecord row;

    /**
     * reference to containing low level Workbook
     */
    private HSSFWorkbook book;

    /**
     * reference to containing Sheet
     */
    private HSSFSheet sheet;

    /**
     * Creates new HSSFRow from scratch. Only HSSFSheet should do this.
     *
     * @param book low-level Workbook object containing the sheet that contains this row
     * @param sheet low-level Sheet object that contains this Row
     * @param rowNum the row number of this row (0 based)
     * @see org.apache.poi.hssf.usermodel.HSSFSheet#createRow(int)
     */
    HSSFRow(HSSFWorkbook book, HSSFSheet sheet, int rowNum)
    {
        this.rowNum = rowNum;
        this.book = book;
        this.sheet = sheet;
        row = new RowRecord(rowNum);

        setRowNum(rowNum);
    }

    /**
     * Creates an HSSFRow from a low level RowRecord object.  Only HSSFSheet should do
     * this.  HSSFSheet uses this when an existing file is read in.
     *
     * @param book low-level Workbook object containing the sheet that contains this row
     * @param sheet low-level Sheet object that contains this Row
     * @param record the low level api object this row should represent
     * @see org.apache.poi.hssf.usermodel.HSSFSheet#createRow(int)
     */
    HSSFRow(HSSFWorkbook book, HSSFSheet sheet, RowRecord record)
    {
        this.book = book;
        this.sheet = sheet;
        row = record;

        setRowNum(record.getRowNumber());
    }
    /**
     * @deprecated (Aug 2008) use {@link HSSFRow#createCell(int) }
     */
    public HSSFCell createCell(short columnIndex) {
        return createCell((int)columnIndex);
    }
    /**
     * Use this to create new cells within the row and return it.
     * <p>
     * The cell that is returned is a CELL_TYPE_BLANK. The type can be changed
     * either through calling <code>setCellValue</code> or <code>setCellType</code>.
     *
     * @param column - the column number this cell represents
     *
     * @return HSSFCell a high level representation of the created cell.
     */
    public HSSFCell createCell(int columnIndex)
    {
      return this.createCell(columnIndex,HSSFCell.CELL_TYPE_BLANK);
    }

    /**
     * @deprecated (Aug 2008) use {@link HSSFRow#createCell(int, int) }
     */
    public HSSFCell createCell(short columnIndex, int type) {
        return createCell((int)columnIndex, type);
    }
    /**
     * Use this to create new cells within the row and return it.
     * <p>
     * The cell that is returned is a CELL_TYPE_BLANK. The type can be changed
     * either through calling setCellValue or setCellType.
     *
     * @param columnIndex - the column number this cell represents
     *
     * @return HSSFCell a high level representation of the created cell.
     */

    public HSSFCell createCell(int columnIndex, int type)
    {
        HSSFCell cell = new HSSFCell(book, sheet, getRowNum(), (short)columnIndex, type);

        addCell(cell);
        sheet.getSheet().addValueRecord(getRowNum(), cell.getCellValueRecord());
        return cell;
    }

    /**
     * remove the HSSFCell from this row.
     * @param cell to remove
     */
    public void removeCell(HSSFCell cell) {
        if(cell == null) {
            throw new IllegalArgumentException("cell must not be null");
        }
        removeCell(cell, true);
    }
    private void removeCell(HSSFCell cell, boolean alsoRemoveRecords) {
        
        int column=cell.getColumnIndex();
        if(column < 0) {
            throw new RuntimeException("Negative cell indexes not allowed");
        }
        if(column >= cells.length || cell != cells[column]) {
            throw new RuntimeException("Specified cell is not from this row");
        }
        cells[column]=null;
        
        if(alsoRemoveRecords) {
            CellValueRecordInterface cval = cell.getCellValueRecord();
            sheet.getSheet().removeValueRecord(getRowNum(), cval);
        }
        
        if (cell.getColumnIndex()+1 == row.getLastCol()) {
            row.setLastCol((short) (findLastCell(row.getLastCol())+1));
        }
        if (cell.getColumnIndex() == row.getFirstCol()) {
            row.setFirstCol(findFirstCell(row.getFirstCol()));
        }
    }
    
    /**
     * Removes all the cells from the row, and their
     *  records too.
     */
    protected void removeAllCells() {
        for(int i=0; i<cells.length; i++) {
            if(cells[i] != null) {
                removeCell(cells[i], true);
            }
        }
        cells=new HSSFCell[INITIAL_CAPACITY];
    }

    /**
     * create a high level HSSFCell object from an existing low level record.  Should
     * only be called from HSSFSheet or HSSFRow itself.
     * @param cell low level cell to create the high level representation from
     * @return HSSFCell representing the low level record passed in
     */
    protected HSSFCell createCellFromRecord(CellValueRecordInterface cell) {
        HSSFCell hcell = new HSSFCell(book, sheet, cell);

        addCell(hcell);
        return hcell;
    }

    /**
     * set the row number of this row.
     * @param rowNum  the row number (0-based)
     * @throws IndexOutOfBoundsException if the row number is not within the range 0-65535.
     */
    public void setRowNum(int rowNum) {
        if ((rowNum < 0) || (rowNum > RowRecord.MAX_ROW_NUMBER)) {
          throw new IllegalArgumentException("Invalid row number (" + rowNum 
                  + ") outside allowable range (0.." + RowRecord.MAX_ROW_NUMBER + ")");
        }
        this.rowNum = rowNum;
        if (row != null)
        {
            row.setRowNumber(rowNum);   // used only for KEY comparison (HSSFRow)
        }
    }

    /**
     * get row number this row represents
     * @return the row number (0 based)
     */
    public int getRowNum()
    {
        return rowNum;
    }
    
    /**
     * Returns the rows outline level. Increased as you
     *  put it into more groups (outlines), reduced as
     *  you take it out of them.
     * TODO - Should this really be public?
     */
    protected int getOutlineLevel() {
        return row.getOutlineLevel();
    }
    
    /**
     * Moves the supplied cell to a new column, which
     *  must not already have a cell there!
     * @param cell The cell to move
     * @param newColumn The new column number (0 based)
     */
    public void moveCell(HSSFCell cell, short newColumn) {
        // Ensure the destination is free
        if(cells.length > newColumn && cells[newColumn] != null) {
            throw new IllegalArgumentException("Asked to move cell to column " + newColumn + " but there's already a cell there");
        }
        
        // Check it's one of ours
        if(! cells[cell.getColumnIndex()].equals(cell)) {
            throw new IllegalArgumentException("Asked to move a cell, but it didn't belong to our row");
        }
        
        // Move the cell to the new position
        // (Don't remove the records though)
        removeCell(cell, false);
        cell.updateCellNum(newColumn);
        addCell(cell);
    }

    /**
     * used internally to add a cell.
     */
    private void addCell(HSSFCell cell) {

        int column=cell.getColumnIndex();
        // re-allocate cells array as required.
        if(column>=cells.length) {
            HSSFCell[] oldCells=cells;
            int newSize=oldCells.length*2;
            if(newSize<column+1) {
                newSize=column+1;
            }
            cells=new HSSFCell[newSize];
            System.arraycopy(oldCells,0,cells,0,oldCells.length);
        }
        cells[column]=cell;
        
        // fix up firstCol and lastCol indexes
        if (row.getFirstCol() == -1 || column < row.getFirstCol()) {
            row.setFirstCol((short)column);
        }
        
        if (row.getLastCol() == -1 || column >= row.getLastCol()) {
            row.setLastCol((short) (column+1)); // +1 -> for one past the last index 
        }
    }

    /**
     * Get the hssfcell representing a given column (logical cell)
     *  0-based. If you ask for a cell that is not defined, then
     *  you get a null.
     * This is the basic call, with no policies applied
     *
     * @param cellnum  0 based column number
     * @return HSSFCell representing that column or null if undefined.
     */
    private HSSFCell retrieveCell(int cellnum) {
        if(cellnum<0||cellnum>=cells.length) return null;
        return cells[cellnum];
    }
    
    /**
     * @deprecated (Aug 2008) use {@link #getCell(int)}
     */
    public HSSFCell getCell(short cellnum) {
        int ushortCellNum = cellnum & 0x0000FFFF; // avoid sign extension
        return getCell(ushortCellNum);
    }
    
    /**
     * Get the hssfcell representing a given column (logical cell)
     *  0-based.  If you ask for a cell that is not defined then
     *  you get a null, unless you have set a different
     *  {@link MissingCellPolicy} on the base workbook.
     *
     * @param cellnum  0 based column number
     * @return HSSFCell representing that column or null if undefined.
     */
    public HSSFCell getCell(int cellnum) {
        return getCell(cellnum, book.getMissingCellPolicy());
    }
    
    /**
     * Get the hssfcell representing a given column (logical cell)
     *  0-based.  If you ask for a cell that is not defined, then
     *  your supplied policy says what to do
     *
     * @param cellnum  0 based column number
     * @param policy Policy on blank / missing cells
     * @return representing that column or null if undefined + policy allows.
     */
    public HSSFCell getCell(int cellnum, MissingCellPolicy policy) {
        HSSFCell cell = retrieveCell(cellnum);
        if(policy == RETURN_NULL_AND_BLANK) {
            return cell;
        }
        if(policy == RETURN_BLANK_AS_NULL) {
            if(cell == null) return cell;
            if(cell.getCellType() == HSSFCell.CELL_TYPE_BLANK) {
                return null;
            }
            return cell;
        }
        if(policy == CREATE_NULL_AS_BLANK) {
            if(cell == null) {
                return createCell(cellnum, HSSFCell.CELL_TYPE_BLANK);
            }
            return cell;
        }
        throw new IllegalArgumentException("Illegal policy " + policy + " (" + policy.id + ")");
    }

    /**
     * get the number of the first cell contained in this row.
     * @return short representing the first logical cell in the row, or -1 if the row does not contain any cells.
     */
    public short getFirstCellNum()
    {
        if (getPhysicalNumberOfCells() == 0)
            return -1;
        else
            return row.getFirstCol();
    }

    /**
     * Gets the index of the last cell contained in this row <b>PLUS ONE</b>. The result also 
     * happens to be the 1-based column number of the last cell.  This value can be used as a
     * standard upper bound when iterating over cells:
     * <pre> 
     * short minColIx = row.getFirstCellNum();
     * short maxColIx = row.getLastCellNum();
     * for(short colIx=minColIx; colIx&lt;maxColIx; colIx++) {
     *   HSSFCell cell = row.getCell(colIx);
     *   if(cell == null) {
     *     continue;
     *   }
     *   //... do something with cell
     * }
     * </pre>
     * 
     * @return short representing the last logical cell in the row <b>PLUS ONE</b>, or -1 if the
     *  row does not contain any cells.
     */
    public short getLastCellNum() {
        if (getPhysicalNumberOfCells() == 0) {
            return -1;
        }
        return row.getLastCol();
    }


    /**
     * gets the number of defined cells (NOT number of cells in the actual row!).
     * That is to say if only columns 0,4,5 have values then there would be 3.
     * @return int representing the number of defined cells in the row.
     */

    public int getPhysicalNumberOfCells()
    {
      int count=0;
      for(int i=0;i<cells.length;i++)
      {
        if(cells[i]!=null) count++;
      }
      return count;
    }

    /**
     * set the row's height or set to ff (-1) for undefined/default-height.  Set the height in "twips" or
     * 1/20th of a point.
     * @param height  rowheight or 0xff for undefined (use sheet default)
     */

    public void setHeight(short height)
    {

        // row.setOptionFlags(
        row.setBadFontHeight(true);
        row.setHeight(height);
    }

    /**
     * set whether or not to display this row with 0 height
     * @param zHeight  height is zero or not.
     */
    public void setZeroHeight(boolean zHeight) {
        row.setZeroHeight(zHeight);
    }
  
    /**
     * get whether or not to display this row with 0 height
     * @return - zHeight height is zero or not.
     */
    public boolean getZeroHeight() {
        return row.getZeroHeight();
    }

    /**
     * set the row's height in points.
     * @param height  row height in points
     */

    public void setHeightInPoints(float height)
    {

        // row.setOptionFlags(
        row.setBadFontHeight(true);
        row.setHeight((short) (height * 20));
    }

    /**
     * get the row's height or ff (-1) for undefined/default-height in twips (1/20th of a point)
     * @return rowheight or 0xff for undefined (use sheet default)
     */

    public short getHeight()
    {
        short height = row.getHeight();

        //The low-order 15 bits contain the row height.
        //The 0x8000 bit indicates that the row is standard height (optional) 
        if ((height & 0x8000) != 0) height = sheet.getSheet().getDefaultRowHeight();
        else height &= 0x7FFF;

        return height;
    }

    /**
     * get the row's height or ff (-1) for undefined/default-height in points (20*getHeight())
     * @return rowheight or 0xff for undefined (use sheet default)
     */

    public float getHeightInPoints()
    {
        return ((float)getHeight() / 20);
    }

    /**
     * get the lowlevel RowRecord represented by this object - should only be called
     * by other parts of the high level API
     *
     * @return RowRecord this row represents
     */

    protected RowRecord getRowRecord()
    {
        return row;
    }

    /**
     * used internally to refresh the "last cell" when the last cell is removed.
     */

    private short findLastCell(short lastcell)
    {
        short cellnum = (short) (lastcell - 1);
        HSSFCell r = getCell(cellnum);

        while (r == null && cellnum >= 0)
        {
            r = getCell(--cellnum);
        }
        return cellnum;
    }

    /**
     * used internally to refresh the "first cell" when the first cell is removed.
     */

    private short findFirstCell(short firstcell)
    {
        short cellnum = (short) (firstcell + 1);
        HSSFCell r = getCell(cellnum);

        while (r == null && cellnum <= getLastCellNum())
        {
            r = getCell(++cellnum);
        }
        if (cellnum > getLastCellNum())
            return -1;
        return cellnum;
    }
    
    /**
     * Is this row formatted? Most aren't, but some rows
     *  do have whole-row styles. For those that do, you
     *  can get the formatting from {@link #getRowStyle()}
     */
    public boolean isFormatted() {
    	return row.getFormatted();
    }
    /**
     * Returns the whole-row cell styles. Most rows won't
     *  have one of these, so will return null. Call
     *  {@link #isFormatted()} to check first.
     */
    public HSSFCellStyle getRowStyle() {
    	if(!isFormatted()) { return null; }
        short styleIndex = row.getXFIndex();
        ExtendedFormatRecord xf = book.getWorkbook().getExFormatAt(styleIndex);
        return new HSSFCellStyle(styleIndex, xf, book);
    }
    /**
     * Applies a whole-row cell styling to the row.
     */
    public void setRowStyle(HSSFCellStyle style) {
    	row.setFormatted(true);
    	row.setXFIndex(style.getIndex());
    }

    /**
     * Used to specify the different possible policies
     *  if for the case of null and blank cells
     */
    public static class MissingCellPolicy {
        private static int NEXT_ID = 1;
        private final int id;
        /* package */ MissingCellPolicy() {
            this.id = NEXT_ID++;
        }
    }

    /** Missing cells are returned as null, Blank cells are returned as normal */
    public static final MissingCellPolicy RETURN_NULL_AND_BLANK = new MissingCellPolicy();
    /** Missing cells are returned as null, as are blank cells */
    public static final MissingCellPolicy RETURN_BLANK_AS_NULL = new MissingCellPolicy();
    /** A new, blank cell is created for missing cells. Blank cells are returned as normal */
    public static final MissingCellPolicy CREATE_NULL_AS_BLANK = new MissingCellPolicy();
    

    /**
     * @return cell iterator of the physically defined cells. 
     * Note that the 4th element might well not be cell 4, as the iterator
     *  will not return un-defined (null) cells.
     * Call getCellNum() on the returned cells to know which cell they are.
     * As this only ever works on physically defined cells, 
     *  the {@link MissingCellPolicy} has no effect.
     */
    public Iterator cellIterator()
    {
      return new CellIterator();
    }
    /**
     * Alias for {@link CellIterator} to allow
     *  foreach loops
     */
    public Iterator iterator() { 
       return cellIterator();
    }
    
    /**
     * An iterator over the (physical) cells in the row.
     */
    private class CellIterator implements Iterator
    {
      int thisId=-1;
      int nextId=-1;
      
      public CellIterator()
      {
        findNext();
      }

      public boolean hasNext() {
        return nextId<cells.length;
      }

      public Object next() {
          if (!hasNext())
              throw new NoSuchElementException("At last element");
        HSSFCell cell=cells[nextId];
        thisId=nextId;
        findNext();
        return cell;
      }

      public void remove() {
          if (thisId == -1)
              throw new IllegalStateException("remove() called before next()");
        cells[thisId]=null;
      }
      
      private void findNext()
      {
        int i=nextId+1;
        for(;i<cells.length;i++)
        {
          if(cells[i]!=null) break;
        }
        nextId=i;
      }
      
    }

    public int compareTo(Object obj)
    {
        HSSFRow loc = (HSSFRow) obj;

        if (this.getRowNum() == loc.getRowNum())
        {
            return 0;
        }
        if (this.getRowNum() < loc.getRowNum())
        {
            return -1;
        }
        if (this.getRowNum() > loc.getRowNum())
        {
            return 1;
        }
        return -1;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof HSSFRow))
        {
            return false;
        }
        HSSFRow loc = (HSSFRow) obj;

        if (this.getRowNum() == loc.getRowNum())
        {
            return true;
        }
        return false;
    }
}
