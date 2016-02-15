package org.ovirt.engine.ui.uicommonweb.models.hosts;

import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class HostVLan extends Model
{

    private VdsNetworkInterface privateInterface;

    public VdsNetworkInterface getInterface()
    {
        return privateInterface;
    }

    public void setInterface(VdsNetworkInterface value)
    {
        privateInterface = value;
    }

    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String value)
    {
        if (!ObjectUtils.objectsEqual(name, value))
        {
            name = value;
            onPropertyChanged(new PropertyChangedEventArgs("Name")); //$NON-NLS-1$
        }
    }

    private String networkName;

    public String getNetworkName()
    {
        return networkName;
    }

    public void setNetworkName(String value)
    {
        if (!ObjectUtils.objectsEqual(networkName, value))
        {
            networkName = value;
            onPropertyChanged(new PropertyChangedEventArgs("NetworkName")); //$NON-NLS-1$
        }
    }

    private String address;

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String value)
    {
        if (!ObjectUtils.objectsEqual(address, value))
        {
            address = value;
            onPropertyChanged(new PropertyChangedEventArgs("Address")); //$NON-NLS-1$
        }
    }

}
