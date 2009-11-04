/*
 * 著作権: Copyright (c) 2007−2008 ZIGEN
 * ライセンス：Eclipse Public License - v 1.0
 * 原文：http://www.eclipse.org/legal/epl-v10.html
 */

package zigen.plugin.db.ui.editors;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import kry.sql.tokenizer.SqlTokenizer;
import kry.sql.tokenizer.Token;
import kry.sql.tokenizer.TokenUtil;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.MultiPageEditorPart;

import zigen.plugin.db.DbPlugin;
import zigen.plugin.db.DbPluginConstant;
import zigen.plugin.db.IStatusChangeListener;
import zigen.plugin.db.ImageCacher;
import zigen.plugin.db.core.Condition;
import zigen.plugin.db.core.ConditionManager;
import zigen.plugin.db.core.ConnectionManager;
import zigen.plugin.db.core.IDBConfig;
import zigen.plugin.db.core.SQLFormatter;
import zigen.plugin.db.core.TableElement;
import zigen.plugin.db.core.TableManager;
import zigen.plugin.db.core.TimeWatcher;
import zigen.plugin.db.core.Transaction;
import zigen.plugin.db.core.rule.AbstractSQLCreatorFactory;
import zigen.plugin.db.core.rule.ISQLCreatorFactory;
import zigen.plugin.db.preference.PreferencePage;
import zigen.plugin.db.preference.SQLEditorPreferencePage;
import zigen.plugin.db.ui.actions.CopyRecordDataAction;
import zigen.plugin.db.ui.actions.DeleteRecordAction;
import zigen.plugin.db.ui.actions.GlobalAction;
import zigen.plugin.db.ui.actions.ITableViewEditorAction;
import zigen.plugin.db.ui.actions.InsertRecordAction;
import zigen.plugin.db.ui.actions.MaxRecordException;
import zigen.plugin.db.ui.actions.PasteRecordDataAction;
import zigen.plugin.db.ui.actions.SelectAllRecordAction;
import zigen.plugin.db.ui.editors.event.TableDefaultSortListener;
import zigen.plugin.db.ui.editors.event.TableKeyAdapter;
import zigen.plugin.db.ui.editors.event.TableKeyEventHandler;
import zigen.plugin.db.ui.editors.event.TableSortListener;
import zigen.plugin.db.ui.editors.exceptions.NotFoundColumnInfoException;
import zigen.plugin.db.ui.editors.exceptions.NotFoundSynonymInfoException;
import zigen.plugin.db.ui.editors.internal.CellEditorType;
import zigen.plugin.db.ui.editors.internal.ColumnFilterInfo;
import zigen.plugin.db.ui.editors.internal.FileCellEditor;
import zigen.plugin.db.ui.editors.internal.thread.AbstractSQLThread;
import zigen.plugin.db.ui.editors.internal.thread.ModifyTableThread;
import zigen.plugin.db.ui.internal.Column;
import zigen.plugin.db.ui.internal.ITable;
import zigen.plugin.db.ui.jobs.ChangeColorJob;
import zigen.plugin.db.ui.jobs.RecordCountForTableJob;
import zigen.plugin.db.ui.jobs.RecordSearchJob;
import zigen.plugin.db.ui.jobs.RefreshColumnJob;
import zigen.plugin.db.ui.jobs.RefreshTreeNodeAction;
import zigen.plugin.db.ui.jobs.TableFilterJob;
import zigen.plugin.db.ui.util.LineNumberRulerColumnUtil;
import zigen.plugin.db.ui.util.StyledTextUtil;
import zigen.plugin.db.ui.views.StatusLineContributionItem;
import zigen.plugin.db.ui.views.TreeView;
import zigen.plugin.db.ui.views.internal.ColorManager;
import zigen.plugin.db.ui.views.internal.DDLToolBar;
import zigen.plugin.db.ui.views.internal.SQLCharacterPairMatcher;
import zigen.plugin.db.ui.views.internal.SQLCodeConfiguration;
import zigen.plugin.db.ui.views.internal.SQLDocument;
import zigen.plugin.db.ui.views.internal.SQLSourceViewer;

/**
 * TableEditorクラス.
 *
 * @author ZIGEN
 * @version 1.0
 * @since JDK1.4 history Symbol Date Person Note [1] 2005/03/26 ZIGEN create.
 */
public class TableViewEditorFor31 extends MultiPageEditorPart implements ITableViewEditor, IPropertyChangeListener, ISelectionListener, IStatusChangeListener, IPageChangeListener {

	private boolean isEditing = false;

	private ConditionManager condiitonMgr = DbPlugin.getDefault().getConditionManager();

	private IDBConfig config;

	private SourceViewer logViewer;

	private TableViewer viewer;

	protected SQLSourceViewer ddlViewer; // DDL View

	protected SQLCodeConfiguration sqlConfiguration;

	protected ColorManager colorManager = new ColorManager();

	protected ImageCacher ic = ImageCacher.getInstance();

	private Table table;

	protected ITable tableNode;

	private CellEditor[] cellEditors;

	private Label infoLabel;

	private TableElement[] elements;

	private int maxSize = 10;

	private List conditionHistory = new ArrayList();

	protected Combo conditionComb;

	private static final int CONDITION_SHOW_COLS = 20; // 一度に表示できる項目数を20とする

	private TableKeyEventHandler handler;

	protected InsertRecordAction insertRecordAction;

	protected SelectAllRecordAction selectAllRecordAction;

	protected DeleteRecordAction deleteAction;

	protected CopyRecordDataAction copyAction;

	protected PasteRecordDataAction pasteAction;

	protected ChangeColorJob changeColorJob;

	private TableSortListener sortListener;

	protected ColumnFilterInfo[] filterInfos;

	protected StatusLineContributionItem responseTimeItem;

	protected String responseTime;

	protected ISelection selection;

	protected MatchingCharacterPainter painter;

	protected LineNumberRulerColumn rulerCol;

	protected ToolItem searchItem;

	protected ToolItem filterItem;

	protected ToolItem pingColumnItem;

	private boolean isLockedColumnWidth;

	protected IPreferenceStore ps;

	protected Condition fCondition;

	protected int offset = 1;

	protected int limit = 0;

	protected long totalCount;

	protected Label totalPage;

	protected DDLToolBar toolBar;

	protected TableViewerPager pager;

	protected ToolItem addToolItem;

	protected ToolItem deleteToolItem;

	protected CoolItem pagerItem;

	protected CoolItem infoLabelItem;


	/**
	 * Where条件を入力して検索した場合に格納されるようにする
	 */
	protected String whereString;

	public TableViewEditorFor31() {
		super();
		ps = DbPlugin.getDefault().getPreferenceStore();
		isLockedColumnWidth = ps.getBoolean(PreferencePage.P_LOCKE_COLUMN_WIDTH); // 初期値
	}

