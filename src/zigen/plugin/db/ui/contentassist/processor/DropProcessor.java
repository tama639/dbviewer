/*
 * 著作権: Copyright (c) 2007−2008 ZIGEN
 * ライセンス：Eclipse Public License - v 1.0
 * 原文：http://www.eclipse.org/legal/epl-v10.html
 */
package zigen.plugin.db.ui.contentassist.processor;

import java.util.List;

import zigen.plugin.db.DbPlugin;
import zigen.plugin.db.core.TableInfo;
import zigen.plugin.db.ui.contentassist.ContentAssistUtil;
import zigen.plugin.db.ui.contentassist.ContentInfo;
import zigen.plugin.db.ui.contentassist.ProcessorInfo;
import zigen.plugin.db.ui.contentassist.SQLProposalCreator2;
import zigen.sql.parser.SqlParser;
import zigen.sql.parser.ast.ASTDropStatement;

public class DropProcessor extends DefaultProcessor {

	public DropProcessor(List proposals, ProcessorInfo info) {
		super(proposals, info);
	}

	public void createProposals(ASTDropStatement st) {

		String[] modifiers = rule.getKeywordNames();
		try {
			ContentInfo ci = new ContentInfo(ContentAssistUtil.getIDBConfig());

			if (ci.isConnected()) {

				TableInfo[] currentTableInfos = ci.getTableInfo(); // カレントスキーマに対応するテーブル情報リスト取得

				switch (currentScope) {
				case SqlParser.SCOPE_DROP:
					// Deleteのあとは、Fromのみ表示
					modifiers = new String[] {"TABLE", //$NON-NLS-1$
							"VIEW", //$NON-NLS-1$
							"SYNONYM" //$NON-NLS-1$
					};

					break;

				case SqlParser.SCOPE_TARGET:
					// テーブルリストを表示する
					//SQLProposalCreator2.addProposal(proposals, currentTableInfos, pinfo);
					if (isAfterPeriod) {
						addTableProposalBySchema(ci, word);

					} else {
						int _offset = wordGroup.indexOf('.');
						if (_offset > 0) {
							String w_schema = wordGroup.substring(0, _offset);
							addTableProposalBySchema(ci, w_schema);
						} else {
							SQLProposalCreator2.addProposal(proposals, currentTableInfos, pinfo);// テーブル名の一覧
							SQLProposalCreator2.addProposal(proposals, ci.getSchemaInfos(), pinfo);// スキーマの一覧
						}

					}

					break;
				}

			}

		} catch (Exception e) {
			DbPlugin.getDefault().showErrorDialog(e);

		} finally {
			// SQLキーワードの登録
			SQLProposalCreator2.addProposal(proposals, modifiers, pinfo);
		}

	}

}
