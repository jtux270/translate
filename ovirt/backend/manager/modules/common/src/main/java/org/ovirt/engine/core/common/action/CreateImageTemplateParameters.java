package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import org.ovirt.engine.core.compat.Guid;

public class CreateImageTemplateParameters extends ImagesActionsParametersBase implements Serializable {
    private static final long serialVersionUID = 1528721415797299722L;

    private Guid destinationStorageDomainId;
    private Guid privateVmTemplateId;

    public Guid getVmTemplateId() {
        return privateVmTemplateId;
    }

    private void setVmTemplateId(Guid value) {
        privateVmTemplateId = value;
    }

    private Guid privateVmId;

    public Guid getVmId() {
        return privateVmId;
    }

    private void setVmId(Guid value) {
        privateVmId = value;
    }

    private String privateVmTemplateName;

    public String getVmTemplateName() {
        return privateVmTemplateName;
    }

    private void setVmTemplateName(String value) {
        privateVmTemplateName = value;
    }

    public CreateImageTemplateParameters(Guid imageId, Guid vmTemplateId, String vmTemplateName, Guid vmId) {
        super(imageId);
        setVmTemplateId(vmTemplateId);
        setVmTemplateName(vmTemplateName);
        setVmId(vmId);
    }

    public CreateImageTemplateParameters() {
        privateVmTemplateId = Guid.Empty;
        privateVmId = Guid.Empty;
    }

    public void setDestinationStorageDomainId(Guid destinationStorageDomainId) {
        this.destinationStorageDomainId = destinationStorageDomainId;
    }

    public Guid getDestinationStorageDomainId() {
        return destinationStorageDomainId;
    }
}