	private void makeActions() {
		insertRecordAction = new InsertRecordAction();
		selectAllRecordAction = new SelectAllRecordAction();
		deleteAction = new DeleteRecordAction();
		copyAction = new CopyRecordDataAction();
		pasteAction = new PasteRecordDataAction();
		insertRecordAction.setActiveEditor(this);
		selectAllRecordAction.setActiveEditor(this);
		deleteAction.setActiveEditor(this);
		copyAction.setActiveEditor(this);
		pasteAction.setActiveEditor(this);

	}

	private void saveConditionHistory() {
		String schemaName = tableNode.getSchemaName();
		String tableName = tableNode.getName();
		Condition condition = new Condition();
		String url = config.getUrl();
		condition.setConnectionUrl(url);
		condition.setSchema(schemaName);
		condition.setTable(tableName);
		condition.setConditions(conditionHistory);
		condition.setFilterPattern(filterPattern);
		condition.setCheckFilterPattern(false); // Filterチェックは起動時にはOFFにするため、常にFalseを設定
		condiitonMgr.setCondition(condition);
	}

	private void loadConditionHistory() {
		String url = config.getUrl();
		String schemaName = tableNode.getSchemaName();
		String tableName = tableNode.getName();
		fCondition = condiitonMgr.getCondition(url, schemaName, tableName);
		if (fCondition != null) {
			conditionHistory = fCondition.getConditions();
			this.checkFilterPattern = fCondition.isCheckFilterPattern();
			this.filterPattern = fCondition.getFilterPattern();
		}
	}

	protected void createPages() {
		makeActions();
		Composite composite = new Composite(getContainer(), SWT.NONE);
		composite.setLayout(new FillLayout());
		int index = addPage(composite);
		setPageText(index, "Dummy"); //$NON-NLS-1$
		// createDefinPage();
		// createDDLPage();
		// createMainPage();
		loadConditionHistory();
		DbPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		DbPlugin.addStatusChangeListener(this);

	}

