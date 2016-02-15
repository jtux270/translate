package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.common.vdscommands.GetDeviceListVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsBrokerCommand;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GetDeviceListVDSCommand<P extends GetDeviceListVDSCommandParameters> extends VdsBrokerCommand<P> {

    protected static final String DEVTYPE_VALUE_FCP = "fcp";
    protected static final String DEVTYPE_FIELD = "devtype";
    protected static final String STATUS = "status";

    /* Paths */
    protected static final String PATHSTATUS = "pathstatus";
    protected static final String LUN_FIELD = "lun";
    protected static final String DEVICE_ACTIVE_VALUE = "active";
    protected static final String DEVICE_STATE_FIELD = "state";
    protected static final String PHYSICAL_DEVICE_FIELD = "physdev";

    private LUNListReturnForXmlRpc _result;

    public GetDeviceListVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        int storageType = getParameters().getStorageType().getValue();
        _result = getBroker().getDeviceList(storageType);

        proceedProxyReturnValue();
        setReturnValue(parseLUNList(_result.lunList));
    }

    public static ArrayList<LUNs> parseLUNList(Map<String, Object>[] lunList) {
        ArrayList<LUNs> result = new ArrayList<LUNs>(lunList.length);
        for (Map<String, Object> xlun : lunList) {
            result.add(parseLunFromXmlRpc(xlun));
        }
        return result;
    }

    public static LUNs parseLunFromXmlRpc(Map<String, Object> xlun) {
        LUNs lun = new LUNs();
        if (xlun.containsKey("GUID")) {
            lun.setLUN_id(xlun.get("GUID").toString());
        }
        if (xlun.containsKey("pvUUID")) {
            lun.setphysical_volume_id(xlun.get("pvUUID").toString());
        }
        if (xlun.containsKey("vgUUID")) {
            lun.setvolume_group_id(xlun.get("vgUUID").toString());
        } else {
            lun.setvolume_group_id("");
        }
        if (xlun.containsKey("vgName")) {
            lun.setStorageDomainId(Guid.createGuidFromString(xlun.get("vgName").toString()));
        }
        if (xlun.containsKey("serial")) {
            lun.setSerial(xlun.get("serial").toString());
        }
        if (xlun.containsKey(PATHSTATUS)) {
            Object[] temp = (Object[]) xlun.get(PATHSTATUS);
            Map<String, Object>[] pathStatus = null;
            if (temp != null) {
                lun.setPathsDictionary(new HashMap<String, Boolean>());
                pathStatus = new Map[temp.length];
                for (int i = 0; i < temp.length; i++) {
                    pathStatus[i] = (Map<String, Object>) temp[i];
                }

                for (Map xcon : pathStatus) {
                    if (xcon.containsKey(LUN_FIELD)) {
                        lun.setLunMapping(Integer.parseInt(xcon.get(LUN_FIELD).toString()));
                    }

                    if (xcon.containsKey(PHYSICAL_DEVICE_FIELD) && xcon.containsKey(DEVICE_STATE_FIELD)) {
                        // set name and state - if active true, otherwise false
                        lun.getPathsDictionary()
                                .put(xcon.get(PHYSICAL_DEVICE_FIELD).toString(),
                                        DEVICE_ACTIVE_VALUE.equals(xcon.get(DEVICE_STATE_FIELD).toString()));
                    }
                }
            }
        }
        if (xlun.containsKey("vendorID")) {
            lun.setVendorId(xlun.get("vendorID").toString());
        }
        if (xlun.containsKey("productID")) {
            lun.setProductId(xlun.get("productID").toString());
        }
        lun.setLunConnections(new ArrayList<StorageServerConnections>());
        if (xlun.containsKey("pathlist")) {
            Object[] temp = (Object[]) xlun.get("pathlist");
            Map[] pathList = null;
            if (temp != null) {
                pathList = new Map[temp.length];
                for (int i = 0; i < temp.length; i++) {
                    pathList[i] = (Map<String, Object>) temp[i];
                }
                for (Map xcon : pathList) {
                    lun.getLunConnections().add(parseConnection(xcon));
                }
            }
        }
        Long size = IrsBrokerCommand.assignLongValue(xlun, "devcapacity");
        if (size == null) {
            size = IrsBrokerCommand.assignLongValue(xlun, "capacity");
        }
        if (size != null) {
            lun.setDeviceSize((int) (size / SizeConverter.BYTES_IN_GB));
        }
        if (xlun.containsKey("vendorID")) {
            lun.setVendorName(xlun.get("vendorID").toString());
        }

        if (xlun.containsKey(DEVTYPE_FIELD)) {
            String devtype = xlun.get(DEVTYPE_FIELD).toString();
            if (!DEVTYPE_VALUE_FCP.equalsIgnoreCase(devtype)) {
                lun.setLunType(StorageType.ISCSI);
            }
        }
        if (xlun.containsKey(STATUS)) {
            String status = xlun.get(STATUS).toString();
            lun.setStatus(EnumUtils.valueOf(LunStatus.class, status, true));
        }
        return lun;
    }

    public static StorageServerConnections parseConnection(Map<String, Object> xcon) {
        StorageServerConnections con = new StorageServerConnections();
        if (xcon.containsKey("connection")) {
            con.setconnection(xcon.get("connection").toString());
        }
        if (xcon.containsKey("portal")) {
            con.setportal(xcon.get("portal").toString());
        }
        if (xcon.containsKey("port")) {
            con.setport(xcon.get("port").toString());
        }
        if (xcon.containsKey("iqn")) {
            con.setiqn(xcon.get("iqn").toString());
        }
        if (xcon.containsKey("user")) {
            con.setuser_name(xcon.get("user").toString());
        }
        if (xcon.containsKey("password")) {
            con.setpassword(xcon.get("password").toString());
        }
        return con;
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return _result.mStatus;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return _result;
    }
}
