package org.ovirt.engine.ui.uicommonweb.models.networks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.PairQueryable;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostSetupNetworksModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

@SuppressWarnings("unused")
public class NetworkHostListModel extends SearchableListModel
{
    private UICommand setupNetworksCommand;
    private NetworkHostFilter viewFilterType;

    private Collection<VdsNetworkInterface> attachedByLabelInterfaces;

    public NetworkHostListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().hostsTitle());
        setHelpTag(HelpTag.hosts);
        setHashName("hosts"); //$NON-NLS-1$

        setSetupNetworksCommand(new UICommand("SetupNetworks", this)); //$NON-NLS-1$

        updateActionAvailability();
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
        asyncQuery.setModel(getViewFilterType());
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(final Object model, Object ReturnValue) {
                if (model.equals(getViewFilterType())) {
                    final Iterable returnList = (Iterable) ((VdcQueryReturnValue) ReturnValue).getReturnValue();
                    if (NetworkHostFilter.unattached.equals(getViewFilterType())) {
                        final List<PairQueryable<VdsNetworkInterface, VDS>> items =
                                new ArrayList<PairQueryable<VdsNetworkInterface, VDS>>();
                        for (Object obj : returnList) {
                            items.add(new PairQueryable<VdsNetworkInterface, VDS>(null, (VDS) obj));
                        }
                        setItems(items);
                    } else if (NetworkHostFilter.attached.equals(getViewFilterType())) {
                        initAttachedInterfaces((List<PairQueryable<VdsNetworkInterface, VDS>>) returnList);
                    }
                }
            }
        };

        IdQueryParameters params = new IdQueryParameters(getEntity().getId());
        params.setRefresh(getIsQueryFirstTime());

        if (NetworkHostFilter.unattached.equals(getViewFilterType())) {
            Frontend.getInstance().runQuery(VdcQueryType.GetVdsWithoutNetwork, params, asyncQuery);
        } else if (NetworkHostFilter.attached.equals(getViewFilterType())) {
            Frontend.getInstance().runQuery(VdcQueryType.GetVdsAndNetworkInterfacesByNetworkId, params, asyncQuery);
        }

        setIsQueryFirstTime(false);
    }

    private void initAttachedInterfaces(final List<PairQueryable<VdsNetworkInterface, VDS>> items) {
        if (StringUtils.isEmpty(getEntity().getLabel()) || items.isEmpty()) {
            setItems(items);
            return;
        }

        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(getViewFilterType());
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValueObj) {
                if (!model.equals(getViewFilterType())) {
                    return;
                }

                attachedByLabelInterfaces =
                        (Collection<VdsNetworkInterface>) ((VdcQueryReturnValue) returnValueObj).getReturnValue();
                setItems(items);
            }
        };

        IdQueryParameters params = new IdQueryParameters(getEntity().getId());
        params.setRefresh(false);
        Frontend.getInstance().runQuery(VdcQueryType.GetInterfacesByLabelForNetwork,
                params,
                asyncQuery);
    }

    public Boolean isInterfaceAttachedByLabel(VdsNetworkInterface iface) {
        return attachedByLabelInterfaces != null && attachedByLabelInterfaces.contains(iface);
    }

    @Override
    public void setItems(Collection value) {
        Collections.sort((List<PairQueryable<VdsNetworkInterface, VDS>>) value,
                new Comparator<PairQueryable<VdsNetworkInterface, VDS>>() {

                    @Override
                    public int compare(PairQueryable<VdsNetworkInterface, VDS> arg0,
                            PairQueryable<VdsNetworkInterface, VDS> arg1) {
                        int compareValue =
                                arg0.getSecond().getVdsGroupName().compareTo(arg1.getSecond().getVdsGroupName());

                        if (compareValue != 0) {
                            return compareValue;
                        }

                        return arg0.getSecond().getName().compareTo(arg1.getSecond().getName());
                    }
                });
        super.setItems(value);
    }

    public void setupNetworks() {
        if (getWindow() != null) {
            return;
        }

        HostSetupNetworksModel setupNetworksWindowModel =
                new HostSetupNetworksModel(this,
                        ((PairQueryable<VdsNetworkInterface, VDS>) getSelectedItem()).getSecond());
        setWindow(setupNetworksWindowModel);
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

        if (command == getSetupNetworksCommand())
        {
            setupNetworks();
        }

    }

    private void updateActionAvailability() {
        List<PairQueryable<VdsNetworkInterface, VDS>> selectedItems =
                getSelectedItems() != null ? getSelectedItems() : new ArrayList();

        getSetupNetworksCommand().setIsExecutionAllowed(selectedItems.size() == 1
                && selectedItems.get(0).getSecond().getVdsGroupCompatibilityVersion().compareTo(Version.v3_1) >= 0);
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

    public UICommand getSetupNetworksCommand() {
        return setupNetworksCommand;
    }

    private void setSetupNetworksCommand(UICommand value) {
        setupNetworksCommand = value;
    }

    public NetworkHostFilter getViewFilterType() {
        return viewFilterType;
    }

    public void setViewFilterType(NetworkHostFilter viewFilterType) {
        this.viewFilterType = viewFilterType;
        search();
    }

    @Override
    public NetworkView getEntity() {
        return (NetworkView) ((super.getEntity() instanceof NetworkView) ? super.getEntity() : null);
    }

    public void setEntity(NetworkView value)
    {
        super.setEntity(value);
    }

    @Override
    protected String getListName() {
        return "NetworkHostListModel"; //$NON-NLS-1$
    }

}
