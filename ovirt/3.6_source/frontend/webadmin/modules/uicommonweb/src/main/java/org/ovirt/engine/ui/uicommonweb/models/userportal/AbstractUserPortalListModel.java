package org.ovirt.engine.ui.uicommonweb.models.userportal;

import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.uicommonweb.ConsoleOptionsFrontendPersister.ConsoleContext;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConsolePopupModel;
import org.ovirt.engine.ui.uicommonweb.models.ConsolesFactory;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.VmConsoles;
import org.ovirt.engine.ui.uicompat.ICancelable;


public abstract class AbstractUserPortalListModel extends ListWithDetailsModel<Void, /* VmOrPool */ Object, UserPortalItemModel> implements ICancelable {
    private UICommand editConsoleCommand;

    protected ConsolesFactory consolesFactory;

    public AbstractUserPortalListModel() {
        setEditConsoleCommand(new UICommand("NewServer", this)); //$NON-NLS-1$
    }

    protected Iterable filterVms(List all) {
        List<VM> result = new LinkedList<VM>();
        for (Object o : all) {
            if (o instanceof VM) {
                result.add((VM) o);
            }
        }
        return result;
    }

    public List<VmConsoles> getAutoConnectableConsoles() {
        List<VmConsoles> autoConnectableConsoles = new LinkedList<VmConsoles>();

        if (items != null) {
            for (UserPortalItemModel upItem : items) {

                if (!upItem.isPool() && upItem.getVmConsoles().canConnectToConsole()) {
                    autoConnectableConsoles.add(upItem.getVmConsoles());
                }
            }
        }

        return autoConnectableConsoles;
    }

    public boolean getCanConnectAutomatically() {
        return getAutoConnectableConsoles().size() == 1;
    }

    public UICommand getEditConsoleCommand() {
        return editConsoleCommand;
    }

    private void setEditConsoleCommand(UICommand editConsoleCommand) {
        this.editConsoleCommand = editConsoleCommand;
    }

    public abstract void onVmAndPoolLoad();

    @Override
    protected Object provideDetailModelEntity(UserPortalItemModel selectedItem) {
        // Each item in this list model is not a business entity,
        // therefore select an Entity property to provide it to
        // the detail models.
        if (selectedItem == null) {
            return null;
        }

        return selectedItem.getEntity();
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getEditConsoleCommand()) {
            editConsole();
        } else if ("OnEditConsoleSave".equals(command.getName())) { //$NON-NLS-1$
            onEditConsoleSave();
        } else if (Model.CANCEL_COMMAND.equals(command.getName())) {
            cancel();
        }
    }

    private void onEditConsoleSave() {
        cancel();
    }

    private void editConsole() {
        if (getWindow() != null || getSelectedItem().getVmConsoles() == null) {
            return;
        }

        ConsolePopupModel model = new ConsolePopupModel();
        model.setVmConsoles(getSelectedItem().getVmConsoles());
        model.setHelpTag(HelpTag.editConsole);
        model.setHashName("editConsole"); //$NON-NLS-1$
        setWindow(model);

        UICommand saveCommand = UICommand.createDefaultOkUiCommand("OnEditConsoleSave", this); //$NON-NLS-1$
        model.getCommands().add(saveCommand);
        UICommand cancelCommand = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        model.getCommands().add(cancelCommand);
    }

    protected abstract ConsoleContext getConsoleContext();

    public void cancel() {
        setWindow(null);
        setConfirmWindow(null);
    }

}
