/*
 * Copyright 2016 IKS Gesellschaft fuer Informations- und Kommunikationssysteme mbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iksgmbh.sql.pojomemodb.dataobjects.temporal;

import java.math.BigDecimal;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.iksgmbh.sql.pojomemodb.SQLKeyWords;
import com.iksgmbh.sql.pojomemodb.SqlPojoMemoDB;
import com.iksgmbh.sql.pojomemodb.dataobjects.persistent.Column;
import com.iksgmbh.sql.pojomemodb.dataobjects.persistent.Table;

public class JoinTable extends Table
{
	private SqlPojoMemoDB memoryDb;
	
	public JoinTable(final SqlPojoMemoDB aMemoryDb, 
			         final String firstTableName) throws SQLDataException 
	{
		super(firstTableName);
		memoryDb = aMemoryDb;
		
		final Table table = (Table) memoryDb.getTableStoreData().getTableData(firstTableName);
		addColumnsFromDataTable(table);
		setDataRows(table.getDataRows());
	}

	public JoinTable(JoinTable joinTable, String buildJoinTableName) throws SQLDataException 
	{
		super(buildJoinTableName);
		addColumnsFromJoinTable(joinTable);
		setDataRows(joinTable.getDataRows());
	}

	private void addColumnsFromDataTable(final Table table) throws SQLDataException 
	{
		final List<String> namesOfColumns = table.getNamesOfColumns();
		
		for (String columnName : namesOfColumns)
        {
			final Column column = table.getColumn(columnName);
            final ColumnInitData columnInitData = new ColumnInitData(column);
            columnInitData.columnName = table.getTableName() + "." + columnName;
            createNewColumn(columnInitData, memoryDb);
		}
	}

	private void addColumnsFromJoinTable(final JoinTable joinTable) throws SQLDataException 
	{
		final List<String> namesOfColumns = joinTable.getNamesOfColumns();
		
		for (String columnName : namesOfColumns)
        {
			final Column column = joinTable.getColumn(columnName);
            final ColumnInitData columnInitData = new ColumnInitData(column);
            createNewColumn(columnInitData, memoryDb);
		}
	}
	
	/**
	 * Joins the current content of the JoinTable instance with another new table
	 * defined by the two columnIDs in the joinCondition.
	 * 
	 * Precondition: At least one columnID in the joinCondition (right or left hand side) 
	 *               must be known to the JoinTable.
	 * 
	 * If the joinCondition refers only to known tables,
	 * the joinCondition is used to filter the data rows by this condition.
	 * 
	 * @param joinCondition
	 * @return false if joinCondition refers to two new tables. 
	 * @throws SQLDataException 
	 */
	public boolean join(final WhereCondition joinCondition) throws SQLDataException 
	{
		if (joinCondition.getComparator() != SQLKeyWords.COMPARATOR_EQUAL) {
			throw new RuntimeException("This feature is not yet supported!");
		}

		
		if (isLeftHandSideKnownColumnId(joinCondition))
		{
			if (isRightHandSideKnownColumnId(joinCondition))  {
				removeDataRowsNotMatching(joinCondition);
			} else {
				doJoining(joinCondition.getColumnName(), joinCondition.getValueAsString());
			}
		} else {
			if (isRightHandSideKnownColumnId(joinCondition))  {
				doJoining(joinCondition.getValueAsString(), joinCondition.getColumnName());
			} else {
				return false;  // columns in joinCondition does not match columns in joinTable
			}
		}
		
		return true;
	}

	private void removeDataRowsNotMatching(final WhereCondition joinCondition) throws SQLDataException 
	{
		final List<Object[]> newDataRows = new ArrayList<Object[]>();  
		
		for (Object[] dataset : dataRows) 
		{
			final int i1 = getColumn(joinCondition.getColumnName()).getIndexInTable();
			final Object o1 = dataset[i1];
			final int i2 = getColumn(joinCondition.getValueAsString()).getIndexInTable();
			final Object o2 = dataset[i2];
			if (compareEquals(o1, o2)) newDataRows.add(dataset);
		}
		
		setDataRows(newDataRows);
	}

	private void doJoining(final String knownColumnId, 
			               final String newColumnId) throws SQLDataException 
	{
		final String[] splitResult = newColumnId.split("\\.");
		final String newTableName = splitResult[0];
		final String joinColumnOfNewTable = splitResult[1];
		final Table table = (Table) memoryDb.getTableStoreData().getTableData(newTableName);
		addColumnsFromDataTable(table);
		setDataRows( createJoinedDataRows(table, joinColumnOfNewTable, knownColumnId) );
	}

	private List<Object[]> createJoinedDataRows(final Table table, 
			                                    final String nameOfJoinColumnOfNewTable, 
			                                    final String knownColumnId) 
			                                    	  throws SQLDataException 
	{
		final List<Object[]> toReturn = new ArrayList<Object[]>();
		int indexOfKnownColumn = getColumn(knownColumnId).getIndexInTable();
		
		for (Object[] knownDataset : dataRows) 
		{
			final Object o1 = knownDataset[indexOfKnownColumn];
			final List<Object[]> newDataRows = table.getDataRows();
			int indexOfJoinColumnInNewTable = table.getColumn(nameOfJoinColumnOfNewTable).getIndexInTable();
			
			for (Object[] datasetOfNewTable : newDataRows) 
			{
				final Object o2 = datasetOfNewTable[indexOfJoinColumnInNewTable];
				if ( compareEquals(o1, o2) ) {
					toReturn.add(createJoinedDataSet(knownDataset, datasetOfNewTable));
				}
			}
		}
		
		return toReturn;
	}

	private Object[] createJoinedDataSet(final Object[] knownDataset, 
			                             final Object[] datasetOfNewTable) 
	{
		final Object[] toReturn = new Object[knownDataset.length + datasetOfNewTable.length];
		
		int i = -1;
		for (Object object : knownDataset) {
			i++;
			toReturn[i] = object;
		}
		
		for (Object object : datasetOfNewTable) {
			i++;
			toReturn[i] = object;			
		}
		
		return toReturn;
	}

	private boolean compareEquals(final Object o1, final Object o2) throws SQLDataException 
	{
		if (o1 == null || o2 == null) return false;
		
		if (o1.getClass() != o2.getClass()) {
			throw new SQLDataException("Data type mismatch!");
		}
		
		if (o1.getClass().getSimpleName().equals("String")) {
			final String s1 = (String) o1;
			final String s2 = (String) o2;
			return s1.equals(s2);
		}
		
		if (o1.getClass().getSimpleName().equals("BigDecimal")) {
			final BigDecimal d1 = (BigDecimal) o1;
			final BigDecimal d2 = (BigDecimal) o2;
			return d1.compareTo(d2) == 0;
		}
		
		if (o1.getClass().getSimpleName().equals("Integer")) {
			final Integer i1 = (Integer) o1;
			final Integer i2 = (Integer) o2;
			return i1 == i2;
		}

		if (o1.getClass().getName().equals("java.util.Date")) {
			final Date d1 = (Date) o1;
			final Date d2 = (Date) o2;
			return d1.getTime() == d2.getTime();
		}

		throw new SQLDataException("Unsupported data type: " + o1.getClass().getName());
	}

	private boolean isRightHandSideKnownColumnId(WhereCondition joinCondition) {
		return isKnownColumnId(joinCondition.getValueAsString());
	}

	private boolean isLeftHandSideKnownColumnId(WhereCondition joinCondition) {
		return isKnownColumnId(joinCondition.getColumnName());
	}

	private boolean isKnownColumnId(final String s) 
	{
		for (String columnId : sortedColumnNames) {
			if (s.toUpperCase().equals(columnId)) {
				return true;
			}
		}
		return false;
	}

}