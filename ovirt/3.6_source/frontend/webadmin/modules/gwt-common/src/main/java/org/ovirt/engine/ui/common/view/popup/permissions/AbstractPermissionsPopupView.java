package org.ovirt.engine.ui.common.view.popup.permissions;

import java.util.ArrayList;

import org.ovirt.engine.core.aaa.ProfileEntry;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.presenter.popup.permissions.AbstractPermissionsPopupPresenterWidget;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.HasUiCommandClickHandlers;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.PopupNativeKeyPressHandler;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.TextBoxChanger;
import org.ovirt.engine.ui.common.widget.renderer.NameRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.AbstractEntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.users.AdElementListModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;

public abstract class AbstractPermissionsPopupView<T extends AdElementListModel> extends AbstractModelBoundPopupView<T> implements AbstractPermissionsPopupPresenterWidget.ViewDef<T> {

    @SuppressWarnings("rawtypes")
    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, AbstractPermissionsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    public interface Style extends CssResource {
        String alignBottomSearch();
    }

    /**
     * This is the max width of a column in this dialogs
     */
    private static final String MAX_COL_WIDTH = "270px"; //$NON-NLS-1$

    @UiField
    @WithElementId
    public UiCommandButton searchButton;

    @UiField(provided = true)
    @Path("profile.selectedItem")
    @WithElementId("profile")
    public ListModelListBoxEditor<ProfileEntry> profileSelection;

    @UiField(provided = true)
    @Path("namespace.selectedItem")
    @WithElementId("namespace")
    public ListModelListBoxEditor<String> namespaceSelection;

    @UiField(provided = true)
    @Path("role.selectedItem")
    @WithElementId("role")
    public ListModelListBoxEditor<Role> roleSelection;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    public EntityModelCellTable<ListModel> searchItems;

    @UiField
    @Ignore
    @WithElementId
    public RadioButton everyoneRadio;

    @UiField
    @Ignore
    @WithElementId
    public RadioButton specificUserOrGroupRadio;

    @UiField
    @Path("searchString")
    @WithElementId("searchString")
    public TextBoxChanger searchStringEditor;

    @UiField
    public FlowPanel everyonePanel;

    @UiField
    public FlowPanel roleSelectionPanel;

    @UiField
    public ScrollPanel searchItemsScrollPanel;

    @UiField
    @Ignore
    Label errorMessage;

    @UiField
    Style style;

    private PopupNativeKeyPressHandler nativeKeyPressHandler;

    private final static CommonApplicationConstants constants = AssetProvider.getConstants();

    public AbstractPermissionsPopupView(EventBus eventBus) {
        super(eventBus);
        initListBoxEditors();
        searchItems = new EntityModelCellTable<ListModel>(true);
        searchItems.enableColumnResizing();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        generateIds();
        searchStringEditor.setStyleName("");
        initTable();
        specificUserOrGroupRadio.setValue(true);
        everyoneRadio.setValue(false);
        //Have to add these classes to the searchStringEditor as the UiBinder seems to remove them
        searchStringEditor.addStyleName("form-control"); //$NON-NLS-1$
        searchStringEditor.addStyleName(style.alignBottomSearch());
        localize();
    }

    protected abstract void generateIds();

    protected abstract T doFlush();

    private void initListBoxEditors() {
        profileSelection = new ListModelListBoxEditor<ProfileEntry>(new NullSafeRenderer<ProfileEntry>() {
            @Override
            public String renderNullSafe(ProfileEntry object) {
                return object.toString();
            }
        });

        roleSelection = new ListModelListBoxEditor<>(new NameRenderer<Role>());

        namespaceSelection = new ListModelListBoxEditor<String>(new NullSafeRenderer<String>() {
            @Override
            protected String renderNullSafe(String object) {
                return object;
            }
        });
    }

    private void initTable() {
        // Table Entity Columns
        searchItems.addColumn(new AbstractEntityModelTextColumn<DbUser>() {
            @Override
            public String getText(DbUser user) {
                return user.getFirstName();
            }
        }, constants.firsNamePermissionsPopup(), MAX_COL_WIDTH);

        searchItems.addColumn(new AbstractEntityModelTextColumn<DbUser>() {
            @Override
            public String getText(DbUser user) {
                return user.getLastName();
            }
        }, constants.lastNamePermissionsPopup(), MAX_COL_WIDTH);

        searchItems.addColumn(new AbstractEntityModelTextColumn<DbUser>() {
            @Override
            public String getText(DbUser user) {
                return user.getLoginName();
            }
        }, constants.userNamePermissionsPopup(), MAX_COL_WIDTH);
    }

    void localize() {
        searchButton.setLabel(constants.goPermissionsPopup());
    }

    @Override
    public void edit(final T object) {
        searchItems.setRowData(new ArrayList<EntityModel>());
        searchItems.asEditor().edit(object);
    }

    @Override
    public T flush() {
        searchItems.flush();
        return doFlush();
    }

    @Override
    public void focusInput() {
        searchStringEditor.setFocus(true);
    }

    @Override
    public HasUiCommandClickHandlers getSearchButton() {
        return searchButton;
    }

    @Override
    public HasKeyPressHandlers getKeyPressSearchInputBox() {
        return searchStringEditor;
    }

    @Override
    public HasClickHandlers getEveryoneRadio() {
        return everyoneRadio;
    }

    @Override
    public HasClickHandlers getSpecificUserOrGroupRadio() {
        return specificUserOrGroupRadio;
    }

    @Override
    public PopupNativeKeyPressHandler getNativeKeyPressHandler() {
        return nativeKeyPressHandler;
    }

    @Override
    public HasHandlers getSearchStringEditor() {
        return searchStringEditor;
    }

    @Override
    public void setPopupKeyPressHandler(PopupNativeKeyPressHandler handler) {
        super.setPopupKeyPressHandler(handler);
        this.nativeKeyPressHandler = handler;
    }

    @Override
    public void changeStateOfElementsWhenAccessIsForEveryone(boolean isEveryone) {
        profileSelection.setEnabled(!isEveryone);
        searchStringEditor.setEnabled(!isEveryone);
        searchButton.getCommand().setIsExecutionAllowed(!isEveryone);
        searchItems.setVisible(!isEveryone);
    }

    @Override
    public HasValue<String> getSearchString() {
        return searchStringEditor;
    }

    @Override
    public void hideRoleSelection(Boolean indic) {
        roleSelectionPanel.setVisible(!indic);
    }

    @Override
    public void hideEveryoneSelection(Boolean indic) {
        everyonePanel.setVisible(!indic);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        errorMessage.setText(message);
    }
}
