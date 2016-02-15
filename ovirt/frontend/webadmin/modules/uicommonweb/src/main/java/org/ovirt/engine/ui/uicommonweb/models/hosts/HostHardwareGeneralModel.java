package org.ovirt.engine.ui.uicommonweb.models.hosts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;
import org.ovirt.engine.ui.uicompat.UIMessages;

@SuppressWarnings("unused")
public class HostHardwareGeneralModel extends EntityModel
{
    private static final UIConstants constants = ConstantsManager.getInstance().getConstants();
    private static final UIMessages messages = ConstantsManager.getInstance().getMessages();

    @Override
    public VDS getEntity()
    {
        return (VDS) super.getEntity();
    }

    private String hardwareManufacturer;

    public String getHardwareManufacturer()
    {
        return hardwareManufacturer;
    }

    public void setHardwareManufacturer(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareManufacturer, value))
        {
            hardwareManufacturer = value;
            onPropertyChanged(new PropertyChangedEventArgs("manufacturer")); //$NON-NLS-1$
        }
    }

    private String hardwareProductName;

    public String getHardwareProductName()
    {
        return hardwareProductName;
    }

    public void setHardwareProductName(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareProductName, value))
        {
            hardwareProductName = value;
            onPropertyChanged(new PropertyChangedEventArgs("productName")); //$NON-NLS-1$
        }
    }

    private String hardwareSerialNumber;

    public String getHardwareSerialNumber()
    {
        return hardwareSerialNumber;
    }

    public void setHardwareSerialNumber(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareSerialNumber, value))
        {
            hardwareSerialNumber = value;
            onPropertyChanged(new PropertyChangedEventArgs("serialNumber")); //$NON-NLS-1$
        }
    }

    private String hardwareVersion;

    public String getHardwareVersion()
    {
        return hardwareVersion;
    }

    public void setHardwareVersion(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareVersion, value))
        {
            hardwareVersion = value;
            onPropertyChanged(new PropertyChangedEventArgs("hardwareVersion")); //$NON-NLS-1$
        }
    }

    private String hardwareUUID;

    public String getHardwareUUID()
    {
        return hardwareUUID;
    }

    public void setHardwareUUID(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareUUID, value))
        {
            hardwareUUID = value;
            onPropertyChanged(new PropertyChangedEventArgs("uuid")); //$NON-NLS-1$
        }
    }

    private String hardwareFamily;

    public String getHardwareFamily()
    {
        return hardwareFamily;
    }

    @SuppressWarnings("deprecation")
    public void setHardwareFamily(String value)
    {
        if (!ObjectUtils.objectsEqual(hardwareFamily, value))
        {
            hardwareFamily = value;
            onPropertyChanged(new PropertyChangedEventArgs("family")); //$NON-NLS-1$
        }
    }


    private String cpuType;

    public String getCpuType()
    {
        return cpuType;
    }

    public void setCpuType(String value)
    {
        if (!ObjectUtils.objectsEqual(cpuType, value)) {
            cpuType = value;
            onPropertyChanged(new PropertyChangedEventArgs("CpuType")); //$NON-NLS-1$
        }
    }

    private String cpuModel;

    public String getCpuModel()
    {
        return cpuModel;
    }

    public void setCpuModel(String value)
    {
        if (!ObjectUtils.objectsEqual(cpuModel, value)) {
            cpuModel = value;
            onPropertyChanged(new PropertyChangedEventArgs("CpuModel")); //$NON-NLS-1$
        }
    }

    private Integer numberOfSockets;

    public Integer getNumberOfSockets()
    {
        return numberOfSockets;
    }

    public void setNumberOfSockets(Integer value)
    {
        if (numberOfSockets == null && value == null) {
            return;
        } if (numberOfSockets == null || !numberOfSockets.equals(value)) {
            numberOfSockets = value;
            onPropertyChanged(new PropertyChangedEventArgs("NumberOfSockets")); //$NON-NLS-1$
        }
    }

    private String coresPerSocket;

    public String getCoresPerSocket()
    {
        return coresPerSocket;
    }

    public void setCoresPerSocket(String value)
    {
        if (coresPerSocket == null && value == null) {
            return;
        } if (coresPerSocket == null || !coresPerSocket.equals(value)) {
            coresPerSocket = value;
            onPropertyChanged(new PropertyChangedEventArgs("CoresPerSocket")); //$NON-NLS-1$
        }
    }

    private String threadsPerCore;

    public String getThreadsPerCore()
    {
        return threadsPerCore;
    }

    public void setThreadsPerCore(String value)
    {
        if (threadsPerCore == null && value == null) {
            return;
        } if (threadsPerCore == null || !threadsPerCore.equals(value)) {
            threadsPerCore = value;
            onPropertyChanged(new PropertyChangedEventArgs("ThreadsPerCore")); //$NON-NLS-1$
        }
    }

    public enum HbaDeviceKeys { MODEL_NAME, // Model name field
                                TYPE,       // Device type
                                WWNN,       // WWNN of the NIC
                                WWNPS       // Comma separated list of WWNPs (port ids)
    };

    private List<EnumMap<HbaDeviceKeys, String>> hbaDevices;

    public List<EnumMap<HbaDeviceKeys, String>> getHbaDevices() {
        return hbaDevices;
    }

    public void setHbaDevices(List<EnumMap<HbaDeviceKeys, String>> hbaDevices) {
        this.hbaDevices = hbaDevices;
    }

    public HostHardwareGeneralModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().generalTitle());
        setHelpTag(HelpTag.hardware);
        setHashName("hardware"); //$NON-NLS-1$
        setAvailableInModes(ApplicationMode.VirtOnly);
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);
    }

    private void updateProperties()
    {
        VDS vds = getEntity();
        setHardwareManufacturer(vds.getHardwareManufacturer());
        setHardwareVersion(vds.getHardwareVersion());
        setHardwareProductName(vds.getHardwareProductName());
        setHardwareUUID(vds.getHardwareUUID());
        setHardwareSerialNumber(vds.getHardwareSerialNumber());
        setHardwareFamily(vds.getHardwareFamily());

        setCpuType(vds.getCpuName() != null ? vds.getCpuName().getCpuName() : null);
        setCpuModel(vds.getCpuModel());
        setNumberOfSockets(vds.getCpuSockets());

        if (vds.getCpuCores() != null && vds.getCpuSockets() != null
                && vds.getCpuThreads() != null && vds.getCpuSockets() != 0) {
            int coresPerSocket = vds.getCpuCores() / vds.getCpuSockets();


            String fieldValue = String.valueOf(coresPerSocket);
            if (vds.getCountThreadsAsCores()) {
                fieldValue = ConstantsManager.getInstance().getMessages()
                        .threadsAsCoresPerSocket(coresPerSocket, vds.getCpuThreads() / vds.getCpuSockets());
            }

            setCoresPerSocket(fieldValue);
        } else {
            setCoresPerSocket(null);
        }

        if (vds.getVdsGroupCompatibilityVersion() != null
                && Version.v3_2.compareTo(vds.getVdsGroupCompatibilityVersion()) > 0) {
            // Members of pre-3.2 clusters don't support SMT; here we act like a 3.1 engine
            setThreadsPerCore(constants.unsupported());
        } else if (vds.getCpuThreads() == null || vds.getCpuCores() == null || vds.getCpuCores() == 0) {
            setThreadsPerCore(constants.unknown());
        } else {
            Integer threads = vds.getCpuThreads() / vds.getCpuCores();
            setThreadsPerCore(messages.commonMessageWithBrackets(threads.toString(), threads > 1 ? constants.smtEnabled()
                    : constants.smtDisabled()));
        }

        /* Go through the list of HBA devices and transfer the necessary info
           to the GWT host hardware model */
        List<EnumMap<HbaDeviceKeys, String>> hbaDevices = new ArrayList<EnumMap<HbaDeviceKeys, String>>();
        List<Map<String, String>> fcDevices = vds.getHBAs().get("FC"); //$NON-NLS-1$

        if (fcDevices != null) {
            for (Map<String, String> device: fcDevices) {
                EnumMap<HbaDeviceKeys, String> deviceModel = new EnumMap<HbaDeviceKeys, String>(HbaDeviceKeys.class);
                deviceModel.put(HbaDeviceKeys.MODEL_NAME, device.get("model")); //$NON-NLS-1$
                deviceModel.put(HbaDeviceKeys.WWNN, device.get("wwnn")); //$NON-NLS-1$
                deviceModel.put(HbaDeviceKeys.WWNPS, device.get("wwpn")); //$NON-NLS-1$
                deviceModel.put(HbaDeviceKeys.TYPE, "FC"); //$NON-NLS-1$

                hbaDevices.add(deviceModel);
            }
        }

        setHbaDevices(hbaDevices);
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        if (getEntity() != null)
        {
            updateProperties();
        }
    }
}
