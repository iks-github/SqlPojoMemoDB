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
package com.iksgmbh.sql.pojomemodb.sqlparser;

import com.iksgmbh.sql.pojomemodb.SQLKeyWords;
import com.iksgmbh.sql.pojomemodb.SqlExecutor.ParsedSelectData;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class SelectParserTest
{	
	private SelectParser sut = new SelectParser(null);	

	@Test
	public void parsesSelectStatementForAllColumnsWithAlias() throws SQLException 
	{
		// arrange (statement 1 contains table alias with a value that is a prefix of a another one !)
	    final String selectStatement1 = "select a.* from Table1 a join Table2 ta on (a.ID = ta.ID) join Table3 tab on (ta.ID = tab.ID) where tab.name = null and ta.name = null";
		final String selectStatement2 = "select m.* from modell m  where m.name = null";
		
		// act
		final ParsedSelectData result1 = sut.parseSelectSql(selectStatement1);
		final ParsedSelectData result2 = sut.parseSelectSql(selectStatement2);
		
		// assert
		assertEquals("tableName", "TABLE1", result1.tableNames.get(0));
		assertEquals("tableName", "MODELL", result2.tableNames.get(0));
	}	

	@Test
	public void throwsExceptionForUnknownAliasWithWithColumns() throws SQLException 
	{
		// arrange
	    final String selectStatement = "select  n.* from modell m  where m.name = null";
		
		try {
			// act
			sut.parseSelectSql(selectStatement);
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// assert
			assertEquals("Error message", "Column information: <n.*> is not parseable!", e.getMessage());
		}
	}	
	
	
	@Test
	public void parsesSelectStatementForNextSequenceValueInMySqlDialect() throws SQLException 
	{
		// arrange
	    final String selectStatement = "select NextId(\"testmodell\")";
		
		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "testmodell", result.mysqlNextIdTable);
	}	
	
	@Test
	public void parsesSelectStatementWithTableNameInFrontOfColumnNames() throws SQLException 
	{
		// arrange
	    final String selectStatement = "select TEST_TABLE_NAME.ID, TEST_TABLE_NAME.TYPE " +
	                                   "from TEST_TABLE_NAME";
		
		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("# selected columns", 2, result.selectedColumns.size());
		assertEquals("first column name", "ID", result.selectedColumns.get(0));
	}	
	
	@Test
	public void parsesSelectStatementWithTableAliasNameAndAs() throws SQLException 
	{
		// arrange
	    final String selectStatement = "select TT.ID, TT.TYPE, TT.NAME " +
	                                   "from TEST_TABLE_NAME as TT";
		
		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("# selected columns", 3, result.selectedColumns.size());
		assertEquals("first column name", "ID", result.selectedColumns.get(0));
	}
	
	@Test
	public void parsesSelectStatementWithTableAliasName() throws SQLException 
	{
		// arrange
	    final String selectStatement = "select TT.ID, TT.TYPE, TT.NAME " +
	                                   "from TEST_TABLE_NAME TT";
		
		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("# selected columns", 3, result.selectedColumns.size());
		assertEquals("first column name", "ID", result.selectedColumns.get(0));
	}

	@Test
	public void parsesSelectStatementWithoutWhereConditionForAllColumns() throws SQLException 
	{
		// arrange
		final String selectStatement =  "select * from TEST_TABLE_NAME";
		
		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("selected columns", null, result.selectedColumns);
		assertEquals("number of where conditions", 0, result.whereConditions.size());
	}
	
	@Test
	public void parsesSelectStatementWithoutWhereConditionForSubsetOfColumns() throws SQLException
	{
		// arrange
		final String selectStatement =  "select Name, From from TEST_TABLE_NAME";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("selected columns", 2, result.selectedColumns.size());
		assertEquals("column name", "Name", result.selectedColumns.get(0));
		assertEquals("column name", "From", result.selectedColumns.get(1));
		assertEquals("number of where conditions", 0, result.whereConditions.size());
	}
	
	@Test
	public void parsesSelectStatementWithWhereConditionForSubsetOfColumns() throws SQLException
	{
		// arrange
		final String selectStatement =  "select Name, From from TEST_TABLE_NAME where Until=to_date('15.05.16','DD.MM.RR') AND ID=123";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);
		
		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("selected columns", 2, result.selectedColumns.size());
		assertEquals("column name", "Name", result.selectedColumns.get(0));
		assertEquals("column name", "From", result.selectedColumns.get(1));
		assertEquals("number of where conditions", 2,  result.whereConditions.size() );
		assertEquals("column name", "Until",  result.whereConditions.get(0).getColumnName() );
		assertEquals("Comparator", "=",  result.whereConditions.get(0).getComparator() );
		assertEquals("value", "to_date('15.05.16','DD.MM.RR')",  result.whereConditions.get(0).getValueAsString() );
		assertEquals("column name", "ID",  result.whereConditions.get(1).getColumnName() );
		assertEquals("Comparator", "=",  result.whereConditions.get(1).getComparator() );
		assertEquals("value", "123",  result.whereConditions.get(1).getValueAsString() );
	}

	
	// #############################################################################################
	//                                   J O I N    T E S T S
	// #############################################################################################


	@Test
	public void parsesJoinStatementUsingNonAnsiSyntax() throws SQLException
	{
		// arrange
		final String selectStatement = "select T1.Name, T2.Name, T1.From, T2.Type from TEST_TABLE_NAME_1 as T1, TEST_TABLE_NAME_2 as T2 where T1.ID=T2.ID";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# columns", 4, result.selectedColumns.size());
		assertEquals("# whereConditions", 1, result.whereConditions.size());
		assertEquals("# tables", 2, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_1", result.tableNames.get(0));
		assertEquals("where condition column name", "TEST_TABLE_NAME_1.ID", result.whereConditions.get(0).getColumnName());
		assertEquals("where condition value", "TEST_TABLE_NAME_2.ID", result.whereConditions.get(0).getValueAsString());
	}
	
	@Test
	public void parsesJoinStatement_UsingNonAnsiSyntax_ThreeJoinTables() throws SQLException
	{
		// arrange
		final String selectStatement = "select T1.Name, T2.Name, T3.From, T2.Type from TEST_TABLE_NAME_1 as T1, TEST_TABLE_NAME_2 as T2, TEST_TABLE_NAME_3 as T3 "
				                        + "where T1.ID=T2.ID and T2.Name=T3.Name";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# columns", 4, result.selectedColumns.size());
		assertEquals("# whereConditions", 2, result.whereConditions.size());
		assertEquals("# tables", 3, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_1", result.tableNames.get(0));
		assertEquals("where condition column name", "TEST_TABLE_NAME_1.ID", result.whereConditions.get(0).getColumnName());
		assertEquals("where condition value", "TEST_TABLE_NAME_2.ID", result.whereConditions.get(0).getValueAsString());
	}
	

	@Test
	public void throwsExceptionForUnkownAliases_InSelectClause_UsingNonAnsiSyntax() throws SQLException
	{
		// arrange
		final String selectStatement = "select T3.Name, T2.Name, T1.From, T2.Type from TEST_TABLE_NAME_1 as T1, TEST_TABLE_NAME_2 as T2 where T1.ID=T2.ID";

		try {
			// act
			sut.parseSelectSql(selectStatement);
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// assert
			assertEquals("Error message", "Unknown column id <T3.Name> detected.", e.getMessage());
		}
	}

	@Test
	public void throwsExceptionForUnkownAliases_InWhereClause_UsingNonAnsiSyntax() throws SQLException
	{
		// arrange
		final String selectStatement = "select T1.Name, T2.Name, T1.From, T2.Type from TEST_TABLE_NAME_1 as T1, TEST_TABLE_NAME_2 as T2 where T1.ID=T3.ID";

		try {
			// act
			sut.parseSelectSql(selectStatement);
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// assert
			assertEquals("Error message", "Unknown column id <T3.ID> detected.", e.getMessage());
		}
	}

	@Test
	public void parsesInnerJoinStatementUsing_AnsiSyntax() throws SQLException
	{
		// arrange
		final String selectStatement = "select * from TEST_TABLE_NAME_1 T1 INNER JOIN TEST_TABLE_NAME_2 T2 on T1.ID=T2.ID";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# whereConditions", 1, result.whereConditions.size());
		assertEquals("# tables", 2, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_1", result.tableNames.get(0));
		assertEquals("where condition column name", "TEST_TABLE_NAME_1.ID", result.whereConditions.get(0).getColumnName());
		assertEquals("where condition value", "TEST_TABLE_NAME_2.ID", result.whereConditions.get(0).getValueAsString());
	}
	
	@Test
	public void throwsExceptionForMissingOnClauseInAnsiSyntax() throws SQLException
	{
		// arrange
		final String selectStatement = "select * from TEST_TABLE_NAME_1 T1 INNER JOIN TEST_TABLE_NAME_2 T2";

		try {
			// act
			sut.parseSelectSql(selectStatement);
			fail("Expected exception was not thrown!");
		} catch (Exception e) {
			// assert
			assertEquals("Error message", "Missing ON keyword: TEST_TABLE_NAME_1 T1 INNER JOIN TEST_TABLE_NAME_2 T2", e.getMessage());
		}
	}
	

	@Test
	public void parsesSimpleJoinStatementUsing_asInnerJoin() throws SQLException
	{
		// arrange
		final String selectStatement = "select * from TEST_TABLE_NAME_1 T1 JOIN TEST_TABLE_NAME_2 T2 on T1.ID=T2.ID";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# whereConditions", 1, result.whereConditions.size());
		assertEquals("# tables", 2, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_1", result.tableNames.get(0));
		assertEquals("where condition column name", "TEST_TABLE_NAME_1.ID", result.whereConditions.get(0).getColumnName());
		assertEquals("where condition value", "TEST_TABLE_NAME_2.ID", result.whereConditions.get(0).getValueAsString());
	}

	@Test
	public void parsesSimpleJoinStatementUsing_asInnerJoin_withWhereClause() throws SQLException
	{
		// arrange
		final String selectStatement = "select * from TEST_TABLE_NAME_1 T1 JOIN TEST_TABLE_NAME_2 T2 on T1.ID=T2.ID where T1.size > 0";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# whereConditions", 2, result.whereConditions.size());
		assertEquals("# tables", 2, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_1", result.tableNames.get(0));
	}
	
	@Test
	public void parsesSimpleJoinStatementUsing_asInnerJoin_ThreeJoinTables() throws SQLException
	{
		// arrange
		final String selectStatement = "select * from TEST_TABLE_NAME_1 T1 JOIN TEST_TABLE_NAME_2 T2 on T1.ID=T2.ID "
				                                                        + "JOIN TEST_TABLE_NAME_3 T3 on T3.ID=T2.ID";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("# whereConditions", 2, result.whereConditions.size());
		assertEquals("# tables", 3, result.tableNames.size());
		assertEquals("table name", "TEST_TABLE_NAME_3", result.tableNames.get(2));
		assertEquals("where condition column name", "TEST_TABLE_NAME_3.ID", result.whereConditions.get(1).getColumnName());
		assertEquals("where condition value", "TEST_TABLE_NAME_2.ID", result.whereConditions.get(1).getValueAsString());
	}

	@Test
	public void parsesSelectStatementWithOrderBy() throws SQLException
	{
		// arrange
		final String selectStatement = "select TEST_TABLE_NAME.ID, TEST_TABLE_NAME.TYPE " +
				"from TEST_TABLE_NAME order by TEST_TABLE_NAME.ID";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("tableName", "TEST_TABLE_NAME", result.tableNames.get(0));
		assertEquals("# selected columns", 2, result.selectedColumns.size());
		assertEquals("first column name", "ID", result.selectedColumns.get(0));
		assertEquals("order direction", SQLKeyWords.ASC, result.orderConditions.get(0).getDirection());
	}

	@Test
	public void parsesSelectStatementWithOrderBy_asc_desc() throws SQLException
	{
		// arrange
		final String selectStatement = "select TEST_TABLE_NAME.ID, TEST_TABLE_NAME.TYPE " +
				"from TEST_TABLE_NAME order by TEST_TABLE_NAME.ID ASC, TEST_TABLE_NAME.TYPE desc";

		// act
		final ParsedSelectData result = sut.parseSelectSql(selectStatement);

		// assert
		assertEquals("order column name 1", "TEST_TABLE_NAME.ID", result.orderConditions.get(0).getColumnName());
		assertEquals("order column name 2", "TEST_TABLE_NAME.TYPE", result.orderConditions.get(1).getColumnName());
		assertEquals("order direction 1", SQLKeyWords.ASC, result.orderConditions.get(0).getDirection());
		assertEquals("order direction 2", SQLKeyWords.DESC, result.orderConditions.get(1).getDirection());
	}

	@Test
	public void throwsExceptionForMissingOrderColumn() throws SQLException
	{
		// arrange
		final String selectStatement = "select TEST_TABLE_NAME.ID, TEST_TABLE_NAME.TYPE from TEST_TABLE_NAME order by";


		try{
			// act
			sut.parseSelectSql(selectStatement);
			fail("Expected exception was not thrown!");
		} catch (Exception e){
			// assert
			assertEquals("Error message", "No column defined for order by!", e.getMessage());
		}
	}

}