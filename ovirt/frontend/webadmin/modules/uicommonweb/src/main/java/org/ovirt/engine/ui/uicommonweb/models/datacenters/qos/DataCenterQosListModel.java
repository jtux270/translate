package org.ovirt.engine.ui.uicommonweb.models.datacenters.qos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.qos.QosBase;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QosQueryParameterBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;

public abstract class DataCenterQosListModel<T extends QosBase, P extends QosParametersModel<T>> extends SearchableListModel {

    private UICommand newCommand;
    private UICommand editCommand;
    private UICommand removeCommand;

    public DataCenterQosListModel() {
        setTitle(getQosTitle());
        setHelpTag(getQosHelpTag());
        setHashName(getQosHashName());

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
    }

    protected abstract String getQosTitle();

    protected abstract String getQosHashName();

    protected abstract HelpTag getQosHelpTag();

    protected abstract QosType getQosType();

    protected abstract NewQosModel<T, P> getNewQosModel();

    protected abstract EditQosModel<T, P> getEditQosModel(final T qoS);

    protected abstract RemoveQosModel<T> getRemoveQosModel();

    @Override
    public StoragePool getEntity() {
        return (StoragePool) super.getEntity();
    }

    public void setEntity(StoragePool value) {
        super.setEntity(value);
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    protected void syncSearch() {
        if (getEntity() == null) {
            return;
        }
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.model = this;
        asyncQuery.asyncCallback = new INewAsyncCallback() {

            @SuppressWarnings("unchecked")
            @Override
            public void onSuccess(Object model, Object returnValue) {
                DataCenterQosListModel<T, P> qosListModel = (DataCenterQosListModel) model;
                qosListModel.setItems((ArrayList<T>) ((VdcQueryReturnValue) returnValue).getReturnValue());

            }
        };
        IdQueryParameters parameters = new QosQueryParameterBase(getEntity().getId(), getQosType());
        parameters.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetAllQosByStoragePoolIdAndType,
                parameters,
                asyncQuery);
    }

    public UICommand getNewCommand() {
        return newCommand;
    }

    public void setNewCommand(UICommand newCommand) {
        this.newCommand = newCommand;
    }

    @Override
    public UICommand getEditCommand() {
        return editCommand;
    }

    public void setEditCommand(UICommand editCommand) {
        this.editCommand = editCommand;
    }

    public UICommand getRemoveCommand() {
        return removeCommand;
    }

    public void setRemoveCommand(UICommand removeCommand) {
        this.removeCommand = removeCommand;
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getNewCommand()) {
            newQos();
        } else if (command == getEditCommand()) {
            edit();
        } else if (command == getRemoveCommand()) {
            remove();
        }
    }

    public void remove() {
        if (getConfirmWindow() != null) {
            return;
        }

        setConfirmWindow(getRemoveQosModel());
    }

    public void edit() {
        T qos = (T) getSelectedItem();

        if (getWindow() != null) {
            return;
        }

        final EditQosModel<T, P> qosModel = getEditQosModel(qos);
        setWindow(qosModel);

        qosModel.getDataCenters().setItems(Arrays.asList(getEntity()), getEntity());
    }

    @Override
    protected void onSelectedItemChanged() {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability() {
        List selectedItems = getSelectedItems();

        getEditCommand().setIsExecutionAllowed(selectedItems != null && selectedItems.size() == 1);
        getRemoveCommand().setIsExecutionAllowed(selectedItems != null && selectedItems.size() > 0);
    }

    public void newQos() {
        if (getWindow() != null) {
            return;
        }

        final NewQosModel<T, P> newQosModel = getNewQosModel();
        setWindow(newQosModel);

        newQosModel.getDataCenters().setItems(Arrays.asList(getEntity()), getEntity());
    }

}
