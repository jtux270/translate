package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class VmAppListModel extends SearchableListModel
{

    @Override
    public Collection getItems()
    {
        return items;
    }

    @Override
    public void setItems(Collection value)
    {
        if (items != value)
        {
            itemsChanging(value, items);
            items = value;
            itemsChanged();
            getItemsChangedEvent().raise(this, EventArgs.EMPTY);
            onPropertyChanged(new PropertyChangedEventArgs("Items")); //$NON-NLS-1$
        }
    }

    public VmAppListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().applicationsTitle());
        setHelpTag(HelpTag.applications);
        setHashName("applications"); //$NON-NLS-1$
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);
        if (e.propertyName.equals("appList")) //$NON-NLS-1$
        {
            updateAppList();
        }
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        updateAppList();
    }

    protected void updateAppList() {
        if (getEntity() instanceof VM) {
            updateAppListFromVm((VM) getEntity());
        } else {
            VmPool pool = (VmPool) getEntity();
            if (pool != null)
            {
                AsyncQuery _asyncQuery = new AsyncQuery();
                _asyncQuery.setModel(this);
                _asyncQuery.asyncCallback = new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model, Object result)
                    {
                        if (result != null)
                        {
                            VM vm = (VM) ((VdcQueryReturnValue) result).getReturnValue();
                            if (vm != null) {
                                updateAppListFromVm(vm);
                            }
                        }
                    }
                };
                Frontend.getInstance().runQuery(VdcQueryType.GetVmDataByPoolId,
                        new IdQueryParameters(pool.getVmPoolId()),
                        _asyncQuery);
            }
        }
    }

    private void updateAppListFromVm(VM vm) {
        setItems(null);
        if (vm != null && vm.getAppList() != null)
        {
            ArrayList<String> list = new ArrayList<String>();

            String[] array = vm.getAppList().split("[,]", -1); //$NON-NLS-1$
            for (String item : array)
            {
                list.add(item);
            }
            Collections.sort(list);

            setItems(list);
        } else {
            setItems(new ArrayList<String>());
        }
    }

    @Override
    protected void syncSearch()
    {
        updateAppList();
        setIsQueryFirstTime(false);
    }

    @Override
    protected String getListName() {
        return "VmAppListModel"; //$NON-NLS-1$
    }
}
