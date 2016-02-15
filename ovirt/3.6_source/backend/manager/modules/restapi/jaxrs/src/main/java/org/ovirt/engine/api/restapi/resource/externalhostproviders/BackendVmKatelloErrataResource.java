package org.ovirt.engine.api.restapi.resource.externalhostproviders;

import java.util.List;

import org.ovirt.engine.api.model.KatelloErrata;
import org.ovirt.engine.api.model.KatelloErratum;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.externalhostproviders.KatelloErrataResource;
import org.ovirt.engine.api.resource.externalhostproviders.KatelloErratumResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendCollectionResource;
import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendVmKatelloErrataResource extends AbstractBackendCollectionResource<KatelloErratum, Erratum> implements KatelloErrataResource {

    private String vmId;

    public BackendVmKatelloErrataResource(String vmId) {
        super(KatelloErratum.class, Erratum.class);
        this.vmId = vmId;
    }

    @Override
    public KatelloErrata list() {
        return mapCollection(getBackendCollection(VdcQueryType.GetErrataForVm, new IdQueryParameters(asGuid(vmId))));
    }

    private KatelloErrata mapCollection(List<Erratum> entities) {
        KatelloErrata collection = new KatelloErrata();
        for (org.ovirt.engine.core.common.businessentities.Erratum entity : entities) {
            collection.getKatelloErrata().add(addLinks(populate(map(entity), entity), VM.class));
        }

        return collection;
    }

    @Override
    public KatelloErratumResource getKatelloErratumSubResource(String id) {
        return inject(new BackendVmKatelloErratumResource(id, vmId));
    }

    @Override
    protected KatelloErratum addParents(KatelloErratum erratum) {
        VM vm = new VM();
        vm.setId(vmId);
        erratum.setVm(vm);
        return super.addParents(erratum);
    }
}
