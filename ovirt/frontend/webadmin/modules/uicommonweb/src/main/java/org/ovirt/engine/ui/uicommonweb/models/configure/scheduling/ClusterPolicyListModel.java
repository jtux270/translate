package org.ovirt.engine.ui.uicommonweb.models.configure.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;
import org.ovirt.engine.core.common.scheduling.parameters.ClusterPolicyCRUDParameters;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.roles_ui.RoleListModel.CommandType;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.ObservableCollection;

public class ClusterPolicyListModel extends ListWithDetailsModel {
    public static final String COPY_OF = "Copy_of_"; //$NON-NLS-1$
    private ManagePolicyUnitModel policyUnitModel;
    private ArrayList<PolicyUnit> policyUnits;
    private UICommand newCommand;
    private UICommand editCommand;
    private UICommand removeCommand;
    private UICommand cloneCommand;
    private UICommand managePolicyUnitCommand;

    public ClusterPolicyListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().clusterPolicyTitle());

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setCloneCommand(new UICommand("Clone", this)); //$NON-NLS-1$
        setManagePolicyUnitCommand(new UICommand("ShowPolicyUnit", this)); //$NON-NLS-1$

        setSearchPageSize(1000);

        updateActionAvailability();
    }

    public List<PolicyUnit> getBalancePolicyUnits() {
        ArrayList<PolicyUnit> list = new ArrayList<PolicyUnit>();
        for (PolicyUnit policyUnit : getPolicyUnits()) {
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.LOAD_BALANCING) {
                list.add(policyUnit);
            }

        }
        return list;
    }

    public ArrayList<PolicyUnit> getFilterPolicyUnits() {
        ArrayList<PolicyUnit> list = new ArrayList<PolicyUnit>();
        for (PolicyUnit policyUnit : getPolicyUnits()) {
            if (policyUnit.getPolicyUnitType() == PolicyUnitType.FILTER) {
                list.add(policyUnit);
            }
        }
        return list;
    }

    private void updateActionAvailability() {
        boolean temp = getSelectedItems() != null && getSelectedItems().size() == 1;

        getCloneCommand().setIsExecutionAllowed(temp);
        getEditCommand().setIsExecutionAllowed(temp);
        getRemoveCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() > 0
                && !isPolicyLocked(getSelectedItems()));
    }

    private boolean isPolicyLocked(List policies) {
        for (Object item : policies) {
            ClusterPolicy cp = (ClusterPolicy) item;
            if (cp.isLocked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void initDetailModels() {
        super.initDetailModels();

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new ClusterPolicyClusterListModel());

        setDetailModels(list);
    }

    @Override
    protected void syncSearch() {
        super.syncSearch();
        if (getIsQueryFirstTime()) {
            Frontend.getInstance().runQuery(VdcQueryType.GetAllPolicyUnits, new VdcQueryParametersBase(), new AsyncQuery(this,
                    new INewAsyncCallback() {

                        @Override
                        public void onSuccess(Object model, Object returnValue) {
                            ClusterPolicyListModel clusterPolicyListModel = (ClusterPolicyListModel) model;
                            ArrayList<PolicyUnit> list =
                                    (ArrayList<PolicyUnit>) ((VdcQueryReturnValue) returnValue).getReturnValue();
                            clusterPolicyListModel.setPolicyUnits(list);
                            clusterPolicyListModel.fetchClusterPolicies();
                            if (policyUnitModel != null) {
                                policyUnitModel.getPolicyUnits().setItems(sort(policyUnits));
                            }
                        }
                    }));

        } else {
            fetchClusterPolicies();
        }
    }

    private void fetchClusterPolicies() {
        AsyncQuery asyncQuery = new AsyncQuery(this, new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                ClusterPolicyListModel clusterPolicyListModel = (ClusterPolicyListModel) model;
                ArrayList<ClusterPolicy> list =
                        (ArrayList<ClusterPolicy>) ((VdcQueryReturnValue) returnValue).getReturnValue();
                Collections.sort(list, new Linq.ClusterPolicyComparator());
                clusterPolicyListModel.setItems(list);
            }
        });

        VdcQueryParametersBase parametersBase = new VdcQueryParametersBase();
        parametersBase.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetClusterPolicies, parametersBase, asyncQuery);
        setIsQueryFirstTime(false);
    }

    public ArrayList<PolicyUnit> getPolicyUnits() {
        return policyUnits;
    }

    public void setPolicyUnits(ArrayList<PolicyUnit> policyUnits) {
        this.policyUnits = policyUnits;
    }

    public UICommand getNewCommand() {
        return newCommand;
    }

    public void setNewCommand(UICommand newCommand) {
        this.newCommand = newCommand;
    }

    @Override
    public UICommand getEditCommand() {
        return editCommand;
    }

    public void setEditCommand(UICommand editCommand) {
        this.editCommand = editCommand;
    }

    public UICommand getRemoveCommand() {
        return removeCommand;
    }

    public void setRemoveCommand(UICommand removeCommand) {
        this.removeCommand = removeCommand;
    }

    public UICommand getCloneCommand() {
        return cloneCommand;
    }

    public void setCloneCommand(UICommand cloneCommand) {
        this.cloneCommand = cloneCommand;
    }

    public UICommand getManagePolicyUnitCommand() {
        return managePolicyUnitCommand;
    }

    public void setManagePolicyUnitCommand(UICommand showPolicyUnitCommand) {
        this.managePolicyUnitCommand = showPolicyUnitCommand;
    }

    private void newEntity() {
        initClusterPolicy(CommandType.New, new ClusterPolicy());
    }

    private void edit() {
        initClusterPolicy(CommandType.Edit, (ClusterPolicy) getSelectedItem());
    }

    private void cloneEntity() {
        ClusterPolicy clusterPolicy = (ClusterPolicy) Cloner.clone(getSelectedItem());
        clusterPolicy.setId(null);
        clusterPolicy.setLocked(false);
        clusterPolicy.setName(COPY_OF + clusterPolicy.getName());
        initClusterPolicy(CommandType.Clone, clusterPolicy);
    }

    private void remove() {
        if (getWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeClusterPolicyTitle());
        model.setHelpTag(HelpTag.remove_cluster_policy);
        model.setHashName("remove_cluster_policy"); //$NON-NLS-1$
        if (getSelectedItems() == null) {
            return;
        }

        ArrayList<String> list = new ArrayList<String>();
        for (ClusterPolicy item : Linq.<ClusterPolicy> cast(getSelectedItems())) {
            list.add(item.getName());
        }
        model.setItems(list);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void onRemove()
    {
        for (Object item : getSelectedItems()) {
            ClusterPolicy clusterPolicy = (ClusterPolicy) item;
            Frontend.getInstance().runAction(VdcActionType.RemoveClusterPolicy,
                    new ClusterPolicyCRUDParameters(clusterPolicy.getId(), clusterPolicy));
        }

        setWindow(null);

        // Execute search to keep list updated.
        getSearchCommand().execute();
    }

    private void managePolicyUnits() {
        if (getWindow() != null) {
            return;
        }

        policyUnitModel = new ManagePolicyUnitModel();
        policyUnitModel.setTitle(ConstantsManager.getInstance().getConstants().managePolicyUnits());
        policyUnitModel.setHelpTag(HelpTag.manage_policy_units);
        policyUnitModel.setHashName("manage_policy_units"); //$NON-NLS-1$

        policyUnitModel.setPolicyUnits(new ListModel());
        policyUnitModel.getPolicyUnits().setItems(sort(policyUnits));
        policyUnitModel.getRefreshPolicyUnitsEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                setIsQueryFirstTime(true);
                syncSearch();
            }
        });

        UICommand command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().close());
        command.setIsCancel(true);
        policyUnitModel.getCommands().add(command);

        setWindow(policyUnitModel);
    }

    private ArrayList<PolicyUnit> sort(ArrayList<PolicyUnit> policyUnits) {
        Collections.sort(policyUnits, new Linq.PolicyUnitComparator());
        return policyUnits;
    }

    private void cancel() {
        setWindow(null);
        policyUnitModel = null;
    }

    private void initClusterPolicy(CommandType commandType, ClusterPolicy clusterPolicy) {
        if (getWindow() != null) {
            return;
        }
        NewClusterPolicyModel clusterPolicyModel =
                NewClusterPolicyModel.createModel(commandType, clusterPolicy, this, getPolicyUnits());
        setWindow(clusterPolicyModel);
    }

    @Override
    protected void onSelectedItemChanged() {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }


    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getNewCommand()) {
            newEntity();
        } else if (command == getEditCommand()) {
            edit();
        } else if (command == getCloneCommand()) {
            cloneEntity();
        } else if (command == getRemoveCommand()) {
            remove();
        } else if (command.getName().equals("OnRemove")) { //$NON-NLS-1$
            onRemove();
        } else if (command.getName().equals("Cancel")) { //$NON-NLS-1$
            cancel();
        } else if (command == getManagePolicyUnitCommand()) { //$NON-NLS-1$
            managePolicyUnits();
        }
    }

    @Override
    protected String getListName() {
        return "ClusterPolicyListModel"; //$NON-NLS-1$
    }

}
