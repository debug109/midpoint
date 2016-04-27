/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<T extends ObjectType> extends BasePanel<T> {
	private static final long serialVersionUID = 1L;

	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_TABLE = "table";

	private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);

	private Class<T> type;
	private PageBase parentPage;

	private LoadableModel<Search> searchModel;

	private BaseSortableDataProvider<SelectableBean<T>> provider;

	private Collection<SelectorOptions<GetOperationOptions>> options;

	private boolean multiselect;
	
	private TableId tableId;

	public Class<T> getType() {
		return type;
	}

	private static Map<Class, String> storageMap;

	static {
		storageMap = new HashMap<Class, String>();
		storageMap.put(PageUsers.class, SessionStorage.KEY_USERS);
		storageMap.put(PageResources.class, SessionStorage.KEY_RESOURCES);
		storageMap.put(PageReports.class, SessionStorage.KEY_REPORTS);
		storageMap.put(PageRoles.class, SessionStorage.KEY_ROLES);
		storageMap.put(PageServices.class, SessionStorage.KEY_SERVICES);
	}
	
//	private static Map<Class, TableId> tablePagingMap;

//	static {
//		tablePagingMap = new HashMap<Class, TableId>();
//		tablePagingMap.put(PageResources.class, TableId.PAGE_RESOURCES_PANEL);
//		tablePagingMap.put(PageReports.class, TableId.PAGE_REPORTS);
//		tablePagingMap.put(PageRoles.class, TableId.TABLE_ROLES);
//		tablePagingMap.put(PageServices.class, TableId.TABLE_SERVICES);
//		tablePagingMap.put(PageUsers.class, TableId.TABLE_USERS);
//	}

	public ObjectListPanel(String id, Class<T> type, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options,
			PageBase parentPage) {
		super(id);
		this.type = type;
		this.parentPage = parentPage;
		this.options = options;
		this.tableId = tableId;
		initLayout();
	}

	ObjectListPanel(String id, Class<T> type, boolean multiselect, PageBase parentPage) {
		super(id);
		this.type = type;
		this.parentPage = parentPage;
		this.multiselect = multiselect;
		initLayout();
	}

	public boolean isMultiselect() {
		return multiselect;
	}

	public void setProvider(BaseSortableDataProvider<SelectableBean<T>> provider) {
		this.provider = provider;
	}

	public List<T> getSelectedObjects() {
		BaseSortableDataProvider<SelectableBean<T>> dataProvider = getDataProvider();
		if (dataProvider instanceof ObjectDataProvider2) {
			return ((ObjectDataProvider2) dataProvider).getSelectedData();
		} else if (dataProvider instanceof ListDataProvider2) {
			return ((ListDataProvider2) dataProvider).getSelectedObjects();
		}
		return new ArrayList<>();
	}

	private void initLayout() {
		Form<T> mainForm = new Form<T>(ID_MAIN_FORM);
		add(mainForm);

		searchModel = new LoadableModel<Search>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			public Search load() {
				String storageKey = getStorageKey();
				Search search = null;
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getPageStorage(storageKey);
					if (storage != null) {
						search = storage.getSearch();
					}
				}
				if (search == null) {
					search = SearchFactory.createSearch(type, parentPage.getPrismContext(), true);
				}
				return search;
			}
		};

		BoxedTablePanel<SelectableBean<T>> table = createTable();
		mainForm.add(table);

	}

	protected BaseSortableDataProvider<SelectableBean<T>> getProvider() {
		if (provider != null) {
			return provider;
		}
		ObjectDataProvider2<SelectableBean<T>, T> objProvider = new ObjectDataProvider2<SelectableBean<T>, T>(
				parentPage, type) {
			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				String storageKey = getStorageKey();
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getPageStorage(storageKey);
					if (storage != null) {
						storage.setPaging(paging);
					}
				}
			}

			@Override
			public SelectableBean<T> createDataObjectWrapper(T obj) {
				SelectableBean<T> bean = super.createDataObjectWrapper(obj);
				List<InlineMenuItem> inlineMenu = createInlineMenu();
				if (inlineMenu != null) {
					bean.getMenuItems().addAll(inlineMenu);
				}
				return bean;
			}
		};
		if (options == null){
			if (ResourceType.class.equals(type)) {
				options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
			}
		} else {
			if (ResourceType.class.equals(type)) {
				GetOperationOptions root = SelectorOptions.findRootOptions(options);
				root.setNoFetch(Boolean.TRUE);
			}
			objProvider.setOptions(options);
		}
		provider = objProvider;
		return provider;
	}

	private BoxedTablePanel<SelectableBean<T>> createTable() {
		List<IColumn<SelectableBean<T>, String>> columns = initColumns();
		provider = getProvider();
		provider.setQuery(getQuery());

//		TableId tableId = tablePagingMap.get(parentPage.getClass());
		BoxedTablePanel<SelectableBean<T>> table = new BoxedTablePanel<SelectableBean<T>>(ID_TABLE, provider,
				columns, tableId, tableId == null ? 10 : parentPage.getSessionStorage().getUserProfile().getPagingSize(tableId)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return initSearch(headerId);
			}

			@Override
			protected String getBoxCssClasses() {
				return ObjectListPanel.this.getBoxCssClasses();
			}

			@Override
			protected WebMarkupContainer createButtonToolbar(String id) {
				WebMarkupContainer bar = ObjectListPanel.this.createTableButtonToolbar(id);

				return bar != null ? bar : super.createButtonToolbar(id);
			}
		};
		table.setOutputMarkupId(true);
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey); 
			if (storage != null) {
				table.setCurrentPage(storage.getPaging());
			}
		}

		return table;
	}

	protected String getBoxCssClasses() {
		return null;
	}

	/**
	 * there's no way to do it properly...
	 */
	@Deprecated
	protected WebMarkupContainer createTableButtonToolbar(String id) {
		return null;
	}

	private BaseSortableDataProvider<SelectableBean<T>> getDataProvider() {
		BoxedTablePanel<SelectableBean<T>> table = getTable();
		BaseSortableDataProvider<SelectableBean<T>> provider = (BaseSortableDataProvider<SelectableBean<T>>) table
				.getDataTable().getDataProvider();
		return provider;

	}

	protected BoxedTablePanel<SelectableBean<T>> getTable() {
		return (BoxedTablePanel<SelectableBean<T>>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private String getStorageKey() {
		return storageMap.get(parentPage.getClass());
	}

	private PageStorage getPageStorage(String storageKey){
		PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
		if (storage == null) {
			storage = getSession().getSessionStorage().initPageStorage(storageKey);
		}
		return storage;
	}
		
	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
		BaseSortableDataProvider<SelectableBean<T>> provider = getDataProvider();
		provider.setQuery(query);
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey);
			if (storage != null) {
				storage.setSearch(searchModel.getObject());
				storage.setPaging(null);
			}
		}

		Table table = getTable();
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	public void refreshTable(Class<T> newType, AjaxRequestTarget target) {
		BaseSortableDataProvider<SelectableBean<T>> provider = getDataProvider();
		provider.setQuery(getQuery());
		if (newType != null && provider instanceof ObjectDataProvider2) {
			((ObjectDataProvider2) provider).setType(newType);
		}

		if (newType != null && !this.type.equals(newType)) {
			this.type = newType;
			searchModel.reset();
		} else {
			saveSearchModel();
		}

		BoxedTablePanel table = getTable();

		((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	private SearchFormPanel initSearch(String headerId) {
		SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
				ObjectListPanel.this.searchPerformed(query, target);
			}

		};

		return searchPanel;
	}

	private void saveSearchModel() {
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey);
			if (storage != null) {
				storage.setSearch(searchModel.getObject());
				storage.setPaging(null);
			}
		}

	}

	public void clearCache() {
		BaseSortableDataProvider provider = getDataProvider();
		provider.clearCache();
		if (provider instanceof ObjectDataProvider2) {
			((ObjectDataProvider2) provider).clearSelectedObjects();
		}
	}

	public ObjectQuery getQuery() {
		ObjectQuery customQuery = createContentQuery();

		return customQuery;
	}

	protected ObjectQuery createContentQuery() {
		Search search = searchModel.getObject();
		ObjectQuery query = search.createObjectQuery(parentPage.getPrismContext());
		query = addFilterToContentQuery(query);
		return query;
	}

	protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
		return query;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	protected abstract IColumn<SelectableBean<T>, String> createCheckboxColumn();

	protected abstract IColumn<SelectableBean<T>, String> createNameColumn();

	protected abstract List<IColumn<SelectableBean<T>, String>> createColumns();

	protected abstract List<InlineMenuItem> createInlineMenu();

	protected List<IColumn<SelectableBean<T>, String>> initColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		CheckBoxHeaderColumn<SelectableBean<T>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<T>>) createCheckboxColumn();
		if (checkboxColumn != null) {
			columns.add(checkboxColumn);
		}

		IColumn<SelectableBean<T>, String> iconColumn = ColumnUtils.createIconColumn(type);
		columns.add(iconColumn);

		IColumn<SelectableBean<T>, String> nameColumn = createNameColumn();
		columns.add(nameColumn);

		List<IColumn<SelectableBean<T>, String>> others = createColumns();
		columns.addAll(others);

		return columns;
	}

	public void addPerformed(AjaxRequestTarget target, List<T> selected) {
		parentPage.hideMainPopup(target);
	}

}
