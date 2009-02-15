/*
 * 著作権: Copyright (c) 2007−2008 ZIGEN
 * ライセンス：Eclipse Public License - v 1.0
 * 原文：http://www.eclipse.org/legal/epl-v10.html
 */
package zigen.plugin.db.core.rule.mysql;

import java.sql.SQLException;

import zigen.plugin.db.core.rule.DefaultStatementFactory;

public class MySQLStatementFactory extends DefaultStatementFactory {

	public MySQLStatementFactory(boolean convertUnicode) {
		super(convertUnicode);
	}

	protected String getDate(Object value) throws SQLException {
		if (value == null)
			return NULL;
		return "'" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public char getEncloseChar(){
		return '`'; //MySQL は `(バッククォーテーション)
	}
}
