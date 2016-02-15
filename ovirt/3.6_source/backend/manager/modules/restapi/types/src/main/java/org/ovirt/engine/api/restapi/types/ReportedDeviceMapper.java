package org.ovirt.engine.api.restapi.types;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.api.model.IP;
import org.ovirt.engine.api.model.IPs;
import org.ovirt.engine.api.model.MAC;
import org.ovirt.engine.api.model.ReportedDevice;
import org.ovirt.engine.api.model.ReportedDeviceType;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.compat.Guid;

public class ReportedDeviceMapper {

    @Mapping(from = VmGuestAgentInterface.class, to = ReportedDevice.class)
    public static ReportedDevice map(VmGuestAgentInterface entity, ReportedDevice model) {
        model = model == null ? new ReportedDevice() : model;

        if (entity.getVmId() != null) {
            model.setVm(new VM());
            model.getVm().setId(entity.getVmId().toString());
        }
        if (entity.getMacAddress() != null) {
            MAC mac = new MAC();
            mac.setAddress(entity.getMacAddress());
            model.setMac(mac);
        }
        model.setName(entity.getInterfaceName());
        model.setDescription("guest reported data");
        model.setId(generateDeviceId(entity).toString());
        model.setType(ReportedDeviceType.NETWORK.value());
        mapIps(entity, model);
        return model;
    }

    @Mapping(from = ReportedDevice.class, to = VmGuestAgentInterface.class)
    public static VmGuestAgentInterface map(ReportedDevice model, VmGuestAgentInterface template) {
        VmGuestAgentInterface entity = template != null ? template : new VmGuestAgentInterface();
        if (model.isSetVm() && model.getVm().isSetId()) {
            entity.setVmId(GuidUtils.asGuid(model.getVm().getId()));
        }
        if (model.isSetName()) {
            entity.setInterfaceName(model.getName());
        }
        if (model.isSetMac() && model.getMac().isSetAddress()) {
            entity.setMacAddress(model.getMac().getAddress());
        }
        if (model.isSetIps() && model.getIps().isSetIPs()) {
            List<String> ipv4 = new ArrayList<String>();
            List<String> ipv6 = new ArrayList<String>();
            for (IP ip : model.getIps().getIPs()) {
                if (ip.isSetVersion() && ip.isSetAddress()) {
                    IpVersion ipVersion = IpVersion.fromValue(ip.getVersion());
                    switch (ipVersion) {
                    case V4:
                        ipv4.add(ip.getAddress());
                        break;
                    case V6:
                        ipv6.add(ip.getAddress());
                        break;
                    }
                }
            }
            entity.setIpv4Addresses(ipv4);
            entity.setIpv6Addresses(ipv6);
        }

        return entity;
    }

    @Mapping(from = ReportedDeviceType.class, to = String.class)
    public static String map(ReportedDeviceType type) {
        switch (type) {
        case NETWORK:
            return "network";
        }
        return null;
    }

    private static void mapIps(VmGuestAgentInterface vmGuestAgentInterface, ReportedDevice model) {
        List<String> ipv4 = vmGuestAgentInterface.getIpv4Addresses();
        List<String> ipv6 = vmGuestAgentInterface.getIpv6Addresses();

        if (ipv4 != null && !ipv4.isEmpty() || ipv6 != null && !ipv6.isEmpty()) {
            IPs ips = new IPs();
            model.setIps(ips);
            addIpsByVersion(ips, ipv4, IpVersion.V4);
            addIpsByVersion(ips, ipv6, IpVersion.V6);
        }
    }

    private static void addIpsByVersion(IPs ips, List<String> entityIps, IpVersion ipVersion) {
        if (entityIps != null) {
            for (String entityIp : entityIps) {
                IP ip = new IP();
                ip.setAddress(entityIp);
                ip.setVersion(ipVersion.value());
                ips.getIPs().add(ip);
            }
        }
    }

    public static Guid generateDeviceId(VmGuestAgentInterface vmGuestAgentInterface) {
        return GuidUtils.asGuid((vmGuestAgentInterface.getInterfaceName() + vmGuestAgentInterface.getMacAddress()).getBytes(),
                true);
    }
}
