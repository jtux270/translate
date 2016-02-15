package org.ovirt.engine.core.bll;

import java.util.HashSet;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;

public class GetNextAvailableDiskAliasNameByVMIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {

    public GetNextAvailableDiskAliasNameByVMIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        String suggestedDiskName = null;
        if (getParameters().getId() == null) {
            getQueryReturnValue().setReturnValue(suggestedDiskName);
        } else {
            VM vm = getDbFacade().getVmDao().get(getParameters().getId(), getUserID(), getParameters().isFiltered());
            if (vm != null) {
                updateDisksFromDb(vm);
                suggestedDiskName = getSuggestedDiskName(vm);
            }
            getQueryReturnValue().setReturnValue(suggestedDiskName);
        }
    }

    protected void updateDisksFromDb(VM vm) {
        VmHandler.updateDisksFromDb(vm);
    }

    private String getSuggestedDiskName(VM vm) {
        Set<String> aliases = createDiskAliasesList(vm);
        String suggestedDiskName;
        int i = 0;
        do {
            i++;
            suggestedDiskName = ImagesHandler.getDefaultDiskAlias(vm.getName(), Integer.toString(i));
        } while (aliases.contains(suggestedDiskName));
        return suggestedDiskName;
    }

    private Set<String> createDiskAliasesList(VM vm) {
        Set<String> diskAliases = new HashSet<>(vm.getDiskMap().size());
        for (Disk disk : vm.getDiskMap().values()) {
            diskAliases.add(disk.getDiskAlias());
        }
        return diskAliases;
    }
}
