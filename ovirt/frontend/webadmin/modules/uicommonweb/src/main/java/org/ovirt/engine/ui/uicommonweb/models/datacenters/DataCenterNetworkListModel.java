package org.ovirt.engine.ui.uicommonweb.models.datacenters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class DataCenterNetworkListModel extends SearchableListModel
{

    private static String ENGINE_NETWORK;

    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    @Override
    public StoragePool getEntity()
    {
        return (StoragePool) super.getEntity();
    }

    public void setEntity(StoragePool value)
    {
        super.setEntity(value);
    }

    public DataCenterNetworkListModel()
    {
        // get management network name
        ENGINE_NETWORK = (String) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.ManagementNetwork);

        setTitle(ConstantsManager.getInstance().getConstants().logicalNetworksTitle());
        setHelpTag(HelpTag.logical_networks);
        setHashName("logical_networks"); //$NON-NLS-1$

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$

        updateActionAvailability();
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    public void search()
    {
        if (getEntity() != null)
        {
            super.search();
        }
    }

    @Override
    protected void syncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }

        super.syncSearch();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                ArrayList<Network> newItems = ((VdcQueryReturnValue) ReturnValue).getReturnValue();
                Collections.sort(newItems, new Linq.NetworkComparator());
                SearchableListModel searchableListModel = (SearchableListModel) model;
                searchableListModel.setItems(newItems);
            }
        };

        IdQueryParameters tempVar = new IdQueryParameters(getEntity().getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetAllNetworks, tempVar, _asyncQuery);
    }

    public void remove()
    {
        if (getConfirmWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new RemoveNetworksModel(this);
        setConfirmWindow(model);
    }

    public void edit()
    {
        final Network network = (Network) getSelectedItem();

        if (getWindow() != null)
        {
            return;
        }

        final NetworkModel networkModel = new EditNetworkModel(network, this);
        setWindow(networkModel);

        networkModel.getDataCenters().setItems(Arrays.asList(getEntity()));
        networkModel.getDataCenters().setSelectedItem(getEntity());

    }

    public void newNetwork()
    {
        if (getWindow() != null)
        {
            return;
        }

        final NetworkModel networkModel = new NewNetworkModel(this);
        setWindow(networkModel);

        networkModel.getDataCenters().setItems(Arrays.asList(getEntity()));
        networkModel.getDataCenters().setSelectedItem(getEntity());
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        List tempVar = getSelectedItems();
        ArrayList selectedItems =
                (ArrayList) ((tempVar != null) ? tempVar : new ArrayList());

        boolean anyEngine = false;
        for (Object item : selectedItems)
        {
            Network network = (Network) item;
            if (ObjectUtils.objectsEqual(network.getName(), ENGINE_NETWORK))
            {
                anyEngine = true;
                break;
            }
        }

        getEditCommand().setIsExecutionAllowed(selectedItems.size() == 1);
        getRemoveCommand().setIsExecutionAllowed(selectedItems.size() > 0 && !anyEngine);
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewCommand())
        {
            newNetwork();
        }
        else if (command == getEditCommand())
        {
            edit();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
    }

    @Override
    protected String getListName() {
        return "DataCenterNetworkListModel"; //$NON-NLS-1$
    }

}
