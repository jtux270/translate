package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class VmInterfaceListModel extends SearchableListModel
{

    private UICommand privateNewCommand;
    private UICommand privateEditCommand;
    private UICommand privateRemoveCommand;

    public VmInterfaceListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().networkInterfacesTitle());
        setHelpTag(HelpTag.network_interfaces);
        setHashName("network_interfaces"); //$NON-NLS-1$

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$

        initSelectionGeustAgentData(getSelectedItem());
        updateActionAvailability();
    }

    private List<VmGuestAgentInterface> guestAgentData;

    private List<VmGuestAgentInterface> selectionGuestAgentData;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }


    @Override
    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    public List<VmGuestAgentInterface> getGuestAgentData() {
        return guestAgentData;
    }

    public void setGuestAgentData(List<VmGuestAgentInterface> guestAgentData) {
        this.guestAgentData = guestAgentData;
    }

    public List<VmGuestAgentInterface> getSelectionGuestAgentData() {
        return selectionGuestAgentData;
    }

    public void setSelectionGuestAgentData(List<VmGuestAgentInterface> selectedGuestAgentData) {
        this.selectionGuestAgentData = selectedGuestAgentData;
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        if (getEntity() != null)
        {
            getSearchCommand().execute();
        }

        updateActionAvailability();
    }

    @Override
    protected void syncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }

        final VM vm = getEntity();

        // Initialize guest agent data
        AsyncQuery getVmGuestAgentInterfacesByVmIdQuery = new AsyncQuery();
        getVmGuestAgentInterfacesByVmIdQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                setGuestAgentData((List<VmGuestAgentInterface>) result);
                VmInterfaceListModel.super.syncSearch(VdcQueryType.GetVmInterfacesByVmId, new IdQueryParameters(vm.getId()));
            }
        };
        AsyncDataProvider.getVmGuestAgentInterfacesByVmId(getVmGuestAgentInterfacesByVmIdQuery, vm.getId());
    }

    private void newEntity()
    {
        if (getWindow() != null)
        {
            return;
        }

        VmInterfaceModel model =
                NewVmInterfaceModel.createInstance(getEntity().getStaticData(),
                        getEntity().getStatus(),
                        getEntity().getStoragePoolId(),
                        getEntity().getVdsGroupCompatibilityVersion(),
                        (ArrayList<VmNetworkInterface>) getItems(),
                        this);
        setWindow(model);
    }

    private void edit()
    {
        if (getWindow() != null)
        {
            return;
        }

        VmInterfaceModel model =
                EditVmInterfaceModel.createInstance(getEntity().getStaticData(), getEntity(),
                        getEntity().getVdsGroupCompatibilityVersion(),
                        (ArrayList<VmNetworkInterface>) getItems(),
                        (VmNetworkInterface) getSelectedItem(), this);
        setWindow(model);
    }

    private void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        RemoveVmInterfaceModel model = new RemoveVmInterfaceModel(this, getSelectedItems(), false);
        setWindow(model);
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);

        if (e.propertyName.equals("status")) //$NON-NLS-1$
        {
            updateActionAvailability();
        }
    }

    private void updateActionAvailability()
    {
        VM vm = getEntity();

        ArrayList<VM> items = new ArrayList<VM>();
        if (vm != null)
        {
            items.add(vm);
        }

        getNewCommand().setIsExecutionAllowed(vm != null
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.AddVmInterface));
        getEditCommand().setIsExecutionAllowed(vm != null
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.UpdateVmInterface)
                && (getSelectedItems() != null && getSelectedItems().size() == 1));
        getRemoveCommand().setIsExecutionAllowed(vm != null
                && VdcActionUtils.canExecute(items, VM.class, VdcActionType.RemoveVmInterface) && canRemoveNics()
                && (getSelectedItems() != null && getSelectedItems().size() > 0));
    }

    private boolean canRemoveNics() {
        VM vm = getEntity();
        if (VMStatus.Down.equals(vm.getStatus())) {
            return true;
        }

        ArrayList<VmNetworkInterface> nics =
                getSelectedItems() != null ? Linq.<VmNetworkInterface> cast(getSelectedItems())
                        : new ArrayList<VmNetworkInterface>();

        for (VmNetworkInterface nic : nics)
        {
            if (nic.isPlugged())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewCommand())
        {
            newEntity();
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
        return "VmInterfaceListModel"; //$NON-NLS-1$
    }

    @Override
    protected void onSelectedItemChanging(Object newValue, Object oldValue) {
        initSelectionGeustAgentData(newValue);
        super.onSelectedItemChanging(newValue, oldValue);
    }

    private void initSelectionGeustAgentData(Object selectedItem) {
        if (selectedItem == null || getGuestAgentData() == null){
            setSelectionGuestAgentData(null);
            return;
        }
        List<VmGuestAgentInterface> selectionInterfaces = new ArrayList<VmGuestAgentInterface>();

        for (VmGuestAgentInterface guestInterface : getGuestAgentData()) {
            if (ObjectUtils.objectsEqual(guestInterface.getMacAddress(),
                    ((VmNetworkInterface) selectedItem).getMacAddress())) {
                selectionInterfaces.add(guestInterface);
            }
        }

        setSelectionGuestAgentData(selectionInterfaces);
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    public VM getEntity() {
        return (VM) super.getEntity();
    }

    @Override
    public void setItems(Collection value) {
        super.setItems(value);
        if (getSelectedItem() == null && (getSelectedItems() == null || getSelectedItems().size() == 0)) {
            if (value != null && value.iterator().hasNext()) {
                setSelectedItem(value.iterator().next());
            }
        }
    }
}
