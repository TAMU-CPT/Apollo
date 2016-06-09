package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.UserInfo;
import org.bbop.apollo.gwt.client.dto.UserOrganismPermissionInfo;
import org.bbop.apollo.gwt.client.event.UserChangeEvent;
import org.bbop.apollo.gwt.client.event.UserChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.UserRestService;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;
import org.gwtbootstrap3.extras.bootbox.client.callback.Callback;
import org.gwtbootstrap3.extras.bootbox.client.callback.ConfirmCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SharingPanel extends Composite {

    interface UserBrowserPanelUiBinder extends UiBinder<Widget, SharingPanel> {
    }

    private static UserBrowserPanelUiBinder ourUiBinder = GWT.create(UserBrowserPanelUiBinder.class);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox firstName;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox lastName;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox email;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<UserInfo> dataGrid = new DataGrid<UserInfo>(10, tablecss);
    @UiField
    org.gwtbootstrap3.client.ui.Button shareButton;
    @UiField
    Input passwordTextBox;
    @UiField
    Row passwordRow;
    @UiField
    ListBox roleList;
    @UiField
    FlexTable groupTable;
    @UiField
    org.gwtbootstrap3.client.ui.ListBox availableGroupList;
    @UiField
    org.gwtbootstrap3.client.ui.Button addGroupButton;
    @UiField(provided = true)
    DataGrid<UserOrganismPermissionInfo> organismPermissionsGrid = new DataGrid<>(4, tablecss);
    @UiField(provided = true)
    WebApolloSimplePager pager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField(provided = true)
    WebApolloSimplePager organismPager = new WebApolloSimplePager(WebApolloSimplePager.TextLocation.CENTER);
    @UiField
    Row userRow1;
    @UiField
    Row userRow2;
    @UiField
    org.gwtbootstrap3.client.ui.Label saveLabel;


    private ListDataProvider<UserInfo> dataProvider = new ListDataProvider<>();
    private List<UserInfo> userInfoList = new ArrayList<>();
    private List<UserInfo> filteredUserInfoList = dataProvider.getList();
    private SingleSelectionModel<UserInfo> selectionModel = new SingleSelectionModel<>();
    private UserInfo selectedUserInfo;

    private ListDataProvider<UserOrganismPermissionInfo> permissionProvider = new ListDataProvider<>();
    private List<UserOrganismPermissionInfo> permissionProviderList = permissionProvider.getList();
    private ColumnSortEvent.ListHandler<UserOrganismPermissionInfo> sortHandler = new ColumnSortEvent.ListHandler<UserOrganismPermissionInfo>(permissionProviderList);


    public SharingPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        if (roleList.getItemCount() == 0) {
            roleList.addItem("user");
            roleList.addItem("admin");
        }


        TextColumn<UserInfo> firstNameColumn = new TextColumn<UserInfo>() {
            @Override
            public String getValue(UserInfo user) {
                return user.getName();
            }
        };
        firstNameColumn.setSortable(true);

        SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
            @Override
            public SafeHtml render(String object) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<a href=\"javascript:;\">").appendEscaped(object)
                        .appendHtmlConstant("</a>");
                return sb.toSafeHtml();
            }
        };

        Column<UserInfo, String> secondNameColumn = new Column<UserInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(UserInfo user) {
                return user.getEmail();
            }
        };
        secondNameColumn.setSortable(true);

        TextColumn<UserInfo> thirdNameColumn = new TextColumn<UserInfo>() {
            @Override
            public String getValue(UserInfo user) {
                return user.getRole();
            }
        };
        thirdNameColumn.setSortable(true);

        dataGrid.addColumn(firstNameColumn, "Name");
        dataGrid.addColumn(secondNameColumn, "Email");
        dataGrid.addColumn(thirdNameColumn, "Global Role");

        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedUserInfo = selectionModel.getSelectedObject();
                updateUserInfo();
            }
        });


        dataProvider.addDataDisplay(dataGrid);
        pager.setDisplay(dataGrid);

        createOrganismPermissionsTable();


        ColumnSortEvent.ListHandler<UserInfo> sortHandler = new ColumnSortEvent.ListHandler<UserInfo>(filteredUserInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(firstNameColumn, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo o1, UserInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(secondNameColumn, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo o1, UserInfo o2) {
                return o1.getEmail().compareTo(o2.getEmail());
            }
        });
        sortHandler.setComparator(thirdNameColumn, new Comparator<UserInfo>() {
            @Override
            public int compare(UserInfo o1, UserInfo o2) {
                return o1.getRole().compareTo(o2.getRole());
            }
        });

        Annotator.eventBus.addHandler(UserChangeEvent.TYPE, new UserChangeEventHandler() {
            @Override
            public void onUserChanged(UserChangeEvent userChangeEvent) {
                switch (userChangeEvent.getAction()) {
                    case ADD_USER_TO_GROUP:
                        availableGroupList.removeItem(availableGroupList.getSelectedIndex());
                        if (availableGroupList.getItemCount() > 0) {
                            availableGroupList.setSelectedIndex(0);
                        }
                        addGroupButton.setEnabled(availableGroupList.getItemCount() > 0);

                        String group = userChangeEvent.getGroup();
                        addGroupToUi(group);
                        break;
                    case RELOAD_USERS:
                        reload();
                        break;
                    case REMOVE_USER_FROM_GROUP:
                        removeGroupFromUI(userChangeEvent.getGroup());
                        addGroupButton.setEnabled(availableGroupList.getItemCount() > 0);
                        break;
                    case USERS_RELOADED:
                        selectionModel.clear();
                        break;

                }
            }
        });
    }


    public void reload() {
        if(MainPanel.getInstance().getCurrentUser()!=null) {
            UserRestService.loadUsers(userInfoList);
        }
        dataGrid.redraw();
    }

}
