package org.ovirt.engine.ui.uicommonweb.models.macpool;

import org.ovirt.engine.core.common.action.MacPoolParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class SharedMacPoolModel extends MacPoolModel {

    private static final String CMD_SAVE = "OnSave"; //$NON-NLS-1$
    private static final String CMD_CANCEL = "Cancel"; //$NON-NLS-1$

    protected final Model sourceModel;
    private final VdcActionType actionType;

    private final EntityModel<String> name = new EntityModel<String>();
    private final EntityModel<String> description = new EntityModel<String>();

    public EntityModel<String> getName() {
        return name;
    }

    public EntityModel<String> getDescription() {
        return description;
    }

    public SharedMacPoolModel(Model sourceModel, VdcActionType actionType) {
        this.sourceModel = sourceModel;
        this.actionType = actionType;

        UICommand tempVar = UICommand.createDefaultOkUiCommand("OnSave", this); //$NON-NLS-1$
        getCommands().add(tempVar);
        UICommand tempVar2 = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        getCommands().add(tempVar2);
    }

    @Override
    protected void init() {
        super.init();
        getName().setEntity(getEntity().getName());
        getDescription().setEntity(getEntity().getDescription());
    }

    @Override
    public MacPool flush() {
        getEntity().setName(getName().getEntity());
        getEntity().setDescription(getDescription().getEntity());
        return super.flush();
    }

    @Override
    public boolean validate() {
        super.validate();
        getName().validateEntity(new IValidation[] { new NotEmptyValidation() });
        getDescription().validateEntity(new IValidation[] { new AsciiOrNoneValidation() });
        setIsValid(getIsValid() && getName().getIsValid());
        return getIsValid();
    }

    protected void cancel() {
        sourceModel.setWindow(null);
    }

    private void onSave() {
        if (getProgress() != null || !validate()) {
            return;
        }

        startProgress(null);
        MacPool macPool = flush();
        Frontend.getInstance().runAction(actionType, new MacPoolParameters(macPool), new IFrontendActionAsyncCallback() {

            @Override
            public void executed(FrontendActionAsyncResult result) {
                stopProgress();
                if (result.getReturnValue() != null && result.getReturnValue().getSucceeded()) {
                    onActionSucceeded((Guid) result.getReturnValue().getActionReturnValue());
                }
            }
        });
    }

    protected void onActionSucceeded(Guid macPoolId) {
        cancel();
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (CMD_SAVE.equals(command.getName())) {
            onSave();
        } else if (CMD_CANCEL.equals(command.getName())) {
            cancel();
        }
    }

}
