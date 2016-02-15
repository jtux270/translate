package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.validator.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.common.action.AddVmPoolWithVmsParameters;

@RunWith(MockitoJUnitRunner.class)
public class AddVmPoolWithVmsCommandTest extends CommonVmPoolWithVmsCommandTestAbstract {

    @Mock
    private MultipleStorageDomainsValidator multipleSdValidator;

    @SuppressWarnings("serial")
    @Override
    protected AddVmPoolWithVmsCommand<AddVmPoolWithVmsParameters> createCommand() {
        AddVmPoolWithVmsParameters param =
                new AddVmPoolWithVmsParameters(vmPools, testVm, VM_COUNT, DISK_SIZE);
        param.setStorageDomainId(firstStorageDomainId);
        return spy(new AddVmPoolWithVmsCommand<AddVmPoolWithVmsParameters>(param) {
            @Override
            protected void initTemplate() {
                // do nothing - is done here and not with mockito since it's called in the ctor
            }
        });
    }

    @Test
    public void validateCanDoAction() {
        setupForStorageTests();
        assertTrue(command.canDoAction());
    }

    @Test
    public void validatePatternBasedPoolName() {
        String patternBaseName = "aa-??bb";
        command.getParameters().getVmStaticData().setName(patternBaseName);
        command.getParameters().getVmPool().setName(patternBaseName);
        assertTrue(command.validateInputs());
    }

    @Test
    public void validateBeanValidations() {
        assertTrue(command.validateInputs());
    }
}
