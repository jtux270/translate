package org.ovirt.engine.core.utils.ovf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.SerialNumberPolicy;
import org.ovirt.engine.core.common.businessentities.SsoMethod;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmInit;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.VmDeviceCommonUtils;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.utils.VmInitUtils;
import org.ovirt.engine.core.utils.customprop.DevicePropertiesUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.ovf.xml.XmlDocument;
import org.ovirt.engine.core.utils.ovf.xml.XmlNamespaceManager;
import org.ovirt.engine.core.utils.ovf.xml.XmlNode;
import org.ovirt.engine.core.utils.ovf.xml.XmlNodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class OvfReader implements IOvfBuilder {
    protected OsRepository osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);
    protected ArrayList<DiskImage> _images;
    protected ArrayList<VmNetworkInterface> interfaces;
    protected XmlDocument _document;
    protected XmlNamespaceManager _xmlNS;
    private static final int BYTES_IN_GB = 1024 * 1024 * 1024;
    public static final String EmptyName = "[Empty Name]";
    protected String name = EmptyName;
    private String version;
    private final VmBase vmBase;
    private DisplayType defaultDisplayType;

    public OvfReader(XmlDocument document, ArrayList<DiskImage> images, ArrayList<VmNetworkInterface> interfaces, VmBase vmBase) {
        _images = images;
        this.interfaces = interfaces;
        _document = document;
        this.vmBase = vmBase;

        _xmlNS = new XmlNamespaceManager();
        _xmlNS.addNamespace("ovf", OVF_URI);
        _xmlNS.addNamespace("rasd", RASD_URI);
        _xmlNS.addNamespace("vssd", VSSD_URI);
        _xmlNS.addNamespace("xsi", XSI_URI);
        readHeader();

    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * reads the OVF header
     */
    private void readHeader() {
        version = "";
        XmlNode node = _document.SelectSingleNode("//ovf:Envelope", _xmlNS);
        if (node != null) {
            version = node.attributes.get("ovf:version").getValue();
        }
    }

    @Override
    public void buildReference() {
        buildImageReference();
        buildNicReference();
    }

    @Override
    public void buildNetwork() {
        // No implementation - networks aren't read from the OVF.
    }

    protected long convertGigabyteToBytes(long gb) {
        return gb * BYTES_IN_GB;
    }

    @Override
    public void buildDisk() {
        XmlNodeList list = _document.SelectNodes("//*/Section/Disk");
        for (XmlNode node : list) {
            final Guid guid = new Guid(node.attributes.get("ovf:diskId").getValue());

            DiskImage image = LinqUtils.firstOrNull(_images, new Predicate<DiskImage>() {
                @Override
                public boolean eval(DiskImage diskImage) {
                    return diskImage.getImageId().equals(guid);
                }
            });

            if (node.attributes.get("ovf:vm_snapshot_id") != null) {
                image.setVmSnapshotId(new Guid(node.attributes.get("ovf:vm_snapshot_id").getValue()));
            }

            if (!StringUtils.isEmpty(node.attributes.get("ovf:size").getValue())) {
                image.setSize(convertGigabyteToBytes(Long.parseLong(node.attributes.get("ovf:size").getValue())));
            }
            if (!StringUtils.isEmpty(node.attributes.get("ovf:actual_size").getValue())) {
                image.setActualSizeInBytes(convertGigabyteToBytes(Long.parseLong(node.attributes.get("ovf:actual_size").getValue())));
            }
            if (node.attributes.get("ovf:volume-format") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:volume-format").getValue())) {
                    image.setvolumeFormat(VolumeFormat.valueOf(node.attributes.get("ovf:volume-format").getValue()));
                } else {
                    image.setvolumeFormat(VolumeFormat.Unassigned);
                }
            }
            else {
                image.setvolumeFormat(VolumeFormat.Unassigned);
            }
            if (node.attributes.get("ovf:volume-type") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:volume-type").getValue())) {
                    image.setVolumeType(VolumeType.valueOf(node.attributes.get("ovf:volume-type").getValue()));
                } else {
                    image.setVolumeType(VolumeType.Unassigned);
                }
            }
            else {
                image.setVolumeType(VolumeType.Unassigned);
            }
            if (node.attributes.get("ovf:disk-interface") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:disk-interface").getValue())) {
                    image.setDiskInterface(DiskInterface.valueOf(node.attributes.get("ovf:disk-interface").getValue()));
                }
            }
            else {
                image.setDiskInterface(DiskInterface.IDE);
            }
            if (node.attributes.get("ovf:boot") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:boot").getValue())) {
                    image.setBoot(Boolean.parseBoolean(node.attributes.get("ovf:boot").getValue()));
                }
            }
            if (node.attributes.get("ovf:wipe-after-delete") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:wipe-after-delete").getValue())) {
                    image.setWipeAfterDelete(Boolean.parseBoolean(node.attributes.get("ovf:wipe-after-delete")
                            .getValue()));
                }
            }
            if (node.attributes.get("ovf:disk-alias") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:disk-alias").getValue())) {
                    image.setDiskAlias(String.valueOf(node.attributes.get("ovf:disk-alias")
                            .getValue()));
                }
            }
            if (node.attributes.get("ovf:disk-description") != null) {
                if (!StringUtils.isEmpty(node.attributes.get("ovf:disk-description").getValue())) {
                    image.setDiskDescription(String.valueOf(node.attributes.get("ovf:disk-description")
                            .getValue()));
                }
            }
        }
    }

    @Override
    public void buildVirtualSystem() {
        readGeneralData();
    }

    protected VmDevice readManagedVmDevice(XmlNode node, Guid deviceId) {
        return addManagedVmDevice(readVmDevice(node, deviceId));
    }

    private VmDevice addManagedVmDevice(VmDevice vmDevice) {
        vmDevice.setIsManaged(true);
        vmBase.getManagedDeviceMap().put(vmDevice.getDeviceId(), vmDevice);
        return vmDevice;
    }

    protected VmDevice readUnmanagedVmDevice(XmlNode node, Guid deviceId) {
        VmDevice vmDevice = readVmDevice(node, deviceId);
        vmBase.getUnmanagedDeviceList().add(vmDevice);
        return vmDevice;
    }

    /**
     * Reads vm device attributes from OVF and stores it in the collection
     *
     * @param node
     * @param deviceId
     */
    private VmDevice readVmDevice(XmlNode node, Guid deviceId) {
        VmDevice vmDevice = new VmDevice();
        vmDevice.setId(new VmDeviceId(deviceId, vmBase.getId()));
        if (node.SelectSingleNode(OvfProperties.VMD_ADDRESS, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_ADDRESS, _xmlNS).innerText)) {
            vmDevice.setAddress(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_ADDRESS, _xmlNS).innerText));
        } else {
            vmDevice.setAddress("");
        }
        if (node.SelectSingleNode(OvfProperties.VMD_ALIAS, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_ALIAS, _xmlNS).innerText)) {
            vmDevice.setAlias(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_ALIAS, _xmlNS).innerText));
        } else {
            vmDevice.setAlias("");
        }
        XmlNode specParamsNode = node.SelectSingleNode(OvfProperties.VMD_SPEC_PARAMS, _xmlNS);
        if (specParamsNode != null
                && !StringUtils.isEmpty(specParamsNode.innerText)) {
            vmDevice.setSpecParams(getMapNode(specParamsNode));
        } else {
            // Empty map
            vmDevice.setSpecParams(Collections.<String, Object>emptyMap());
        }
        if (node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS).innerText)) {
            vmDevice.setType(VmDeviceGeneralType.forValue(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS).innerText)));
        } else {
            int resourceType = getResourceType(node, OvfProperties.VMD_RESOURCE_TYPE);
            vmDevice.setType(VmDeviceGeneralType.forValue(VmDeviceType.getoVirtDevice(resourceType)));
        }
        if (node.SelectSingleNode(OvfProperties.VMD_DEVICE, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_DEVICE, _xmlNS).innerText)) {
            vmDevice.setDevice(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_DEVICE, _xmlNS).innerText));
        } else {
            setDeviceByResource(node, vmDevice);
        }
        if (node.SelectSingleNode(OvfProperties.VMD_BOOT_ORDER, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_BOOT_ORDER, _xmlNS).innerText)) {
            vmDevice.setBootOrder(Integer.valueOf(node.SelectSingleNode(OvfProperties.VMD_BOOT_ORDER, _xmlNS).innerText));
        } else {
            vmDevice.setBootOrder(0);
        }
        if (node.SelectSingleNode(OvfProperties.VMD_IS_PLUGGED, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_IS_PLUGGED, _xmlNS).innerText)) {
            vmDevice.setIsPlugged(Boolean.valueOf(node.SelectSingleNode(OvfProperties.VMD_IS_PLUGGED, _xmlNS).innerText));
        } else {
            vmDevice.setIsPlugged(Boolean.TRUE);
        }
        if (node.SelectSingleNode(OvfProperties.VMD_IS_READONLY, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_IS_READONLY, _xmlNS).innerText)) {
            vmDevice.setIsReadOnly(Boolean.valueOf(node.SelectSingleNode(OvfProperties.VMD_IS_READONLY, _xmlNS).innerText));
        } else {
            vmDevice.setIsReadOnly(Boolean.FALSE);
        }
        if (node.SelectSingleNode(OvfProperties.VMD_CUSTOM_PROP, _xmlNS) != null
                && StringUtils.isNotEmpty(node.SelectSingleNode(OvfProperties.VMD_CUSTOM_PROP, _xmlNS).innerText)) {
            vmDevice.setCustomProperties(DevicePropertiesUtils.getInstance().convertProperties(
                    String.valueOf(node.SelectSingleNode(OvfProperties.VMD_CUSTOM_PROP, _xmlNS).innerText)));
        } else {
            vmDevice.setCustomProperties(null);
        }

        if (node.SelectSingleNode(OvfProperties.VMD_SNAPSHOT_PROP, _xmlNS) != null
                && StringUtils.isNotEmpty(node.SelectSingleNode(OvfProperties.VMD_SNAPSHOT_PROP, _xmlNS).innerText)) {
            vmDevice.setSnapshotId(new Guid(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_CUSTOM_PROP, _xmlNS).innerText)));
        }

        return vmDevice;
    }

    /**
     * gets the VM interface
     *
     * @param node
     *            the xml node
     * @return VmNetworkInterface
     */
    public VmNetworkInterface getNetworkInterface(XmlNode node) {
        // prior to 3.0 the instanceId is int , in 3.1 and on this is Guid
        String str = node.SelectSingleNode("rasd:InstanceId", _xmlNS).innerText;
        final Guid guid;
        VmNetworkInterface iface;
        if (!StringUtils.isNumeric(str)) { // 3.1 and above OVF format
            guid = new Guid(str);
            iface = LinqUtils.firstOrNull(interfaces, new Predicate<VmNetworkInterface>() {
                @Override
                public boolean eval(VmNetworkInterface iface) {
                    return iface.getId().equals(guid);
                }
            });
            if (iface == null) {
                iface = new VmNetworkInterface();
                iface.setId(guid);
            }
        } else { // 3.0 and below OVF format
            iface = new VmNetworkInterface();
        }
        return iface;
    }

    /**
     * This method should return the string representation of 'default display type'
     * property in the ovf file. this representation is different for ovf file
     * of VM and ovf file of template.
     *
     * @return the String representation of 'default display type' property in the ovf file
     */
    protected abstract String getDefaultDisplayTypeStringRepresentation();

    protected abstract void readOsSection(XmlNode section);

    protected void readHardwareSection(XmlNode section) {
        boolean readVirtioSerial = false;

        for (XmlNode node : section.SelectNodes("Item")) {
            switch (node.SelectSingleNode("rasd:ResourceType", _xmlNS).innerText) {
            case OvfHardware.CPU:
                readCpuItem(node);
                break;

            case OvfHardware.Memory:
                readMemoryItem(node);
                break;

            case OvfHardware.DiskImage:
                readDiskImageItem(node);
                break;

            case OvfHardware.Network:
                readNetworkItem(node);
                break;

            case OvfHardware.USB:
                readUsbItem(node);
                break;

            case OvfHardware.Monitor:
                readMonitorItem(node);
                break;

            case OvfHardware.Graphics:
                readManagedVmDevice(node, Guid.newGuid()); // so far graphics doesn't contain anything special
                break;

            case OvfHardware.CD:
                readCdItem(node);
                break;

            case OvfHardware.OTHER:
                VmDevice vmDevice = readOtherHardwareItem(node);
                readVirtioSerial = readVirtioSerial ||
                        VmDeviceType.VIRTIOSERIAL.getName().equals(vmDevice.getDevice());
                break;
            }
        }

        if (!readVirtioSerial) {
            addManagedVmDevice(VmDeviceCommonUtils.createVirtioSerialDeviceForVm(vmBase.getId()));
        }
    }

    protected abstract void readDiskImageItem(XmlNode node);

    protected void readMonitorItem(XmlNode node) {
        vmBase.setNumOfMonitors(
                Integer.parseInt(node.SelectSingleNode("rasd:VirtualQuantity", _xmlNS).innerText));
        if (node.SelectSingleNode("rasd:SinglePciQxl", _xmlNS) != null) {
            vmBase.setSingleQxlPci(Boolean.parseBoolean(node.SelectSingleNode("rasd:SinglePciQxl", _xmlNS).innerText));
        }

        if (new Version(getVersion()).compareTo(Version.v3_1) >= 0) {
            readManagedVmDevice(node, Guid.newGuid());
        } else {
            // before v3.1 we had just one monitor item for all the monitors so in this
            // case we need to add monitor devices according to the numOfMonitors field
            for (int i=0; i<vmBase.getNumOfMonitors(); ++i) {
                readManagedVmDevice(node, Guid.newGuid());
            }
        }
    }

    private void readCpuItem(XmlNode node) {
        vmBase.setNumOfSockets(
                Integer.parseInt(node.SelectSingleNode("rasd:num_of_sockets", _xmlNS).innerText));
        vmBase.setCpuPerSocket(
                Integer.parseInt(node.SelectSingleNode("rasd:cpu_per_socket", _xmlNS).innerText));
    }

    private void readMemoryItem(XmlNode node) {
        vmBase.setMemSizeMb(
                Integer.parseInt(node.SelectSingleNode("rasd:VirtualQuantity", _xmlNS).innerText));
    }

    private void readCdItem(XmlNode node) {
        readManagedVmDevice(node, Guid.newGuid());
    }

    private void readNetworkItem(XmlNode node) {
        VmNetworkInterface iface = getNetworkInterface(node);
        updateSingleNic(node, iface);
        vmBase.getInterfaces().add(iface);
        readManagedVmDevice(node, iface.getId());
    }

    private void readUsbItem(XmlNode node) {
        vmBase.setUsbPolicy(
                UsbPolicy.forStringValue(node.SelectSingleNode("rasd:UsbPolicy", _xmlNS).innerText));
    }

    private VmDevice readOtherHardwareItem(XmlNode node) {
        boolean managed = false;
        if (node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS) != null
                && StringUtils.isNotEmpty(node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS).innerText)) {
            VmDeviceGeneralType type = VmDeviceGeneralType.forValue(String.valueOf(node.SelectSingleNode(OvfProperties.VMD_TYPE, _xmlNS).innerText));
            String device = node.SelectSingleNode(OvfProperties.VMD_DEVICE, _xmlNS).innerText;
            // special devices are treated as managed devices but still have the OTHER OVF ResourceType
            managed = VmDeviceCommonUtils.isSpecialDevice(device, type);
        }

        return managed ?
                readManagedVmDevice(node, Guid.newGuid())
                : readUnmanagedVmDevice(node, Guid.newGuid());
    }

    protected void readGeneralData() {
        XmlNode content = _document.SelectSingleNode("//*/Content");
        XmlNode node;
        vmBase.setVmInit(new VmInit());

        // set ovf version to the ovf object
        vmBase.setOvfVersion(getVersion());

        node = content.SelectSingleNode(OvfProperties.DESCRIPTION);
        if (node != null) {
            vmBase.setDescription(node.innerText);
        }

        node = content.SelectSingleNode(OvfProperties.COMMENT);
        if (node != null) {
            vmBase.setComment(node.innerText);
        }

        node = content.SelectSingleNode(OvfProperties.DOMAIN);
        if (node != null) {
            vmBase.getVmInit().setDomain(node.innerText);
        }

        node = content.SelectSingleNode(OvfProperties.CREATION_DATE);
        if (node != null) {
            Date creationDate = OvfParser.UtcDateStringToLocaDate(node.innerText);
            if (creationDate != null) {
                vmBase.setCreationDate(creationDate);
            }
        }

        node = content.SelectSingleNode(OvfProperties.EXPORT_DATE);
        if (node != null) {
            Date exportDate = OvfParser.UtcDateStringToLocaDate(node.innerText);
            if (exportDate != null) {
                vmBase.setExportDate(exportDate);
            }
        }

        node = content.SelectSingleNode(OvfProperties.DEFAULT_BOOT_SEQUENCE);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setDefaultBootSequence(BootSequence.forValue(Integer.parseInt(node.innerText)));
            }
        }

        node = content.SelectSingleNode(OvfProperties.INITRD_URL);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setInitrdUrl(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.KERNEL_URL);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setKernelUrl(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.KERNEL_PARAMS);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setKernelParams(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.GENERATION);
        if (node != null) {
            vmBase.setDbGeneration(Long.parseLong(node.innerText));
        } else {
            vmBase.setDbGeneration(1L);
        }

        // Note: the fetching of 'default display type' should happen before reading
        // the hardware section
        node = content.SelectSingleNode(getDefaultDisplayTypeStringRepresentation());
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                defaultDisplayType = DisplayType.forValue(Integer.parseInt(node.innerText));
                vmBase.setDefaultDisplayType(defaultDisplayType);
            }
        }

        XmlNodeList list = content.SelectNodes("Section");

        Version version = new Version(getVersion());
        if (list != null) {
            // The Os need to be read before the hardware
            node = getNode(list, "xsi:type", "ovf:OperatingSystemSection_Type");
            if (node != null) {
                readOsSection(node);
                if (!osRepository.isLinux(vmBase.getOsId())
                        || !FeatureSupported.singleQxlPci(version)
                        || vmBase.getDefaultDisplayType() != DisplayType.qxl) {
                    vmBase.setSingleQxlPci(false);
                }
            }

            node = getNode(list, "xsi:type", "ovf:VirtualHardwareSection_Type");
            if (node != null) {
                readHardwareSection(node);
            }

            node = getNode(list, "xsi:type", "ovf:SnapshotsSection_Type");
            if (node != null) {
                readSnapshotsSection(node);
            }
        }

        // due to depndency on vmBase.getOsId() must be read AFTER readOsSection
        node = content.SelectSingleNode(OvfProperties.TIMEZONE);
        if (node != null && StringUtils.isNotEmpty(node.innerText)) {
            vmBase.setTimeZone(node.innerText);
        } else {
            if (osRepository.isWindows(vmBase.getOsId())) {
                vmBase.setTimeZone(Config.<String> getValue(ConfigValues.DefaultWindowsTimeZone));
            } else {
                vmBase.setTimeZone(Config.<String> getValue(ConfigValues.DefaultGeneralTimeZone));
            }
        }

        node = content.SelectSingleNode(OvfProperties.ORIGIN);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setOrigin(OriginType.forValue(Integer.parseInt(node.innerText)));
            }
        }

        node = content.SelectSingleNode(OvfProperties.VM_TYPE);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setVmType(VmType.forValue(Integer.parseInt(node.innerText)));
            }
        }

        node = content.SelectSingleNode(OvfProperties.IS_SMARTCARD_ENABLED);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setSmartcardEnabled(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.NUM_OF_IOTHREADS);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText) && !FeatureSupported.isIoThreadsSupported(version)) {
                vmBase.setNumOfIoThreads(Integer.parseInt(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.DELETE_PROTECTED);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setDeleteProtected(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.SSO_METHOD);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setSsoMethod(SsoMethod.fromString(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.TUNNEL_MIGRATION);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setTunnelMigration(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.VNC_KEYBOARD_LAYOUT);
        if (node != null) {
            if (!StringUtils.isEmpty(node.innerText)) {
                vmBase.setVncKeyboardLayout(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.MIN_ALLOCATED_MEMORY);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setMinAllocatedMem(Integer.parseInt(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.IS_STATELESS);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setStateless(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.IS_RUN_AND_PAUSE);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setRunAndPause(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.CREATED_BY_USER_ID);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setCreatedByUserId(Guid.createGuidFromString(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.MIGRATION_DOWNTIME);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setMigrationDowntime(Integer.parseInt(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.MIGRATION_SUPPORT);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                MigrationSupport migrationSupport = MigrationSupport.forValue(Integer.parseInt(node.innerText));
                vmBase.setMigrationSupport(migrationSupport);
            }
        }

        // TODO dedicated to multiple hosts
        readDedicatedHostsList();

        node = content.SelectSingleNode(OvfProperties.SERIAL_NUMBER_POLICY);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setSerialNumberPolicy(SerialNumberPolicy.forValue(Integer.parseInt(node.innerText)));
            }
        }

        node = content.SelectSingleNode(OvfProperties.CUSTOM_SERIAL_NUMBER);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setCustomSerialNumber(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.AUTO_STARTUP);
        if (node != null) {
            vmBase.setAutoStartup(Boolean.parseBoolean(node.innerText));
        }

        node = content.SelectSingleNode(OvfProperties.PRIORITY);
        if (node != null) {
            vmBase.setPriority(Integer.parseInt(node.innerText));
        }

        node = content.SelectSingleNode(OvfProperties.IS_BOOT_MENU_ENABLED);
        if (node != null) {
            vmBase.setBootMenuEnabled(Boolean.parseBoolean(node.innerText));
        }

        node = content.SelectSingleNode(OvfProperties.IS_SPICE_FILE_TRANSFER_ENABLED);
        if (node != null) {
            vmBase.setSpiceFileTransferEnabled(Boolean.parseBoolean(node.innerText));
        }

        node = content.SelectSingleNode(OvfProperties.IS_SPICE_COPY_PASTE_ENABLED);
        if (node != null) {
            vmBase.setSpiceCopyPasteEnabled(Boolean.parseBoolean(node.innerText));
        }

        node = content.SelectSingleNode(OvfProperties.IS_AUTO_CONVERGE);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setAutoConverge(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.IS_MIGRATE_COMPRESSED);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setMigrateCompressed(Boolean.parseBoolean(node.innerText));
            }
        }

        node = content.SelectSingleNode(OvfProperties.CUSTOM_EMULATED_MACHINE);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setCustomEmulatedMachine(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.CUSTOM_CPU_NAME);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setCustomCpuName(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.PREDEFINED_PROPERTIES);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setPredefinedProperties(node.innerText);
            }
        }

        node = content.SelectSingleNode(OvfProperties.USER_DEFINED_PROPERTIES);
        if (node != null) {
            if (StringUtils.isNotEmpty(node.innerText)) {
                vmBase.setUserDefinedProperties(node.innerText);
            }
        }

        vmBase.setCustomProperties(VmPropertiesUtils.getInstance().customProperties(
                vmBase.getPredefinedProperties(), vmBase.getUserDefinedProperties()));

        readGeneralData(content);

        readVmInit(content);
    }

    private void readDedicatedHostsList() {
        vmBase.setDedicatedVmForVdsList(new LinkedList<Guid>()); // initialize to empty list
        // search all dedicated hosts with xPath
        XmlNodeList hostsList = _document.SelectNodes("//*/Content/" + OvfProperties.DEDICATED_VM_FOR_VDS);
        for (XmlNode hostNode : hostsList) {
            if (hostNode != null && StringUtils.isNotEmpty(hostNode.innerText)) {
                vmBase.getDedicatedVmForVdsList().add(Guid.createGuidFromString(hostNode.innerText));
            }
        }
    }

    private void readVmInit(XmlNode content) {
        XmlNode node = content.SelectSingleNode("VmInit");
        VmInit vmInit = vmBase.getVmInit();
        vmInit.setId(vmBase.getId());
        if (node != null) {
            if (node.attributes.get("ovf:hostname") != null) {
                vmInit.setHostname(node.attributes.get("ovf:hostname").getValue());
            }
            if (node.attributes.get("ovf:domain") != null) {
                vmInit.setDomain(node.attributes.get("ovf:domain").getValue());
            }
            if (node.attributes.get("ovf:timeZone") != null) {
                vmInit.setTimeZone(node.attributes.get("ovf:timeZone").getValue());
            }
            if (node.attributes.get("ovf:authorizedKeys") != null) {
                vmInit.setAuthorizedKeys(node.attributes.get("ovf:authorizedKeys").getValue());
            }
            if (node.attributes.get("ovf:regenerateKeys") != null) {
                vmInit.setRegenerateKeys(Boolean.parseBoolean(node.attributes.get("ovf:regenerateKeys").getValue()));
            }
            if (node.attributes.get("ovf:dnsServers") != null) {
                vmInit.setDnsServers(node.attributes.get("ovf:dnsServers").getValue());
            }
            if (node.attributes.get("ovf:dnsSearch") != null) {
                vmInit.setDnsSearch(node.attributes.get("ovf:dnsSearch").getValue());
            }
            if (node.attributes.get("ovf:networks") != null) {
                vmInit.setNetworks(VmInitUtils.jsonNetworksToList(node.attributes.get("ovf:networks").getValue()));
            }
            if (node.attributes.get("ovf:winKey") != null) {
                vmInit.setWinKey(node.attributes.get("ovf:winKey").getValue());
            }
            if (node.attributes.get("ovf:rootPassword") != null) {
                vmInit.setRootPassword(node.attributes.get("ovf:rootPassword").getValue());
            }
            if (node.attributes.get("ovf:customScript") != null) {
                vmInit.setCustomScript(node.attributes.get("ovf:customScript").getValue());
            }
        }
    }

    private XmlNode getNode(XmlNodeList nodeList, String attributeName, String attributeValue) {

        for (XmlNode section : nodeList) {
            String value = section.attributes.get(attributeName).getValue();

            if (value.equals(attributeValue)) {
                return section;
            }
        }

        return null;
    }

    protected void readSnapshotsSection(@SuppressWarnings("unused") XmlNode section) {
        // The snapshot section only has meaning for VMs, and is overridden in OvfVmReader.
    }

    protected abstract void readGeneralData(XmlNode content);

    protected void buildNicReference() {
        XmlNodeList list = _document.SelectNodes("//*/Nic", _xmlNS);
        for (XmlNode node : list) {
            VmNetworkInterface iface = new VmNetworkInterface();
            iface.setId(new Guid(node.attributes.get("ovf:id").getValue()));
            interfaces.add(iface);
        }
        if (!list.iterator().hasNext()) {
            StringBuilder sb = new StringBuilder();
            sb.append("//*/Item[");
            sb.append(OvfProperties.VMD_RESOURCE_TYPE);
            sb.append("=");
            sb.append(OvfHardware.Network);
            sb.append("]");
            list = _document.SelectNodes(sb.toString(), _xmlNS);
            for (XmlNode node : list) {
                VmNetworkInterface iface = new VmNetworkInterface();
                iface.setId(Guid.newGuid());
                updateSingleNic(node, iface);
                interfaces.add(iface);
            }
        }
    }

    protected void updateSingleNic(XmlNode node, VmNetworkInterface iface) {
        String networkName = node.SelectSingleNode(OvfProperties.VMD_CONNECTION, _xmlNS).innerText;
        iface.setNetworkName(StringUtils.defaultIfEmpty(networkName, null));

        XmlNode vnicProfileNameNode = node.SelectSingleNode(OvfProperties.VMD_VNIC_PROFILE_NAME, _xmlNS);
        iface.setVnicProfileName(vnicProfileNameNode == null ? null
                : StringUtils.defaultIfEmpty(vnicProfileNameNode.innerText, null));

        XmlNode linkedNode = node.SelectSingleNode(OvfProperties.VMD_LINKED, _xmlNS);
        iface.setLinked(linkedNode == null ? true : Boolean.valueOf(linkedNode.innerText));

        iface.setName(node.SelectSingleNode(OvfProperties.VMD_NAME, _xmlNS).innerText);

        String resourceSubType = node.SelectSingleNode("rasd:ResourceSubType", _xmlNS).innerText;
        if (StringUtils.isNotEmpty(resourceSubType)) {
            iface.setType(Integer.parseInt(resourceSubType));
        }

        XmlNode speed = node.SelectSingleNode("rasd:speed", _xmlNS);
        iface.setSpeed((speed != null) ? Integer.parseInt(speed.innerText) : VmInterfaceType.forValue(iface.getType())
                .getSpeed());

    }

    private void buildImageReference() {
        XmlNodeList list = _document.SelectNodes("//*/File", _xmlNS);
        for (XmlNode node : list) {
            // If the disk storage type is Cinder then override the disk image with Cinder object, otherwise use the disk image.
            DiskImage disk = new DiskImage();

            // If the OVF is old and does not contain any storage type reference then we assume we can only have disk image.
            if (node.attributes.get("ovf:disk_storage_type") != null) {
                String diskStorageType = node.attributes.get("ovf:disk_storage_type").getValue();
                if (diskStorageType != null && diskStorageType.equals(DiskStorageType.CINDER.name())) {
                    disk = new CinderDisk();
                }
            }
            disk.setImageId(new Guid(node.attributes.get("ovf:id").getValue()));
            disk.setId(OvfParser.GetImageGrupIdFromImageFile(node.attributes.get("ovf:href").getValue()));
            // Default values:
            disk.setActive(true);
            disk.setImageStatus(ImageStatus.OK);
            disk.setDescription(node.attributes.get("ovf:description").getValue());
            _images.add(disk);
        }
    }

    private int getResourceType(XmlNode node, String resource) {
        if (node.SelectSingleNode(resource, _xmlNS) != null
                && !StringUtils.isEmpty(node.SelectSingleNode(resource, _xmlNS).innerText)) {
            return Integer.valueOf(node.SelectSingleNode(resource, _xmlNS).innerText);
        }
        return -1;
    }

    private void setDeviceByResource(XmlNode node, VmDevice vmDevice) {
        int resourceType = getResourceType(node, OvfProperties.VMD_RESOURCE_TYPE);
        int resourceSubType = getResourceType(node, OvfProperties.VMD_SUB_RESOURCE_TYPE);
        if (resourceSubType == -1) {
            // we need special handling for Monitor to define it as vnc or spice
            if (Integer.valueOf(OvfHardware.Monitor) == resourceType) {
                // if default display type is defined in the ovf, set the video device that is suitable for it
                if (defaultDisplayType != null) {
                    vmDevice.setDevice(defaultDisplayType.getDefaultVmDeviceType().getName());
                }
                else {
                    // get number of monitors from VirtualQuantity in OVF
                    if (node.SelectSingleNode(OvfProperties.VMD_VIRTUAL_QUANTITY, _xmlNS) != null
                            && !StringUtils.isEmpty(node.SelectSingleNode(OvfProperties.VMD_VIRTUAL_QUANTITY,
                                    _xmlNS).innerText)) {
                        int virtualQuantity =
                                Integer.valueOf(node.SelectSingleNode(OvfProperties.VMD_VIRTUAL_QUANTITY, _xmlNS).innerText);
                        if (virtualQuantity > 1) {
                            vmDevice.setDevice(VmDeviceType.QXL.getName());
                        } else {
                            // get first supported display device
                            List<Pair<GraphicsType, DisplayType>> supportedGraphicsAndDisplays =
                                    osRepository.getGraphicsAndDisplays(vmBase.getOsId(), new Version(getVersion()));
                            if (!supportedGraphicsAndDisplays.isEmpty()) {
                                DisplayType firstDisplayType = supportedGraphicsAndDisplays.get(0).getSecond();
                                vmDevice.setDevice(firstDisplayType.getDefaultVmDeviceType().getName());
                            } else {
                                vmDevice.setDevice(VmDeviceType.CIRRUS.getName());
                            }
                        }
                    } else { // default to spice if quantity not found
                        vmDevice.setDevice(VmDeviceType.QXL.getName());
                    }
                }
            } else {
                vmDevice.setDevice(VmDeviceType.getoVirtDevice(resourceType).getName());
            }
        } else if (Integer.valueOf(OvfHardware.Network) == resourceType) {
            // handle interfaces with different sub types : we have 0-5 as the VmInterfaceType enum
            VmInterfaceType nicType = VmInterfaceType.forValue(resourceSubType);
            if (nicType != null) {
                if (nicType == VmInterfaceType.pciPassthrough) {
                    vmDevice.setDevice(VmDeviceType.HOST_DEVICE.getName());
                } else {
                    vmDevice.setDevice(VmDeviceType.BRIDGE.getName());
                }
            } else {
                vmDevice.setDevice(VmDeviceType.getoVirtDevice(resourceType).getName());
            }
        }
    }

    private static Map<String, Object> getMapNode(XmlNode node) {
        Map<String, Object> returnValue = new HashMap<String, Object>();

        NodeList list = node.GetChildNodes();
        for (int index = 0; index < list.getLength(); ++index) {
            Node currNode = list.item(index);
            short nodeType = currNode.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                NodeList childNodes = currNode.getChildNodes();
                // If the element node has only one child, then it contains the value
                if (childNodes.getLength() == 1) {
                    Node valueNode = childNodes.item(0);
                    if (valueNode.getNodeType() == Node.TEXT_NODE) {
                        returnValue.put(currNode.getNodeName(), valueNode.getNodeValue());
                    }
                } else if (childNodes.getLength() > 1) {
                    // In this case, we have a nested map, so we parse it
                    returnValue.put(currNode.getNodeName(), getMapNode(new XmlNode(currNode)));
                }
            }
        }

        return returnValue;
    }

    @Override
    public String getStringRepresentation() {
        return _document.getOuterXml();
    }
}
