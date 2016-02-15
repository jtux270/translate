package org.ovirt.engine.ui.uicommonweb.models.networks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.PairQueryable;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkManageModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class NetworkClusterListModel extends SearchableListModel
{
    private UICommand manageCommand;

    private final Comparator<ClusterNetworkModel> manageModelComparator =
            new Comparator<ClusterNetworkModel>() {
                @Override
                public int compare(ClusterNetworkModel o1, ClusterNetworkModel o2) {
                    return o1.getCluster().getName().compareTo(o2.getCluster().getName());
                }
            };

    public NetworkClusterListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().clustersTitle());
        setHelpTag(HelpTag.clusters);
        setHashName("clusters"); //$NON-NLS-1$

        setManageCommand(new UICommand("Manage", this)); //$NON-NLS-1$
    }

    public void manage() {
        if (getWindow() != null) {
            return;
        }

        ClusterNetworkManageModel manageModel = createManageList();
        setWindow(manageModel);
        manageModel.setTitle(ConstantsManager.getInstance().getConstants().assignDetachNetworkTitle());
        manageModel.setHelpTag(HelpTag.assign_network);
        manageModel.setHashName("assign_network"); //$NON-NLS-1$
    }

    private ClusterNetworkManageModel createManageList() {
        List<ClusterNetworkModel> networkManageModelList = new ArrayList<ClusterNetworkModel>();
        List<PairQueryable<VDSGroup, NetworkCluster>> items =
                (List<PairQueryable<VDSGroup, NetworkCluster>>) getItems();

        for (PairQueryable<VDSGroup, NetworkCluster> item : items) {
            Network network = (Network) Cloner.clone(getEntity());
            if (item.getSecond() != null) {
                network.setCluster((NetworkCluster) Cloner.clone(item.getSecond()));
            }
            ClusterNetworkModel networkManageModel = new ClusterNetworkModel(network) {
                @Override
                public String getDisplayedName() {
                    return getCluster().getName();
                }
            };
            networkManageModel.setCluster((VDSGroup) Cloner.clone(item.getFirst()));

            networkManageModelList.add(networkManageModel);
        }

        Collections.sort(networkManageModelList, manageModelComparator);

        ClusterNetworkManageModel listModel = new ClusterNetworkManageModel(this) {
            @Override
            public boolean isMultiCluster() {
                return true;
            }
        };
        listModel.setItems(networkManageModelList);

        return listModel;
    }

    @Override
    public NetworkView getEntity() {
        return (NetworkView) ((super.getEntity() instanceof NetworkView) ? super.getEntity() : null);
    }

    public void setEntity(NetworkView value) {
        super.setEntity(value);
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    public void search() {
        if (getEntity() != null)
        {
            super.search();
        }
    }

    @Override
    protected void syncSearch() {
        if (getEntity() == null)
        {
            return;
        }

        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                NetworkClusterListModel.this.setItems((List<PairQueryable<VDSGroup, NetworkCluster>>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
            }
        };

        IdQueryParameters params = new IdQueryParameters(getEntity().getId());
        params.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetVdsGroupsAndNetworksByNetworkId, params, asyncQuery);
    }

    @Override
    public void setItems(Collection value) {
        Collections.sort((List<PairQueryable<VDSGroup, NetworkCluster>>) value,
                new Comparator<PairQueryable<VDSGroup, NetworkCluster>>() {

                    @Override
                    public int compare(PairQueryable<VDSGroup, NetworkCluster> arg0,
                            PairQueryable<VDSGroup, NetworkCluster> arg1) {
                        return arg0.getFirst().getName().compareTo(arg1.getFirst().getName());
                    }
                });
        super.setItems(value);
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e) {
        super.entityPropertyChanged(sender, e);

        if (e.propertyName.equals("name")) //$NON-NLS-1$
        {
            getSearchCommand().execute();
        }
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getManageCommand()) {
            manage();
        }
    }

    public UICommand getManageCommand() {
        return manageCommand;
    }

    private void setManageCommand(UICommand value) {
        manageCommand = value;
    }

    @Override
    protected String getListName() {
        return "NetworkClusterListModel"; //$NON-NLS-1$
    }
}
