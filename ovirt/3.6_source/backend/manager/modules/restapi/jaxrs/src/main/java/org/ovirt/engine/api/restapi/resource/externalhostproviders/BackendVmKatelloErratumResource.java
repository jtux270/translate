package org.ovirt.engine.api.restapi.resource.externalhostproviders;

import static org.ovirt.engine.api.restapi.utils.HexUtils.hex2string;

import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.KatelloErratum;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.externalhostproviders.KatelloErratumResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendSubResource;
import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.core.common.queries.HostErratumQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmKatelloErratumResource extends AbstractBackendSubResource<KatelloErratum, Erratum> implements KatelloErratumResource {

    private String vmId;

    public BackendVmKatelloErratumResource(String id, String vmId) {
        super(id, KatelloErratum.class, Erratum.class);
        this.vmId = vmId;
    }

    @Override
    public KatelloErratum get() {
        return performGet(VdcQueryType.GetErratumByIdForVm,
                new HostErratumQueryParameters(asGuid(vmId), hex2string(id)),
                VM.class);
    }

    @Override
    protected KatelloErratum addParents(KatelloErratum erratum) {
        VM vm = new VM();
        vm.setId(vmId);
        erratum.setVm(vm);
        return super.addParents(erratum);
    }

    @Override
    protected KatelloErratum addLinks(KatelloErratum model,
            Class<? extends BaseResource> suggestedParent,
            String... subCollectionMembersToExclude) {
        return super.addLinks(model, VM.class);
    }

    @Override
    protected Guid asGuidOr404(String id) {
        // The identifier isn't a UUID.
        return null;
    }
}