	private void createDDLPage() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);

		toolBar = new DDLToolBar(composite, this);
		Composite sqlComposite = new Composite(composite, SWT.NONE);
		sqlComposite.setLayout(new FillLayout());

		FormData data = new FormData();
		data.top = new FormAttachment(toolBar.getCoolBar(), 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		sqlComposite.setLayoutData(data);

		CompositeRuler ruler = new CompositeRuler();
		rulerCol = new LineNumberRulerColumn();
		ruler.addDecorator(0, rulerCol);

		LineNumberRulerColumnUtil.changeColor(colorManager, rulerCol);
		ddlViewer = new SQLSourceViewer(sqlComposite, ruler, null, false, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		// sqlファイルに保存時の初期値を設定しておく
		ddlViewer.setSqlFileName(tableNode.getName());
		toolBar.setSQLSourceViewer(ddlViewer);
		initializeViewerFont(ddlViewer);
		sqlConfiguration = new SQLCodeConfiguration(colorManager);
		ddlViewer.configure(sqlConfiguration);
		ddlViewer.setDocument(new SQLDocument());
		ITextViewerExtension2 extension = (ITextViewerExtension2) ddlViewer;
		painter = new MatchingCharacterPainter(ddlViewer, new SQLCharacterPairMatcher());
		painter.setColor(colorManager.getColor(SQLEditorPreferencePage.P_COLOR_MATCHING));
		extension.addPainter(painter);
		StyledTextUtil.changeColor(colorManager, ddlViewer.getTextWidget());
		ddlViewer.setEditable(false);
		int index = addPage(composite);
		setPageText(index, "DDL"); //$NON-NLS-1$
	}

	private void createMessageArea(Composite parent) {

		CoolBar coolBar1 = new CoolBar(parent, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		coolBar1.setLayoutData(gridData);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		coolBar1.setLayout(gridLayout);
		pagerItem = new CoolItem(coolBar1, SWT.FLAT);
		pager = new TableViewerPager(tableNode, limit);
		pagerItem.setControl(pager.createStackedButtons(coolBar1));
		computeSize(pagerItem);

		pager.setPageNo(1);
		pager.addPageChangeListener(this);

		infoLabelItem = new CoolItem(coolBar1, SWT.NONE);
		infoLabel = new Label(coolBar1, SWT.NONE);
		infoLabel.setText(""); //$NON-NLS-1$
		infoLabel.setForeground(new Color(null, 255, 0, 0));
		// infoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		infoLabelItem.setControl(infoLabel);

		computeSize(infoLabelItem);

	}

	private void createMainPage() {
		Composite main = new Composite(getContainer(), SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.makeColumnsEqualWidth = false;
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 2;
		gridLayout.horizontalSpacing = 2;
		gridLayout.verticalSpacing = 2;
		main.setLayout(gridLayout);
		// ツールバー領域用
		createToolBar(main);

		// データ表示エリア
		table = new Table(main, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

		// Pagerおよびメッセージ領域
		createMessageArea(main);

		GridData gridData2 = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gridData2);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(DbPlugin.getDefaultFont());
		viewer = new TableViewer(table);
		handler = new TableKeyEventHandler(this);
		setHeaderColumn(table);// テーブルヘッダの設定
		// setCellModify(viewer, filterInfos, handler);// セルモディファイの設定
		viewer.setContentProvider(new TableViewContentProvider());
		viewer.setLabelProvider(new TableViewLabelProvider());

		table.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F2) {
					// レコード選択を解除し、先頭のカラムを編集状態にする
					int row = handler.getSelectedRow();
					handler.editTableElement(row, 1);

				}
			}
		});
		table.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {
				if (table.getSelectionIndex() == -1) {
					table.select(0); // 未選択の場合は、強制的に1レコード目を選択
					table.notifyListeners(SWT.Selection, null); // 選択状態を通知

				}
				// テーブル操作用のActionBarにする
				IActionBars bars = getEditorSite().getActionBars();
				setGlobalActionForEditor(bars);
				bars.updateActionBars();

			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent e) {
				selectionChangeHandler(e);
			}

		});

		viewer.setInput(elements);
		columnsPack(table);

		// hookContextMenu();
		contributeToStatusLine();

		addPage(0, main); // 先頭に挿入
		removePage(1);
		setActivePage(0); // 先頭をアクティブ
		getSite().setSelectionProvider(viewer);
		getEditorSite().getPage().addSelectionListener(this);
		setKeyBinding();

		changeColorJob = new ChangeColorJob(table, tableNode);
		changeColorJob.setPriority(ChangeColorJob.LONG);
		changeColorJob.setUser(false);
		changeColorJob.schedule();

	}

	// for Eclipse3.2 Keybinding(新規レコード作成）
	protected void setKeyBinding() {
		;
	}

	private boolean hasContributionItem(IStatusLineManager manager, String id) {
		IContributionItem[] items = manager.getItems();
		for (int i = 0; i < items.length; i++) {
			IContributionItem item = items[i];
			if (item.getId().equals(id)) {
				responseTimeItem = (StatusLineContributionItem) item;
				return true;
			}
		}
		return false;
	}

	public void contributeToStatusLine() {
		IStatusLineManager manager = getIStatusLineManager();
		if (!hasContributionItem(manager, "RecordCount")) { //$NON-NLS-1$
			responseTimeItem = new StatusLineContributionItem("RecordCount"); //$NON-NLS-1$
			manager.add(responseTimeItem);
		}

	}


	// for Eclipse3.1
	protected void conditionEventHandler(KeyEvent e) {
		if (e.character == SWT.CR) {
			e.doit = true;
			whereString = conditionComb.getText();

			pager.setPageNo(1); // Where条件で検索すると必ず1ページに移動する
			offset = 1; // offsetを初期化
			limit = DbPlugin.getDefault().getPreferenceStore().getInt(PreferencePage.P_MAX_VIEW_RECORD); // 制限を初期化
			updateTableViewer(whereString, offset, limit);
		}
	}

	public void pageChanged(int status, int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
		this.whereString = conditionComb.getText();
		updateTableViewer(whereString, offset, limit);

	}


	public void setTotalCount(int dispCount, long totalCount) {
		this.totalCount = totalCount;

		NumberFormat format = NumberFormat.getInstance();
		String displayCount = format.format(dispCount);
		String displayTotalCount = format.format(totalCount);

		StringBuffer sb = new StringBuffer();
		sb.append("["); //$NON-NLS-1$
		sb.append(config.getDbName());
		sb.append("] "); //$NON-NLS-1$
		sb.append(displayCount);
		sb.append(""); //$NON-NLS-1$
		if (!"".equals(displayTotalCount)) { //$NON-NLS-1$
			if ("-1".equals(displayTotalCount)) { //$NON-NLS-1$
				; // 未対応の場合は何もしない
			} else {
				sb.append(" / "); //$NON-NLS-1$
				sb.append(""); //$NON-NLS-1$
				sb.append(displayTotalCount);
				sb.append(Messages.getString("TableViewEditorFor31.12")); //$NON-NLS-1$
			}
		} else {
			sb.append(Messages.getString("TableViewEditorFor31.13")); //$NON-NLS-1$
		}
		sb.append(Messages.getString("TableViewEditorFor31.14")); //$NON-NLS-1$
		sb.append(responseTime);
		sb.append("]"); //$NON-NLS-1$

		setPageText(0, sb.toString());

		pager.setLimit(limit);
		pager.setRecordCount((int) totalCount);
		computeSize(pagerItem);

	}

	public void setResponseTime(String responseTime) {
		this.responseTime = responseTime;
		if (responseTimeItem != null && responseTime != null && !"".equals(responseTime)) { //$NON-NLS-1$
			StringBuffer sb = new StringBuffer();
			sb.append(Messages.getString("TableViewEditorFor31.17")); //$NON-NLS-1$
			sb.append(responseTime);
			responseTimeItem.setText(sb.toString());

		}
	}

	public void refleshAction() {
		deleteAction.refresh();
		copyAction.refresh();
		pasteAction.refresh();

		// ツールバーの制御
		if (table.getSelectionCount() > 0) {
			deleteToolItem.setEnabled(true);
		} else {
			deleteToolItem.setEnabled(false);
		}

	}

	void selectionChangeHandler(SelectionChangedEvent event) {
		refleshAction();
	}


	private void createToolBar(Composite parent) {
		GridData gridData;

		CoolBar coolBar = new CoolBar(parent, SWT.FLAT);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		coolBar.setLayoutData(gridData);
		final ToolBar toolBar1 = new ToolBar(coolBar, SWT.HORIZONTAL);
		// final ToolItem openToolItem = new ToolItem(toolBar1, SWT.PUSH);
		// openToolItem.setImage(getImage(ISharedImages.IMG_TOOL_COPY));
		// openToolItem.setToolTipText("Copy");
		//
		// final ToolItem saveToolItem = new ToolItem(toolBar1, SWT.PUSH);
		// saveToolItem.setImage(getImage(ISharedImages.IMG_TOOL_PASTE));
		// // saveToolItem.setText("Paste");
		// saveToolItem.setToolTipText("Paste");

		addToolItem = new ToolItem(toolBar1, SWT.PUSH);
		addToolItem.setImage(ic.getImage(DbPlugin.IMG_CODE_ADD));
		addToolItem.setToolTipText(Messages.getString("TableViewEditorFor31.9")); //$NON-NLS-1$
		addToolItem.setEnabled(false);
		addToolItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				InsertRecordAction action = getContributor().getInsertRecordAction();
				action.run();
			}
		});

		deleteToolItem = new ToolItem(toolBar1, SWT.PUSH);
		deleteToolItem.setImage(getImage(ISharedImages.IMG_TOOL_DELETE));
		deleteToolItem.setToolTipText(Messages.getString("TableViewEditorFor31.8")); //$NON-NLS-1$
		deleteToolItem.setEnabled(false);
		computeSize(coolBar, toolBar1);
		deleteToolItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				DeleteRecordAction action = getContributor().getDeleteRecordAction();
				action.run();
			}
		});

		final ToolBar toolBar3 = new ToolBar(coolBar, SWT.HORIZONTAL);

		searchItem = new ToolItem(toolBar3, SWT.NONE);
		searchItem.setImage(ic.getImage(DbPlugin.IMG_CODE_EXECUTE));
		searchItem.setEnabled(false); // カラム情報が読み終わるまでFalse
		searchItem.setToolTipText(Messages.getString("TableViewEditorFor31.19")); //$NON-NLS-1$
		searchItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				whereString = conditionComb.getText();
				pager.setPageNo(1);
				offset = 1; // 先頭に初期化する
				limit = DbPlugin.getDefault().getPreferenceStore().getInt(PreferencePage.P_MAX_VIEW_RECORD);
				updateTableViewer(whereString, offset, limit);
			}
		});

		computeSize(coolBar, toolBar3);

		final ToolBar toolBar2 = new ToolBar(coolBar, SWT.HORIZONTAL);
		filterItem = new ToolItem(toolBar2, SWT.NONE);
		filterItem.setImage(ic.getImage(DbPlugin.IMG_CODE_FILTER));
		filterItem.setEnabled(false); // カラム情報が読み終わるまでFalse
		filterItem.setToolTipText(Messages.getString("TableViewEditorFor31.20")); //$NON-NLS-1$
		filterItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				filterBtnSelectHandler(e);
			}
		});

		pingColumnItem = new ToolItem(toolBar2, SWT.CHECK);
		pingColumnItem.setImage(ic.getImage(DbPlugin.IMG_CODE_PIN_COLUMN));
		pingColumnItem.setSelection(isLockedColumnWidth);
		pingColumnItem.setEnabled(false); // カラム情報が読み終わるまでFalse
		pingColumnItem.setToolTipText(Messages.getString("TableViewEditorFor31.29")); //$NON-NLS-1$
		pingColumnItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				isLockedColumnWidth = !isLockedColumnWidth;
			}
		});

		computeSize(coolBar, toolBar2);

		// CSVエクスポート部分
		// ToolBar cToolBar5 = new ToolBar(coolBar, SWT.FLAT);
		// ToolItem exportCsvItem = new ToolItem(cToolBar5, SWT.NONE);
		// exportCsvItem.setImage(ic.getImage(DbPlugin.IMG_CODE_EXPORT));
		// exportCsvItem.setEnabled(false);
		// computeSize(coolBar, cToolBar5);
		// exportCsvItem.addSelectionListener(new SelectionAdapter() {
		// public void widgetSelected(SelectionEvent e) {
		// DeleteRecordAction action = getContributor().getDeleteRecordAction();
		// action.run();
		// }
		// });

		final Composite tool = new Composite(parent, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		tool.setLayoutData(gridData);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.makeColumnsEqualWidth = false;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 0;
		tool.setLayout(gridLayout);

		// where条件
		Label label1 = new Label(tool, SWT.NULL);
		gridData = new GridData(GridData.FILL);
		gridData.verticalIndent = 2;
		label1.setLayoutData(gridData);
		label1.setText(Messages.getString("TableViewEditorFor31.18")); //$NON-NLS-1$

		conditionComb = new Combo(tool, SWT.NONE);
		conditionComb.setEnabled(false);
		if (whereString != null && !"".equals(whereString)) { //$NON-NLS-1$
			conditionComb.setText(whereString);
		}

		conditionComb.setVisibleItemCount(CONDITION_SHOW_COLS);
		conditionComb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		conditionComb.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent e) {
				conditionEventHandler(e);
			}
		});

		// // ------------------------------------------
		// // コードアシスト only Eclipse 3.2
		// // ------------------------------------------
		// AddContentAssist();

		conditionComb.addTraverseListener(new TraverseListener() {

			public void keyTraversed(TraverseEvent e) {
				if (e.character == SWT.TAB) {
					if (table.getItemCount() == 0) {
						// 検索結果が0の場合
						handler.createNewRecord();
					}

				}
			}
		});
		conditionComb.setFont(DbPlugin.getDefaultFont());
		conditionComb.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {
				IActionBars bars = getEditorSite().getActionBars();
				bars.clearGlobalActionHandlers();
				bars.updateActionBars();
			}

		});
		conditionComb.setFocus();
		if (conditionHistory != null) {
			if (conditionHistory.size() == 0) {
				conditionComb.add(""); //$NON-NLS-1$
			} else {
				for (int i = 0; i < conditionHistory.size(); i++) {
					conditionComb.add((String) conditionHistory.get(i));
				}
			}
		}

	}

	protected Image getImage(String key) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(key);
	}

	private void computeSize(CoolBar coolBar, ToolBar cToolBar) {
		CoolItem item = new CoolItem(coolBar, SWT.PUSH);
		cToolBar.pack();
		Point size = cToolBar.getSize();
		item.setControl(cToolBar);
		item.setSize(item.computeSize(size.x + 5, size.y));

	}

	private void computeSize(CoolItem item) {
		Control control = item.getControl();
		Point pt = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		pt = item.computeSize(pt.x + 5, pt.y);
		item.setSize(pt);
	}

	boolean checkFilterPattern = false;

	String filterPattern = null;

	private void filterBtnSelectHandler(SelectionEvent event) {
		Shell shell = this.getSite().getShell();
		ColumnFilterDialog dialog = new ColumnFilterDialog(shell, this);
		int ret = dialog.open();
		if (ret == IDialogConstants.OK_ID) {
			checkFilterPattern = dialog.isFilterPattern;
			filterPattern = dialog.filterPattern;
		}
	}

	public void removeOverHistory() {
		while (conditionHistory.size() > maxSize) { // 空白用を考慮
			int i = conditionHistory.size() - 1;
			conditionHistory.remove(i);
			conditionComb.remove(i);
		}
	}

	void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				switch (getActivePage()) {
				case SHEET_INFO:
					if (tableDefineEditor.focusDefine()) {
						ISelection selection = tableDefineEditor.getDefineViewer().getSelection();
						getContributor().fillContextMenuForDefine(manager, selection);
					} else {
						ISelection selection = tableDefineEditor.getConstraintViewer().getSelection();
						getContributor().fillContextMenuForConstraints(manager, selection);
					}
					break;
				case SHEET_DDL:
					getContributor().fillContextMenuForDDL(manager);
					break;
				case SHEET_DATA:
					getContributor().fillContextMenu(manager);

					// 拡張ポイントの追加
					setExtensionPoint(manager);

					break;
				default:
					break;
				}

			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);

		if (tableDefineEditor != null) {
			Menu menu2 = menuMgr.createContextMenu(tableDefineEditor.getDefineViewer().getControl());
			tableDefineEditor.getDefineViewer().getControl().setMenu(menu2);
			getSite().registerContextMenu(menuMgr, tableDefineEditor.getDefineViewer());
			Menu menu3 = menuMgr.createContextMenu(tableDefineEditor.getConstraintViewer().getControl());
			tableDefineEditor.getConstraintViewer().getControl().setMenu(menu3);
			getSite().registerContextMenu(menuMgr, tableDefineEditor.getConstraintViewer());

		}
		if (ddlViewer != null) {
			Menu menu4 = menuMgr.createContextMenu(ddlViewer.getTextWidget());
			ddlViewer.getTextWidget().setMenu(menu4);
			getSite().registerContextMenu(menuMgr, ddlViewer);
		}
	}

	private TableViewerContributor getContributor() {
		IEditorActionBarContributor contributor = getEditorSite().getActionBarContributor();
		if (contributor instanceof TableViewerContributor) {
			return (TableViewerContributor) contributor;
		} else {
			return null;
		}
	}

	private void setHeaderColumn(Table table) {
		if (elements != null) {
			// TableColumn row = new TableColumn(table, SWT.LEFT);
			TableColumn row = new TableColumn(table, SWT.RIGHT);

			sortListener = new TableSortListener(this, 0);
			row.addSelectionListener(sortListener);
			row.pack();
			TableElement element = elements[0]; // ヘッダー用カラム
			zigen.plugin.db.core.TableColumn[] columns = element.getColumns();
			for (int i = 0; i < columns.length; i++) {
				zigen.plugin.db.core.TableColumn tColumn = columns[i];
				TableColumn col = new TableColumn(table, SWT.LEFT);
				col.setText(tColumn.getColumnName());
				col.addSelectionListener(new TableSortListener(this, i + 1));
				col.pack();
			}
		}
	}

	private void setCellModify(TableViewer viewer, ColumnFilterInfo[] filterInfo, TableKeyEventHandler handler) {
		if (elements == null)
			return;
		final IActionBars bars = getEditorSite().getActionBars();
		TableElement element = elements[0];// ヘッダー用TableElementの取得
		int size = element.getColumns().length + 1; // ROW用に追加
		String[] properties = new String[size];
		zigen.plugin.db.core.TableColumn[] cols = element.getColumns();
		cellEditors = new CellEditor[size];
		TableKeyAdapter keyAdapter = new TableKeyAdapter(handler);
		for (int i = 0; i < cellEditors.length; i++) {
			properties[i] = String.valueOf(i); // property としてIndex番号を渡す
			if (i > 0) { // 1カラム目以降を更新可能とする
				CellEditor cellEditor = null;
				if (CellEditorType.isFileSaveType(cols[i - 1])) {
					cellEditor = new FileCellEditor(table);
					((FileCellEditor) cellEditor).addKeyListener(keyAdapter);
					((FileCellEditor) cellEditor).addTraverseListener(keyAdapter);
				} else {
					cellEditor = new TextCellEditor(table, i);
					cellEditor.getControl().addKeyListener(keyAdapter);
					cellEditor.getControl().addTraverseListener(keyAdapter);

				}

				// cellEditor.getControl().addFocusListener(new
				// CellFocusAdapter(element));
				cellEditor.getControl().addFocusListener(new FocusAdapter() {

					public void focusGained(FocusEvent e) {
						setInfomationText(EDIT_MODE_ON);
						isEditing = true; // 編集中フラグをON
						bars.clearGlobalActionHandlers();
						bars.updateActionBars();
					}

					public void focusLost(FocusEvent e) {
						setInfomationText(EDIT_MODE_OFF);
						isEditing = false; // 編集中フラグをOFF
					}
				});

				// Validatorを使うと、入力エラーの場合、入力した値が消えるため、以下の処理は行なわない
				// cellEditor.setValidator(new CellEditorValidator(config,
				// viewer, i));
				cellEditors[i] = cellEditor;

			}
		}
		viewer.setColumnProperties(properties);
		// viewer.setCellModifier(new CellModifier(viewer, tableNode,
		// filterInfo));
		viewer.setCellModifier(new CellModifier(this, filterInfo, handler));

		viewer.setCellEditors(cellEditors);
	}

	// TODO:設定可能にしたい
	private int max_column_size = 600;

	private void columnsPack(Table table) {
		table.setVisible(false);
		TableColumn[] cols = table.getColumns();
		for (int i = 0; i < cols.length; i++) {
			TableColumn c = cols[i];
			c.pack();

			// 自動幅調整後長すぎるサイズを強制修正
			if(c.getWidth() > max_column_size){
				c.setWidth(max_column_size);
			}
		}
		table.setVisible(true);
	}

	public void dispose() {
		DbPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		getEditorSite().getPage().removeSelectionListener(this);
		DbPlugin.removeStatusChangeListener(this);
		insertRecordAction.setActiveEditor(null);
		selectAllRecordAction.setActiveEditor(null);
		deleteAction.setActiveEditor(null);
		copyAction.setActiveEditor(null);
		pasteAction.setActiveEditor(null);
		if (handler != null) {
			handler.dispose();
			handler = null;
		}
		saveConditionHistory();
		super.dispose();
	}

	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		super.init(site, editorInput);
		try {
			if (editorInput instanceof TableViewEditorInput) {
				TableViewEditorInput input = (TableViewEditorInput) editorInput;
				this.config = input.getConfig();
				this.tableNode = input.getTable();
				String partName = this.tableNode.getSqlTableName();
				setPartName(partName);

				// input.setToolTipText("[" + input.getConfig().getDbName() + "]
				// " + partName); //$NON-NLS-1$ //$NON-NLS-2$

				StringBuffer sb = new StringBuffer();
				sb.append("["); //$NON-NLS-1$
				sb.append(input.getConfig().getDbName());
				sb.append("] "); //$NON-NLS-1$
				sb.append(partName);
				if (tableNode.getRemarks() != null && !"".equals(tableNode.getRemarks().trim())) { //$NON-NLS-1$
					sb.append(" ["); //$NON-NLS-1$
					sb.append(tableNode.getRemarks());
					sb.append("]"); //$NON-NLS-1$
				}
				input.setToolTipText(sb.toString());

				setStatusMessage(input.getToolTipText());

			}

		} catch (Exception e) {
			DbPlugin.getDefault().showErrorDialog(e);
		}
	}

	// Where条件入力部からの実行
	protected void updateTableViewer(String condition, int offset, int limit) {

		// limit=0 の場合は、無制限の検索となるため、その場合offset=0に初期化する
		if (limit == 0) {
			offset = 0;
		}

		// ORDER BYオプションをつけるかどうか
		RecordSearchJob job = new RecordSearchJob(this, condition, getOrderByString(), offset, limit);
		job.setPriority(RecordSearchJob.SHORT);
		job.setUser(true);
		job.schedule();
	}

	// JOBクラスから呼ばれるメソッド
	public void updateTableViewer(String condition, TableElement[] elements, String responseTime, boolean doCalculate) {
		try {
			viewer.setInput(elements);

			TableColumn col = viewer.getTable().getColumn(0);
			col.pack();// 先頭の幅をパック
			TableDefaultSortListener defaultSortListener = new TableDefaultSortListener(this, 0);
			// sortListenerを一旦削除
			col.removeSelectionListener(sortListener);
			// 初期表示用のSortListenerを登録
			col.addSelectionListener(defaultSortListener);
			viewer.getTable().getColumn(0).notifyListeners(SWT.Selection, null);
			// 元のリスナーに戻す
			col.removeSelectionListener(defaultSortListener);
			sortListener = new TableSortListener(this, 0);
			col.addSelectionListener(sortListener);

			// カラム幅を固定にしない場合は、再度カラムパックを呼ぶ
			if (!isLockedColumnWidth) {
				columnsPack(table);
				// Filter実行
				TableFilterJob job = new TableFilterJob(viewer, filterInfos);
				job.setPriority(TableFilterJob.SHORT);
				job.setUser(false); // ダイアログを出す
				job.schedule();

			}

			// unformatすることで、重複を除く。
			String unformated = SQLFormatter.unformat(condition);
			// 一致した条件を一度削除し、最上位に移動させる
			if (conditionHistory.contains(unformated)) {
				conditionHistory.remove(unformated);
				conditionComb.remove(unformated);
			}
			conditionHistory.add(0, unformated); // 先頭に追加
			conditionComb.add(unformated, 0); // 先頭に追加

			conditionComb.select(0);// 先頭を選択する
			// 最大値を超えた分を削除する
			removeOverHistory();

			// 応答時間
			setResponseTime(responseTime);

			// レコード件数の表示
			int dispCnt = elements.length - 1;
			setTotalCount(dispCnt, -1); //$NON-NLS-1$

			// レコード件数の計算
			RecordCountForTableJob job2 = new RecordCountForTableJob(Transaction.getInstance(config), tableNode, condition, dispCnt, doCalculate);
			job2.setUser(false);
			job2.schedule();

		} catch (Exception e) {
			DbPlugin.log(e);
		}
	}

	private Object lock = new Object();

	public void createResultPage(IDBConfig config, ITable iTable, boolean isSearch) throws NotFoundColumnInfoException {

		synchronized (lock) {
			// EditorInputにあるTableを再設定する
			TableViewEditorInput input = new TableViewEditorInput(config, iTable);
			this.setInput(input);
			this.config = config; // DBConfigを新しく設定すること
			// 開いている場合は、再検索しないようにする
			if (viewer == null || isSearch) {
				String maxRecordMessage = ""; //$NON-NLS-1$
				TimeWatcher time = new TimeWatcher();
				time.start();

				boolean doCalculate = false;
				try {
					// ----------------------------------------------------------------------------------
					// この時点ではテーブルのカラム情報が無いため、offset, limitを指定した検索はできない
					// ----------------------------------------------------------------------------------
					elements = TableManager.invoke(config, iTable);

				} catch (MaxRecordException e) {
					elements = e.getTableElements();
					doCalculate = true;
					maxRecordMessage = e.getMessage();
				} catch (NotFoundColumnInfoException e) {
					throw e;
				} catch (SQLException e) {
					DbPlugin.getDefault().showWarningMessage(e.getMessage());
				} catch (Exception e) {
					DbPlugin.getDefault().showErrorDialog(e);
				} finally {
					time.stop();
				}

				// -----------------------------------------------
				// 最初の検索時に、最大表示件数をEditorに設定する
				// -----------------------------------------------
				limit = DbPlugin.getDefault().getPreferenceStore().getInt(PreferencePage.P_MAX_VIEW_RECORD);

				// データ表示部の作成(事前にlimitに値を入れておく)
				createMainPage();

				// メッセージを設定
				setInfomationText(maxRecordMessage);

				setResponseTime(time.getTotalTime());// 応答時間

				if (elements != null) {
					// カラム情報の読み込み
					TreeView view = (TreeView) DbPlugin.findView(DbPluginConstant.VIEW_ID_TreeView);
					LoadingColumnInfoJob job3 = new LoadingColumnInfoJob(view.getTreeViewer(), tableNode);
					job3.setPriority(LoadingColumnInfoJob.SHORT);
					job3.setUser(false);
					job3.schedule();

					// 表示件数の設定
					int dispCnt = elements.length - 1;
					setTotalCount(dispCnt, -1); //$NON-NLS-1$
					RecordCountForTableJob job2 = new RecordCountForTableJob(Transaction.getInstance(config), iTable, null, dispCnt, doCalculate);
					job2.setPriority(RecordCountForTableJob.LONG);
					job2.setUser(false);
					job2.schedule();

				}
			}

		}

	}

	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
	}

	public void editTableElement(Object element, int column) {
		viewer.cancelEditing();
		viewer.editElement(element, column);
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public void setInfomationText(String text) {
		infoLabel.setText(text);
		// infoLabel.pack(true);
		computeSize(infoLabelItem);

	}

	public TableElement getHeaderTableElement() {
		Object obj = viewer.getInput();
		if (obj instanceof TableElement[]) {
			TableElement[] elements = (TableElement[]) obj;
			if (elements.length > 0) {
				return elements[0];
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public ITable getTableNode() {
		return tableNode;
	}

	protected void initializeViewerFont(ISourceViewer viewer) {
		StyledText styledText = viewer.getTextWidget();
		styledText.setFont(DbPlugin.getDefaultFont());
	}

	public void setFocus() {
		setResponseTime(responseTime);
		// 追加機能(DBツリーとリンクさせる)
		DbPlugin.fireStatusChangeListener(selection, IStatusChangeListener.EVT_LinkTable);
		IActionBars bars = getEditorSite().getActionBars();
		bars.clearGlobalActionHandlers();

		switch (getActivePage()) {
		case SHEET_INFO:
			tableDefineEditor.setFocus();
			break;

		case SHEET_DDL:// DDLシート
			setDDLString();
			setGlobalActionForDDL(bars);
			break;

		case SHEET_DATA:// Editorシート
			setGlobalActionForEditor(bars);
			if (conditionComb != null)
				conditionComb.setFocus();
			break;

		default:
			break;
		}

		bars.updateActionBars();
		setStatusMessage(getEditorInput().getToolTipText());
	}

	void setDDLString() {
		try {
			ISQLCreatorFactory factory = AbstractSQLCreatorFactory.getFactory(config, tableNode);
			ddlViewer.getDocument().set(factory.createDDL()); // schema付きで作成
		} catch (Exception e) {
			DbPlugin.log(e);
		}
	}

	void setGlobalActionForDDL(IActionBars bars) {
		bars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), new GlobalAction(ddlViewer, ITextOperationTarget.SELECT_ALL));
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), new GlobalAction(ddlViewer, ITextOperationTarget.COPY));
		insertRecordAction.setEnabled(false); // ショートカット対象のActionを無効
	}

	void setGlobalActionForEditor(IActionBars bars) {
		bars.setGlobalActionHandler("zigen.plugin.db.actions.InsertRecordAction", insertRecordAction); //$NON-NLS-1$
		bars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllRecordAction);
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
		insertRecordAction.setEnabled(true); // ショートカット対象のActionを有効

	}

	public SourceViewer getLogViewer() {
		return logViewer;
	}

	protected void AddContentAssist() {
		; // Eclipse 3.1では実装不可
	}

	public IDBConfig getDBConfig() {
		return config;
	}

	private TableDefineEditor tableDefineEditor;

	private void createDefinPage() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		composite.setLayout(new FillLayout());
		tableDefineEditor = new TableDefineEditor(composite, tableNode);
		tableDefineEditor.setEditor(this);
		tableDefineEditor.createWidget();
		int index = addPage(composite);
		setPageText(index, Messages.getString("TableViewEditorFor31.28")); //$NON-NLS-1$

	}

	public void doSave(IProgressMonitor monitor) {
		try {
			String newTableName = tableDefineEditor.getTableName();
			String newRemarks = tableDefineEditor.getTableComment();
			AbstractSQLThread invoker = new ModifyTableThread(tableNode, newTableName, newRemarks);
			invoker.run();

			TableViewEditorInput input = (TableViewEditorInput) getEditorInput();
			String partName = this.tableNode.getSqlTableName();
			setPartName(partName);

			// input.setToolTipText("[" + input.getConfig().getDbName() + "] " +
			// partName); //$NON-NLS-1$ //$NON-NLS-2$

			StringBuffer sb = new StringBuffer();
			sb.append("["); //$NON-NLS-1$
			sb.append(input.getConfig().getDbName());
			sb.append("] "); //$NON-NLS-1$
			sb.append(partName);
			if (tableNode.getRemarks() != null && !"".equals(tableNode.getRemarks().trim())) { //$NON-NLS-1$
				sb.append(" ["); //$NON-NLS-1$
				sb.append(tableNode.getRemarks());
				sb.append("]"); //$NON-NLS-1$
			}
			input.setToolTipText(sb.toString());

			setStatusMessage(input.getToolTipText());
			// 変更通知(OFF)
			setDirty(false);

		} catch (Exception e) {
			DbPlugin.getDefault().showErrorDialog(e);
		}
	}

	boolean dirty;

	public void doSaveAs() {}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean value) {
		dirty = value;
		firePropertyChange(PROP_DIRTY);
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	public void changeColumnColor(Column column) {
		changeColorJob.setSelectedColumn(column);
		changeColorJob.schedule();

	}

	public void changeColumnColor() {
		changeColorJob.setTable(table);
		changeColorJob.schedule();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection _selection) {
		if (_selection != null && _selection instanceof StructuredSelection) {

			if (part instanceof TreeView) {
				TreeView tree = (TreeView) part;
				if (tree.isLinkingEnabled() && _selection.equals(this.selection)) {
					// // エディターリンク中ならば(設定ファイルから取ること)
					try {
						getSite().getPage().openEditor(getEditorInput(), DbPluginConstant.EDITOR_ID_TableEditor, false);
					} catch (PartInitException e) {
						// 以下のエラーはログ出力のみとする
						// DbPlugin.log(e);
					}
				}

				Object obj = ((StructuredSelection) _selection).getFirstElement();
				if (obj != null) {
					if (obj instanceof Column) {
						// テーブル編集エディターのカラムを選択する
						Column column = (Column) obj;
						if (column.getTable().equals(tableNode)) {
							changeColumnColor((Column) obj);
						}
					}
					if (obj instanceof ITable) {
						// テーブル編集エディターのカラム選択を解除する
						ITable t = (ITable) obj;
						if (t.equals(tableNode)) {
							changeColumnColor(null);
						}
					}

				}
			}

		}

	}

	public void statusChanged(Object obj, int status) {
		if (status == IStatusChangeListener.EVT_RefreshTable) {
			if (obj instanceof ITable) {
				ITable wk = (ITable) obj;
				if (wk.equals(tableNode)) {
					try {
						int cnt = 1;
						while (isLodingColumnInfo) {
							System.out.println("待ちが入っている？");
							Thread.sleep(500); // カラムの読み込み中は待ち
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int current = getActivePage();
					IDBConfig wkConfig = wk.getDbConfig();
					boolean isSearch = true; // 再検索する
					try {
						createResultPage(wkConfig, wk, isSearch);
					} catch (NotFoundColumnInfoException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
					setActivePage(current);
					// EditorPartの名前を変名する
					String partName = this.tableNode.getSqlTableName();
					setPartName(partName);
				}
			}
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (sqlConfiguration != null && ddlViewer != null) {
			StyledTextUtil.changeColor(colorManager, ddlViewer.getTextWidget());
			LineNumberRulerColumnUtil.changeColor(colorManager, rulerCol);
			sqlConfiguration.updatePreferences(ddlViewer.getDocument());
			painter.setColor(colorManager.getColor(SQLEditorPreferencePage.P_COLOR_MATCHING));
			ddlViewer.invalidateTextPresentation();// テキストエディタを再描画
			isLockedColumnWidth = ps.getBoolean(PreferencePage.P_LOCKE_COLUMN_WIDTH); // 初期値
			if (pingColumnItem != null)
				pingColumnItem.setSelection(isLockedColumnWidth);
		}
	}

	public TableDefineEditor getTableDefineEditor() {
		return tableDefineEditor;
	}

	public String getCondition() {
		return whereString;
	}

	public ColumnFilterInfo[] getFilterInfos() {
		return filterInfos;
	}

	// DB Tree ViewのSelection
	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

	protected IStatusLineManager getIStatusLineManager() {
		IEditorSite vieweSite = super.getEditorSite();
		IActionBars actionBars = vieweSite.getActionBars();
		return actionBars.getStatusLineManager();

	}

	public void setStatusMessage(String message) {
		getIStatusLineManager().setMessage(message);
	}

	public void setStatusErrorMessage(String message) {
		getIStatusLineManager().setErrorMessage(message);
	}

	private boolean hasOrderBy() {
		SqlTokenizer tokenizer = new SqlTokenizer(whereString, DbPlugin.getSqlFormatRult());

		for (Iterator it = tokenizer; it.hasNext();) {
			Token token = (Token) it.next();
			if (token.getType() == TokenUtil.TYPE_KEYWORD) {
				if ("order by".equalsIgnoreCase(token.getCustom())) { //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;
	}

	public String getOrderByString() {
		if (!hasOrderBy()) {
			TreeMap map = new TreeMap();
			for (int i = 0; i < filterInfos.length; i++) {
				int sortNum = filterInfos[i].getSortNo();
				if (sortNum > 0) {
					map.put(new Integer(sortNum), filterInfos[i]);
				}
			}
			StringBuffer sb = new StringBuffer();
			int cnt = 0;
			for (Iterator itr = map.keySet().iterator(); itr.hasNext();) {
				Integer key = (Integer) itr.next();
				ColumnFilterInfo info = (ColumnFilterInfo) map.get(key);
				if (cnt == 0) {
					sb.append("ORDER BY "); //$NON-NLS-1$
				} else {
					sb.append(", "); //$NON-NLS-1$
				}
				sb.append(info.getColumnName());
				if (info.isDesc()) {
					sb.append(" DESC"); //$NON-NLS-1$
				}
				cnt++;
			}
			return sb.toString();
		} else {
			return null;
		}

	}

	static Object loadingColumnInfolock = new Object();

	boolean isLodingColumnInfo = false;

	class LoadingColumnInfoJob extends RefreshColumnJob {

		public LoadingColumnInfoJob(TreeViewer treeViewer, ITable table) {
			super(treeViewer, table);
		}

		protected IStatus run(IProgressMonitor monitor) {
			Connection con = null;
			try {
				isLodingColumnInfo = true;
				IDBConfig config = table.getDbConfig();
				// Connection con =
				// Transaction.getInstance(config).getConnection();
				con = ConnectionManager.getConnection(config);
				monitor.beginTask(Messages.getString("TableViewEditorFor31.0"), 10); //$NON-NLS-1$

				synchronized (loadingColumnInfolock) {
					// カラム情報をロード
					if (!table.isExpanded()) {

						monitor.beginTask(Messages.getString("TableViewEditorFor31.34"), 6); //$NON-NLS-1$

						if (!super.loadColumnInfo(monitor, con, table)) {
							table.setExpanded(false);
							return Status.CANCEL_STATUS;
						} else {
							table.setExpanded(true);
							TableElement header = getHeaderTableElement();
							if (header != null) {
								// ヘッダー情報に初期値を設定する
								zigen.plugin.db.core.TableColumn[] columns = header.getColumns();
								if (columns.length == table.getChildrens().length) {
									Column[] col = table.getColumns();
									for (int i = 0; i < columns.length; i++) {
										zigen.plugin.db.core.TableColumn column = columns[i];
										if (column.getColumnName().equals(col[i].getName())) {
											column.setDefaultValue(col[i].getDefaultValue());
										}
									}
								} else {
									throw new IllegalStateException(Messages.getString("TableViewEditorFor31.30")); //$NON-NLS-1$
								}
							}

						}
					}
				}

				// フィルターボタンを有効にする
				showResults(new SetEnabledAction(table));
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				monitor.done();

			} catch (NotFoundColumnInfoException e) {
				table.removeChildAll(); // 子ノードを全て削除
				showResults(new RefreshTreeNodeAction(treeViewer, table)); // 再描画
				showErrorMessage(Messages.getString("TableViewEditorFor31.31"), e); //$NON-NLS-1$
			} catch (NotFoundSynonymInfoException e) {
				table.setEnabled(false);
				table.removeChildAll(); // 子ノードを全て削除
				showResults(new RefreshTreeNodeAction(treeViewer, table)); // 再描画
				showErrorMessage(Messages.getString("TableViewEditorFor31.32"), e); //$NON-NLS-1$

			} catch (Exception e) {
				showErrorMessage(Messages.getString("TableViewEditorFor31.33"), e); //$NON-NLS-1$

			} finally {
				// カラム読み込み中フラグを戻す
				isLodingColumnInfo = false;

				ConnectionManager.closeConnection(con);
			}

			return Status.OK_STATUS;
		}

		protected class SetEnabledAction implements Runnable {

			ITable table;

			public SetEnabledAction(ITable table) {
				this.table = table;
			}

			public void run() {
				try {
					IActionBars bars = getEditorSite().getActionBars();
					bars.clearGlobalActionHandlers();

					Column[] columns = table.getColumns();
					ColumnFilterInfo[] newfilterInfos = new ColumnFilterInfo[columns.length];
					for (int i = 0; i < columns.length; i++) {
						Column col = columns[i];

						newfilterInfos[i] = new ColumnFilterInfo(col);

						if (filterInfos != null) {
							// 設定済みのFilter情報を引き継ぐ
							for (int j = 0; j < filterInfos.length; j++) {
								if (columns[i].getColumn().getColumnName().equals(filterInfos[j].getColumnName())) {
									newfilterInfos[i].setChecked(filterInfos[j].isChecked());
									break;
								}
							}
						}
					}

					filterInfos = newfilterInfos;
					setCellModify(viewer, filterInfos, handler);// セルモディファイの設定
					setGlobalActionForEditor(bars);

					// ------------------------------------------
					// コードアシスト only Eclipse 3.2
					// ------------------------------------------
					AddContentAssist();

					if (!isExistDDLPage) {
						isExistDDLPage = true;
						createDDLPage();
					}
					if (!isExistDefinePage) {
						isExistDefinePage = true;
						createDefinPage();
					}

					tableDefineEditor.updateWidget();
					setDirty(false);// 変更完了通知

					// コンテキストメニュー作成
					hookContextMenu();

					// コントロールを有効化する
					setEnabled(true);

					conditionComb.setFocus();
				} catch (Exception e) {
					DbPlugin.log(e);
				}

			}

			private void setEnabled(boolean b) {
				conditionComb.setEnabled(b);
				searchItem.setEnabled(b);
				filterItem.setEnabled(b);
				pingColumnItem.setEnabled(b);
				getContributor().setEnabled(b);

				addToolItem.setEnabled(b);
				// deleteToolItem.setEnabled(b);

			}
		}
	}

	boolean isExistDDLPage = false;

	boolean isExistDefinePage = false;

	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
		table.setVisible(enabled);	// 貼り付け中はテーブルデータを非表示にすることで高速化

		conditionComb.setEnabled(enabled);
		searchItem.setEnabled(enabled);
		filterItem.setEnabled(enabled);
		pingColumnItem.setEnabled(enabled);
	}

	public int getRecordLimit() {
		return limit;
	}

	public int getRecordOffset() {
		return offset;
	}


	private List extensionList = new ArrayList();

	private void setExtensionPoint(IMenuManager manager) {

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		// 拡張ポイントを取得
		IExtensionPoint point = registry.getExtensionPoint(DbPlugin.getDefault().getBundle().getSymbolicName() + ".tableEditor");
		// コントリビュートされた拡張を取得
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			add(manager, elements);
		}

	}

	private void add(IMenuManager menu, IConfigurationElement[] elems) {

		try {
			for (int k = 0; k < elems.length; k++) {

				IConfigurationElement element = elems[k];
				String name = element.getName();

				if ("contributor".equals(name)) {
					try {
						// class属性で指定されたクラスの
						ITableViewEditorAction action = (ITableViewEditorAction) element.createExecutableExtension("class");
						action.setText(element.getAttribute("label"));
						action.setToolTipText(element.getAttribute("tooltipText"));
						action.setActiveEditor(this);
						action.selectionChanged(viewer.getSelection());
						extensionList.add(action);

						String menubarPath = element.getAttribute("menubarPath");
						IMenuManager subMenu = menu.findMenuUsingPath(menubarPath);

						if (subMenu != null) {
							subMenu.add(action);
							add(subMenu, element.getChildren());
						} else {
							IContributionItem item = menu.findUsingPath(menubarPath);
							if (item != null) {
								if (item instanceof Separator) {
									Separator sep = (Separator) item;
									IContributionManager mgr = sep.getParent();
									mgr.add(action);
									add(subMenu, element.getChildren());
								} else if (item instanceof GroupMarker) {
									GroupMarker sep = (GroupMarker) item;
									IContributionManager mgr = sep.getParent();
									mgr.add(action);
									add(subMenu, element.getChildren());
								} else {
									DbPlugin.log("unexpected Type " + item.getClass().getName());
								}
							}
						}

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else if ("menu".equals(name)) {
					String _id = element.getAttribute("id");
					String _label = element.getAttribute("label");
					IMenuManager subMenu = menu.findMenuUsingPath(_id);
					if (subMenu == null) {
						subMenu = new MenuManager(_label, _id);
						menu.add(subMenu);
						add(subMenu, element.getChildren());
					}

				} else if ("separator".equals(name)) {
					String _name = element.getAttribute("name");
					menu.add(new Separator(_name));

				} else if ("groupMarker".equals(name)) {
					String _name = element.getAttribute("name");
					menu.add(new GroupMarker(_name));

				}

			}
		} catch (Exception e) {
			DbPlugin.log(e);
		}
	}

	private void disposeExtensionPoint() {
		// 拡張しているリスナーを破棄する
		for (Iterator iter = extensionList.iterator(); iter.hasNext();) {
			ITableViewEditorAction action = (ITableViewEditorAction) iter.next();
			action.setActiveEditor(null);
			action = null;
		}
	}


}
