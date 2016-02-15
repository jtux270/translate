package org.ovirt.engine.core.common.businessentities.gluster;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

public class GlusterService extends IVdcQueryable implements BusinessEntity<Guid> {
    private static final long serialVersionUID = 2313305635486777212L;

    private Guid id;
    private ServiceType serviceType;
    private String serviceName;

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GlusterService)) {
            return false;
        }

        GlusterService other = (GlusterService) obj;
        if (!(ObjectUtils.objectsEqual(id, other.getId())
                && serviceType == other.getServiceType()
                && ObjectUtils.objectsEqual(serviceName, other.getServiceName()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getId().hashCode();
        result = prime * result + ((serviceType == null) ? 0 : serviceType.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        return result;
    }
}
