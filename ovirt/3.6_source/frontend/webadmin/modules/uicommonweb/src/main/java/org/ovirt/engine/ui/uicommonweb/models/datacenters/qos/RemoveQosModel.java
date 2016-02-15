package org.ovirt.engine.ui.uicommonweb.models.datacenters.qos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ovirt.engine.core.common.action.QosParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Nameable;
import org.ovirt.engine.core.common.businessentities.qos.QosBase;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.FrontendMultipleQueryAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleQueryAsyncCallback;

public abstract class RemoveQosModel<T extends QosBase> extends ConfirmationModel {

    private final ListModel<T> sourceListModel;

    public RemoveQosModel(ListModel<T> sourceListModel) {
        this.sourceListModel = sourceListModel;

        setTitle(getTitle());
        setMessage();
        addCommands();
    }

    private void addCommands() {
        getCommands().add(UICommand.createDefaultOkUiCommand("onRemove", this)); //$NON-NLS-1$

        getCommands().add(UICommand.createCancelUiCommand("cancel", this)); //$NON-NLS-1$
    }

    @Override
    public abstract String getTitle();

    protected abstract VdcQueryType getUsingEntitiesByQosIdQueryType();

    protected abstract String getRemoveQosMessage(int size);

    protected abstract String getRemoveQosHashName();

    protected abstract HelpTag getRemoveQosHelpTag();

    protected abstract VdcActionType getRemoveActionType();

    private void setMessage() {
        ArrayList<VdcQueryParametersBase> parameters = new ArrayList<VdcQueryParametersBase>();
        ArrayList<VdcQueryType> queryTypes = new ArrayList<VdcQueryType>();
        for (T qos : sourceListModel.getSelectedItems()) {
            VdcQueryParametersBase parameter = new IdQueryParameters(qos.getId());
            parameters.add(parameter);
            queryTypes.add(getUsingEntitiesByQosIdQueryType());
        }
        Frontend.getInstance().runMultipleQueries(queryTypes, parameters, new IFrontendMultipleQueryAsyncCallback() {

            @Override
            public void executed(FrontendMultipleQueryAsyncResult result) {
                Map<String, String> entitiesAndQos = new HashMap<String, String>();

                setHelpTag(getRemoveQosHelpTag());
                setHashName(getRemoveQosHashName());

                int index = 0;
                for (VdcQueryReturnValue returnValue : result.getReturnValues()) {
                    for (Nameable entity : (List<Nameable>) returnValue.getReturnValue()) {
                        entitiesAndQos.put(entity.getName(), sourceListModel.getSelectedItems()
                                .get(index)
                                .getName());
                    }
                    index++;
                }
                if (entitiesAndQos.isEmpty()) {
                    ArrayList<String> list = new ArrayList<String>();
                    for (T item : sourceListModel.getSelectedItems()) {
                        list.add(item.getName());
                    }
                    setItems(list);
                } else {
                    setMessage(getRemoveQosMessage(entitiesAndQos.size()));

                    ArrayList<String> list = new ArrayList<String>();
                    for (Entry<String, String> item : entitiesAndQos.entrySet()) {
                        list.add(item.getKey() + " (" + item.getValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    setItems(list);
                }
            }
        });
    }

    public void onRemove() {
        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();

        for (T qos : sourceListModel.getSelectedItems()) {
            QosParametersBase<T> parameter = new QosParametersBase<T>();
            parameter.setQosId(qos.getId());
            parameters.add(parameter);
        }

        Frontend.getInstance().runMultipleAction(getRemoveActionType(), parameters);

        cancel();
    }

    private void cancel() {
        sourceListModel.setConfirmWindow(null);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);
        if ("onRemove".equals(command.getName())) { //$NON-NLS-1$
            onRemove();
        } else if ("cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        }
    }
}
