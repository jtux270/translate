package org.ovirt.engine.ui.webadmin.section.main.view.popup.configure;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.common.SelectionTreeNodeModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.roles_ui.RoleModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.configure.RolePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.uicommon.model.ModelListTreeViewModel;
import org.ovirt.engine.ui.webadmin.uicommon.model.SimpleSelectionTreeNodeModel;
import org.ovirt.engine.ui.webadmin.widget.editor.EntityModelCellTree;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.inject.Inject;

public class RolePopupView extends AbstractModelBoundPopupView<RoleModel> implements RolePopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<RoleModel, RolePopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, RolePopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<RolePopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static final CellTree.Resources res = GWT.create(AssignTagTreeResources.class);

    @UiField
    @Path(value = "name.entity")
    @WithElementId("name")
    StringEntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId("description")
    StringEntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Ignore
    Label accountTypeLabel;

    @UiField
    @Ignore
    @WithElementId("userRadioButton")
    RadioButton userRadioButtonEditor;

    @UiField
    @Ignore
    @WithElementId("adminRadioButton")
    RadioButton adminRadioButtonEditor;

    @UiField
    @Ignore
    Label explainationLabel;

    @UiField
    @Ignore
    @WithElementId
    Button expandAllButton;

    @UiField
    @Ignore
    @WithElementId
    Button collapseAllButton;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTree<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel> tree;

    private final Driver driver = GWT.create(Driver.class);

    private RoleModel roleModel;

    @Inject
    public RolePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initTree();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        driver.initialize(this);
        localize(constants);
        initExpandButtons();
        initRadioButtons();
    }

    private void initRadioButtons() {
        userRadioButtonEditor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (userRadioButtonEditor.getValue()) {
                    roleModel.getIsAdminRole().setEntity(false);
                }
            }
        });
        adminRadioButtonEditor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (adminRadioButtonEditor.getValue()) {
                    roleModel.getIsAdminRole().setEntity(true);
                }
            }
        });
    }

    private void initExpandButtons() {
        expandAllButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                expandTree();
            }
        });
        collapseAllButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                collapseTree();
            }
        });
    }

    private void initTree() {
        tree = new EntityModelCellTree<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel>(res);
    }

    private void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.RoleNameLabel());
        descriptionEditor.setLabel(constants.RoleDescriptionLabel());
        accountTypeLabel.setText(constants.RoleAccount_TypeLabel());
        userRadioButtonEditor.setText(constants.RoleUserLabel());
        adminRadioButtonEditor.setText(constants.RoleAdminLabel());
        explainationLabel.setText(constants.RoleCheckBoxes());
        expandAllButton.setText(constants.RoleExpand_AllLabel());
        collapseAllButton.setText(constants.RoleCollapse_AllLabel());
    }

    @Override
    public void edit(RoleModel object) {
        this.roleModel = object;
        driver.edit(object);
        final EntityModel<Boolean> adminRole = object.getIsAdminRole();

        // Listen to Properties
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                RoleModel model = (RoleModel) sender;
                String propertyName = ((PropertyChangedEventArgs) args).propertyName;
                if ("PermissionGroupModels".equals(propertyName)) { //$NON-NLS-1$
                    updateTree(model);
                }
            }
        });

        object.getIsAdminRole().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (adminRole.getEntity() == true) {
                    adminRadioButtonEditor.setValue(true);
                } else {
                    userRadioButtonEditor.setValue(true);
                }

            }
        });

        object.getIsAdminRole().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (!adminRole.getIsChangable()) {
                    adminRadioButtonEditor.setEnabled(false);
                    userRadioButtonEditor.setEnabled(false);
                }
            }
        });
    }

    private void updateTree(RoleModel model) {
        // Get tag node list
        ArrayList<SelectionTreeNodeModel> tagTreeNodes = model.getPermissionGroupModels();

        // Get tree view model
        ModelListTreeViewModel<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel> modelListTreeViewModel =
                tree.getTreeViewModel();

        // Set root nodes
        List<SimpleSelectionTreeNodeModel> rootNodes = SimpleSelectionTreeNodeModel.fromList(tagTreeNodes);
        modelListTreeViewModel.setRoots(rootNodes);

        // Update tree data
        AsyncDataProvider<SimpleSelectionTreeNodeModel> asyncTreeDataProvider =
                modelListTreeViewModel.getAsyncTreeDataProvider();
        asyncTreeDataProvider.updateRowCount(rootNodes.size(), true);
        asyncTreeDataProvider.updateRowData(0, rootNodes);
    }

    @Override
    public RoleModel flush() {
        return driver.flush();
    }

    private void expandTree() {
        if (tree != null) {
            expandTree(tree.getRootTreeNode(), true);
        }
    }

    private void collapseTree() {
        if (tree != null) {
            expandTree(tree.getRootTreeNode(), false);
        }
    }

    private void expandTree(TreeNode node, boolean collapse) {
        if (node == null) {
            return;
        }

        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                expandTree(node.setChildOpen(i, collapse), collapse);
            }
        }
    }

    interface AssignTagTreeResources extends CellTree.Resources {

        interface TableStyle extends CellTree.Style {
        }

        @Override
        @Source({ "org/ovirt/engine/ui/webadmin/css/RoleTree.css" })
        TableStyle cellTreeStyle();

    }

}
