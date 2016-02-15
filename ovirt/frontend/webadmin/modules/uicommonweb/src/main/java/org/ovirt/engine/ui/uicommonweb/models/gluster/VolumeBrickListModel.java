package org.ovirt.engine.ui.uicommonweb.models.gluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeBricksActionParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeRemoveBricksParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeReplaceBrickActionParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterTaskType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.BrickDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterClientInfo;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterTaskOperation;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeAdvancedDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.Mempool;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.volumes.VolumeListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class VolumeBrickListModel extends SearchableListModel {

    @Override
    protected String getListName() {
        return "VolumeBrickListModel"; //$NON-NLS-1$
    }

    public VolumeBrickListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().bricksTitle());
        setHelpTag(HelpTag.bricks);
        setHashName("bricks"); //$NON-NLS-1$
        setAddBricksCommand(new UICommand("Add Bricks", this)); //$NON-NLS-1$
        setRemoveBricksCommand(new UICommand("Remove Bricks", this)); //$NON-NLS-1$
        setStopRemoveBricksCommand(new UICommand("StopRemoveBricks", this)); //$NON-NLS-1$
        setCommitRemoveBricksCommand(new UICommand("CommitRemoveBricks", this)); //$NON-NLS-1$
        setStatusRemoveBricksCommand(new UICommand("StatusRemoveBricks", this)); //$NON-NLS-1$
        setRetainBricksCommand(new UICommand("RetainBricks", this)); //$NON-NLS-1$
        setReplaceBrickCommand(new UICommand("Replace Brick", this)); //$NON-NLS-1$
        setBrickAdvancedDetailsCommand(new UICommand("Brick Advanced Details", this)); //$NON-NLS-1$
        getReplaceBrickCommand().setIsAvailable(false);
    }

    private GlusterVolumeEntity volumeEntity;

    public void setVolumeEntity(GlusterVolumeEntity volumeEntity) {
        this.volumeEntity = volumeEntity;
        updateRemoveBrickActionsAvailability(volumeEntity);
    }

    public GlusterVolumeEntity getVolumeEntity() {
        return volumeEntity;
    }

    private UICommand addBricksCommand;

    public UICommand getAddBricksCommand()
    {
        return addBricksCommand;
    }

    private void setAddBricksCommand(UICommand value)
    {
        addBricksCommand = value;
    }

    private UICommand removeBricksCommand;

    public UICommand getRemoveBricksCommand()
    {
        return removeBricksCommand;
    }

    private void setRemoveBricksCommand(UICommand value)
    {
        removeBricksCommand = value;
    }

    private UICommand stopRemoveBricksCommand;

    public UICommand getStopRemoveBricksCommand()
    {
        return stopRemoveBricksCommand;
    }

    private void setStopRemoveBricksCommand(UICommand value)
    {
        stopRemoveBricksCommand = value;
    }

    private UICommand commitRemoveBricksCommand;

    public UICommand getCommitRemoveBricksCommand()
    {
        return commitRemoveBricksCommand;
    }

    private void setCommitRemoveBricksCommand(UICommand value)
    {
        commitRemoveBricksCommand = value;
    }

    private UICommand statusRemoveBricksCommand;

    public UICommand getStatusRemoveBricksCommand()
    {
        return statusRemoveBricksCommand;
    }

    private void setStatusRemoveBricksCommand(UICommand value)
    {
        statusRemoveBricksCommand = value;
    }

    private UICommand retainBricksCommand;

    private void setRetainBricksCommand(UICommand value)
    {
        retainBricksCommand = value;
    }

    public UICommand getRetainBricksCommand() {
        return retainBricksCommand;
    }

    private UICommand replaceBrickCommand;

    public UICommand getReplaceBrickCommand()
    {
        return replaceBrickCommand;
    }

    private void setReplaceBrickCommand(UICommand value)
    {
        replaceBrickCommand = value;
    }

    private UICommand brickAdvancedDetailsCommand;

    public UICommand getBrickAdvancedDetailsCommand()
    {
        return brickAdvancedDetailsCommand;
    }

    private void setBrickAdvancedDetailsCommand(UICommand value)
    {
        brickAdvancedDetailsCommand = value;
    }

    @Override
    protected void onSelectedItemChanged() {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        boolean allowRemove = true;
        boolean allowReplace = true;
        boolean allowAdvanced = true;

        if (volumeEntity == null || getSelectedItems() == null || getSelectedItems().size() == 0) {
            allowRemove = false;
            allowReplace = false;
            allowAdvanced = false;
        }
        else {
            GlusterAsyncTask volumeTask = volumeEntity.getAsyncTask();
            if (volumeTask != null
                    && (volumeTask.getStatus() == JobExecutionStatus.STARTED || volumeTask.getType() == GlusterTaskType.REMOVE_BRICK
                            && volumeTask.getStatus() == JobExecutionStatus.FINISHED)) {
                allowRemove = false;
            }
            else if (volumeEntity.getVolumeType() == GlusterVolumeType.STRIPE
                    || getSelectedItems().size() == volumeEntity.getBricks().size()) {
                allowRemove = false;
            }
            else if (volumeEntity.getVolumeType() == GlusterVolumeType.REPLICATE
                    && (volumeEntity.getBricks().size() == VolumeListModel.REPLICATE_COUNT_DEFAULT || getSelectedItems().size() > 1)) {
                allowRemove = false;
            }

            if (getSelectedItems().size() == 1) {
                allowReplace = true;
                allowAdvanced = volumeEntity.isOnline() && ((GlusterBrickEntity) getSelectedItems().get(0)).isOnline();
            }
            else {
                allowReplace = false;
                allowAdvanced = false;
            }
        }

        getRemoveBricksCommand().setIsExecutionAllowed(allowRemove);
        getReplaceBrickCommand().setIsExecutionAllowed(allowReplace);
        getBrickAdvancedDetailsCommand().setIsExecutionAllowed(allowAdvanced);
    }

    public void updateRemoveBrickActionsAvailability(GlusterVolumeEntity volumeEntity) {
        boolean allowStopRemove = true;
        boolean allowCommitRemove = true;
        boolean allowStatusRemove = true;
        boolean allowRetain = true;

        // Stop/Commit/Retain brick removal can be invoked from the Volume(tab) Activities menu as well
        // So no need to check if there are any bricks selected or not, command availability
        // will be decided based on the task on the volume
        allowStopRemove =
                volumeEntity != null && volumeEntity.getAsyncTask() != null
                        && volumeEntity.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK
                        && volumeEntity.getAsyncTask().getStatus() == JobExecutionStatus.STARTED;

        allowCommitRemove =
                volumeEntity != null && volumeEntity.getAsyncTask() != null
                        && volumeEntity.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK
                        && volumeEntity.getAsyncTask().getStatus() == JobExecutionStatus.FINISHED;
        allowRetain =
                volumeEntity != null && volumeEntity.getAsyncTask() != null
                        && volumeEntity.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK
                        && volumeEntity.getAsyncTask().getStatus() == JobExecutionStatus.FINISHED;

        allowStatusRemove =
                volumeEntity != null && volumeEntity.getAsyncTask() != null
                        && volumeEntity.getAsyncTask().getType() == GlusterTaskType.REMOVE_BRICK;

        getStopRemoveBricksCommand().setIsExecutionAllowed(allowStopRemove);
        getCommitRemoveBricksCommand().setIsExecutionAllowed(allowCommitRemove);
        getStatusRemoveBricksCommand().setIsExecutionAllowed(allowStatusRemove);
        getRetainBricksCommand().setIsExecutionAllowed(allowRetain);
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    protected void syncSearch() {
        if (getEntity() != null) {
            GlusterVolumeEntity glusterVolumeEntity = (GlusterVolumeEntity) getEntity();
            // If the items are same, just fire the item changed event to make sure that items are displayed
            if (getItems() == glusterVolumeEntity.getBricks()) {
                getItemsChangedEvent().raise(this, EventArgs.EMPTY);
            } else {
                setItems(glusterVolumeEntity.getBricks());
            }
        }
        else {
            setItems(null);
        }
    }

    private void checkUpServerAndAddBricks() {
        if (getWindow() != null) {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        if (volumeEntity == null) {
            return;
        }

        AsyncDataProvider.isAnyHostUpInCluster(new AsyncQuery(volumeEntity, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object entity, Object returnValue) {
                boolean clusterHasUpHost = (Boolean) returnValue;
                if (clusterHasUpHost) {
                    addBricks((GlusterVolumeEntity) entity);
                }
                else {
                    ConfirmationModel model = new ConfirmationModel();
                    setWindow(model);
                    model.setTitle(ConstantsManager.getInstance().getConstants().addBricksTitle());
                    model.setMessage(ConstantsManager.getInstance()
                            .getConstants()
                            .cannotAddBricksNoUpServerFound());
                    model.setHelpTag(HelpTag.cannot_add_bricks);
                    model.setHashName("cannot_add_bricks"); //$NON-NLS-1$

                    UICommand command = new UICommand("Cancel", VolumeBrickListModel.this); //$NON-NLS-1$
                    command.setTitle(ConstantsManager.getInstance().getConstants().close());
                    command.setIsCancel(true);
                    model.getCommands().add(command);
                    return;
                }
            }
        }), volumeEntity.getVdsGroupName());
    }

    private void addBricks(GlusterVolumeEntity volumeEntity) {

        final VolumeBrickModel volumeBrickModel = new VolumeBrickModel();

        volumeBrickModel.getReplicaCount().setEntity(volumeEntity.getReplicaCount());
        volumeBrickModel.getReplicaCount().setIsChangable(true);
        volumeBrickModel.getReplicaCount().setIsAvailable(volumeEntity.getVolumeType().isReplicatedType());

        volumeBrickModel.getStripeCount().setEntity(volumeEntity.getStripeCount());
        volumeBrickModel.getStripeCount().setIsChangable(true);
        volumeBrickModel.getStripeCount().setIsAvailable(volumeEntity.getVolumeType().isStripedType());

        volumeBrickModel.setTitle(ConstantsManager.getInstance().getConstants().addBricksTitle());
        volumeBrickModel.setHelpTag(HelpTag.add_bricks);
        volumeBrickModel.setHashName("add_bricks"); //$NON-NLS-1$
        volumeBrickModel.getVolumeType().setEntity(volumeEntity.getVolumeType());

        setWindow(volumeBrickModel);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(volumeBrickModel);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                VDSGroup cluster = (VDSGroup) result;
                volumeBrickModel.getForce()
                        .setIsAvailable(GlusterFeaturesUtil.isGlusterForceAddBricksSupported(cluster.getcompatibility_version()));

                AsyncQuery _asyncQueryInner = new AsyncQuery();
                _asyncQueryInner.setModel(model);
                _asyncQueryInner.asyncCallback = new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model, Object result)
                    {
                        VolumeBrickModel volumeBrickModel = (VolumeBrickModel) model;
                        ArrayList<VDS> hostList = (ArrayList<VDS>) result;
                        Iterator<VDS> iterator = hostList.iterator();
                        while (iterator.hasNext())
                        {
                            if (iterator.next().getStatus() != VDSStatus.Up)
                            {
                                iterator.remove();
                            }
                        }

                        volumeBrickModel.getServers().setItems(hostList);
                    }
                };
                AsyncDataProvider.getHostListByCluster(_asyncQueryInner, cluster.getName());
            }
        };
        AsyncDataProvider.getClusterById(_asyncQuery, volumeEntity.getClusterId());

        // TODO: fetch the mount points to display
        volumeBrickModel.getBricks().setItems(new ArrayList<EntityModel<GlusterBrickEntity>>());

        UICommand command = new UICommand("OnAddBricks", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        volumeBrickModel.getCommands().add(command);

        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        volumeBrickModel.getCommands().add(command);
    }

    private void onAddBricks() {
        VolumeBrickModel volumeBrickModel = (VolumeBrickModel) getWindow();
        if (volumeBrickModel == null)
        {
            return;
        }

        if (!volumeBrickModel.validate())
        {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();
        if (volumeEntity == null)
        {
            return;
        }

        ArrayList<GlusterBrickEntity> brickList = new ArrayList<GlusterBrickEntity>();
        for (Object model : volumeBrickModel.getBricks().getItems())
        {
            GlusterBrickEntity brickEntity = (GlusterBrickEntity) ((EntityModel) model).getEntity();
            brickEntity.setVolumeId(volumeEntity.getId());
            brickList.add(brickEntity);
        }

        volumeBrickModel.setMessage(null);

        if (!validateReplicaStripeCount(volumeEntity, volumeBrickModel))
        {
            return;
        }

        if (brickList.size() == 0)
        {
            volumeBrickModel.setMessage(ConstantsManager.getInstance().getConstants().emptyAddBricksMsg());
            return;
        }

        if (!VolumeBrickModel.validateBrickCount(volumeEntity.getVolumeType(), volumeEntity.getBricks().size()
                + brickList.size(),
                volumeBrickModel.getReplicaCountValue(), volumeBrickModel.getStripeCountValue(),
                false))
        {
            volumeBrickModel.setMessage(VolumeBrickModel.getValidationFailedMsg(volumeEntity.getVolumeType(), false));
            return;
        }

        if ((volumeEntity.getVolumeType() == GlusterVolumeType.REPLICATE
                || volumeEntity.getVolumeType() == GlusterVolumeType.DISTRIBUTED_REPLICATE)
                && !volumeBrickModel.validateReplicateBricks(volumeEntity.getReplicaCount(), volumeEntity.getBricks())) {
            ConfirmationModel confirmModel = new ConfirmationModel();
            setConfirmWindow(confirmModel);
            confirmModel.setTitle(ConstantsManager.getInstance()
                    .getConstants()
                    .addBricksReplicateConfirmationTitle());
            confirmModel.setHelpTag(HelpTag.add_bricks_confirmation);
            confirmModel.setHashName("add_bricks_confirmation"); //$NON-NLS-1$
            confirmModel.setMessage(ConstantsManager.getInstance()
                    .getConstants()
                    .addBricksToReplicateVolumeFromSameServerMsg());

            UICommand okCommand = new UICommand("OnAddBricksInternal", this); //$NON-NLS-1$
            okCommand.setTitle(ConstantsManager.getInstance().getConstants().yes());
            okCommand.setIsDefault(true);
            getConfirmWindow().getCommands().add(okCommand);

            UICommand cancelCommand = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
            cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().no());
            cancelCommand.setIsCancel(true);
            getConfirmWindow().getCommands().add(cancelCommand);
        }
        else {
            onAddBricksInternal();
        }
    }

    private void onAddBricksInternal() {

        cancelConfirmation();

        VolumeBrickModel volumeBrickModel = (VolumeBrickModel) getWindow();
        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        ArrayList<GlusterBrickEntity> brickList = new ArrayList<GlusterBrickEntity>();
        for (Object model : volumeBrickModel.getBricks().getItems())
        {
            GlusterBrickEntity brickEntity = (GlusterBrickEntity) ((EntityModel) model).getEntity();
            brickEntity.setVolumeId(volumeEntity.getId());
            brickList.add(brickEntity);
        }

        volumeBrickModel.startProgress(null);

        GlusterVolumeBricksActionParameters parameter = new GlusterVolumeBricksActionParameters(volumeEntity.getId(),
                        brickList,
                        volumeBrickModel.getReplicaCountValue(),
                        volumeBrickModel.getStripeCountValue(),
                        (Boolean) volumeBrickModel.getForce().getEntity());

        Frontend.getInstance().runAction(VdcActionType.AddBricksToGlusterVolume, parameter, new IFrontendActionAsyncCallback() {

            @Override
            public void executed(FrontendActionAsyncResult result) {
                VolumeBrickListModel localModel = (VolumeBrickListModel) result.getState();
                localModel.postOnAddBricks(result.getReturnValue());

            }
        }, this);
    }

    private void cancelConfirmation() {
        setConfirmWindow(null);
    }

    public void postOnAddBricks(VdcReturnValueBase returnValue)
    {
        VolumeBrickModel model = (VolumeBrickModel) getWindow();

        model.stopProgress();

        if (returnValue != null && returnValue.getSucceeded())
        {
            cancel();
        }
    }

    public void cancel() {
        setWindow(null);
    }

    private boolean validateReplicaStripeCount(GlusterVolumeEntity volumeEntity, VolumeBrickModel volumeBrickModel)
    {
        if (volumeEntity.getVolumeType().isReplicatedType())
        {
            int newReplicaCount = volumeBrickModel.getReplicaCountValue();
            if (newReplicaCount > (volumeEntity.getReplicaCount() + 1))
            {
                volumeBrickModel.setMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .addBricksReplicaCountIncreaseValidationMsg());
                return false;
            }
        }
        if (volumeEntity.getVolumeType().isStripedType())
        {
            int newStripeCount = volumeBrickModel.getStripeCountValue();
            if (newStripeCount > (volumeEntity.getStripeCount() + 1))
            {
                volumeBrickModel.setMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .addBricksStripeCountIncreaseValidationMsg());
                return false;
            }
        }
        return true;
    }

    private void removeBricks()
    {
        if (getSelectedItems() == null || getSelectedItems().isEmpty())
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        RemoveBrickModel removeBrickModel = new RemoveBrickModel();
        removeBrickModel.setHelpTag(HelpTag.volume_remove_bricks);
        removeBrickModel.setHashName("volume_remove_bricks"); //$NON-NLS-1$
        removeBrickModel.setTitle(ConstantsManager.getInstance().getConstants().removeBricksTitle());
        setWindow(removeBrickModel);

        removeBrickModel.setReplicaCount(volumeEntity.getReplicaCount());
        removeBrickModel.setStripeCount(volumeEntity.getStripeCount());

        ArrayList<String> list = new ArrayList<String>();
        for (GlusterBrickEntity item : Linq.<GlusterBrickEntity> cast(getSelectedItems()))
        {
            list.add(item.getQualifiedName());
        }
        removeBrickModel.setItems(list);

        if (!validateRemoveBricks(volumeEntity.getVolumeType(),
                Linq.<GlusterBrickEntity> cast(getSelectedItems()),
                volumeEntity.getBricks(),
                removeBrickModel))
        {
            removeBrickModel.setMigrationSupported(false);
            removeBrickModel.setMessage(removeBrickModel.getValidationMessage());
        }
        else
        {
            removeBrickModel.setMigrationSupported(volumeEntity.getVolumeType().isDistributedType());
            removeBrickModel.getMigrateData().setEntity(removeBrickModel.isMigrationSupported());

            if (removeBrickModel.isReduceReplica())
            {
                removeBrickModel.setMessage(ConstantsManager.getInstance()
                        .getMessages()
                        .removeBricksReplicateVolumeMessage(volumeEntity.getReplicaCount(),
                                volumeEntity.getReplicaCount() - 1));
                removeBrickModel.setMigrationSupported(false);
                removeBrickModel.getMigrateData().setEntity(false);
            }
            else
            {
                removeBrickModel.setMessage(ConstantsManager.getInstance().getConstants().removeBricksMessage());
            }

            UICommand command = new UICommand("OnRemove", this); //$NON-NLS-1$
            command.setTitle(ConstantsManager.getInstance().getConstants().ok());
            command.setIsDefault(true);
            removeBrickModel.getCommands().add(command);
        }

        UICommand command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        removeBrickModel.getCommands().add(command);
    }

    public boolean validateRemoveBricks(GlusterVolumeType volumeType,
            List<GlusterBrickEntity> selectedBricks,
            List<GlusterBrickEntity> brickList,
            RemoveBrickModel removeBrickModel)
    {
        boolean valid = true;

        switch (volumeType)
        {
        case REPLICATE:
            if (selectedBricks.size() > 1)
            {
                valid = false;
                removeBrickModel.setValidationMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .cannotRemoveBricksReplicateVolume());
            }
            removeBrickModel.setReplicaCount(removeBrickModel.getReplicaCount() - 1);
            removeBrickModel.setReduceReplica(true);
            break;

        case DISTRIBUTED_REPLICATE:
            valid = validateDistriputedReplicateRemove(volumeType, selectedBricks, brickList, removeBrickModel);
            if (!valid)
            {
                removeBrickModel.setValidationMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .cannotRemoveBricksDistributedReplicateVolume());
            }
            break;

        case DISTRIBUTED_STRIPE:
            valid = validateDistriputedStripeRemove(volumeType, selectedBricks, brickList, removeBrickModel);
            if (!valid)
            {
                removeBrickModel.setValidationMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .cannotRemoveBricksDistributedStripeVolume());
            }
            break;

        case STRIPED_REPLICATE:
            valid = validateStripedReplicateRemove(volumeType, selectedBricks, brickList, removeBrickModel);
            if (!valid)
            {
                removeBrickModel.setValidationMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .cannotRemoveBricksStripedReplicateVolume());
            }
            break;

        case DISTRIBUTED_STRIPED_REPLICATE:
            valid = validateDistributedStripedReplicateRemove(volumeType, selectedBricks, brickList, removeBrickModel);
            if (!valid)
            {
                removeBrickModel.setValidationMessage(ConstantsManager.getInstance()
                        .getConstants()
                        .cannotRemoveBricksDistributedStripedReplicateVolume());
            }
            break;

        default:
            break;
        }

        return valid;
    }

    public boolean validateStripedReplicateRemove(GlusterVolumeType volumeType,
            List<GlusterBrickEntity> selectedBricks,
            List<GlusterBrickEntity> brickList,
            RemoveBrickModel removeBrickModel) {
        // validate only count in the UI
        int stripeCount = removeBrickModel.getStripeCount();
        int replicaCount = removeBrickModel.getReplicaCount();

        if ((brickList.size() - selectedBricks.size()) != stripeCount * replicaCount) {
            return false;
        }

        return true;
    }

    public boolean validateDistributedStripedReplicateRemove(GlusterVolumeType volumeType,
            List<GlusterBrickEntity> selectedBricks,
            List<GlusterBrickEntity> brickList,
            RemoveBrickModel removeBrickModel) {
        int stripeCount = removeBrickModel.getStripeCount();
        int replicaCount = removeBrickModel.getReplicaCount();

        if (selectedBricks.size() % (stripeCount * replicaCount) != 0) {
            return false;
        }

        return true;
    }

    public boolean validateDistriputedReplicateRemove(GlusterVolumeType volumeType,
            List<GlusterBrickEntity> selectedBricks,
            List<GlusterBrickEntity> brickList,
            RemoveBrickModel removeBrickModel)
    {
        int replicaCount = removeBrickModel.getReplicaCount();
        int distributions = brickList.size() / replicaCount;

        // Key - No.of.bricks selected in sub-volume
        // Value - No.of sub-volumes which has 'Key' no.of bricks selected
        Map<Integer, Integer> selectedBricksToSubVolumesMap = new HashMap<Integer, Integer>();

        for (int distIndex = 0; distIndex < distributions; distIndex++) {

            List<GlusterBrickEntity> bricksInSubVolumeList =
                    brickList.subList((distIndex * replicaCount), (distIndex * replicaCount) + replicaCount);

            int selectedBricksInSubVolume = 0;
            for (GlusterBrickEntity brick : bricksInSubVolumeList) {
                if (selectedBricks.contains(brick)) {
                    selectedBricksInSubVolume++;
                }
            }
            if (selectedBricksInSubVolume > 0) {
                if (!selectedBricksToSubVolumesMap.containsKey(selectedBricksInSubVolume)) {
                    selectedBricksToSubVolumesMap.put(selectedBricksInSubVolume, 0);
                }
                selectedBricksToSubVolumesMap.put(selectedBricksInSubVolume, selectedBricksToSubVolumesMap.get(selectedBricksInSubVolume) + 1);
            }
        }

        // If the size of the map is more than 1, then the user has selected different no.of bricks from different
        // sub-volumes, hence not valid for removal.
        if (selectedBricksToSubVolumesMap.size() == 1) {
            // If the user has selected once brick from each sub-volume, then replica count needs to be reduced
            if (selectedBricksToSubVolumesMap.containsKey(1) && selectedBricksToSubVolumesMap.get(1) == distributions) {
                removeBrickModel.setReplicaCount(removeBrickModel.getReplicaCount() - 1);
                removeBrickModel.setReduceReplica(true);
                return true;
            }
            else if (selectedBricksToSubVolumesMap.containsKey(replicaCount)) {
                return true;
            }
            return false;
        }

        return false;
    }

    public boolean validateDistriputedStripeRemove(GlusterVolumeType volumeType,
            List<GlusterBrickEntity> selectedBricks,
            List<GlusterBrickEntity> brickList,
            RemoveBrickModel removeBrickModel)
    {
        int stripeCount = removeBrickModel.getStripeCount();
        int distributions = brickList.size() / stripeCount;

        if (selectedBricks.size() != stripeCount)
        {
            return false;
        }

        for (int i = 0; i < distributions; i++)
        {
            List<GlusterBrickEntity> subBrickList =
                    brickList.subList((i * stripeCount), (i * stripeCount) + stripeCount);
            if (subBrickList.containsAll(selectedBricks))
            {
                return true;
            }
        }

        return false;
    }

    private void onRemoveBricks() {
        if (getWindow() == null)
        {
            return;
        }

        RemoveBrickModel model = (RemoveBrickModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (getSelectedItems() == null || getSelectedItems().isEmpty()) {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        GlusterVolumeRemoveBricksParameters parameter =
                new GlusterVolumeRemoveBricksParameters(volumeEntity.getId(), getSelectedItems());

        if (volumeEntity.getVolumeType() == GlusterVolumeType.REPLICATE)
        {
            parameter.setReplicaCount(volumeEntity.getReplicaCount() - 1);
        }
        else if (volumeEntity.getVolumeType() == GlusterVolumeType.DISTRIBUTED_REPLICATE)
        {
            if (model.isReduceReplica())
            {
                parameter.setReplicaCount(volumeEntity.getReplicaCount() - 1);
            }
            else
            {
                parameter.setReplicaCount(volumeEntity.getReplicaCount());
            }
        }

        model.startProgress(null);

        boolean isMigrate = (Boolean) model.getMigrateData().getEntity();

        Frontend.getInstance().runAction(isMigrate ? VdcActionType.StartRemoveGlusterVolumeBricks
                : VdcActionType.GlusterVolumeRemoveBricks, parameter, new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {

                ConfirmationModel localModel = (ConfirmationModel) result.getState();
                localModel.stopProgress();
                setWindow(null);
            }
        }, model);
    }

    private void stopRemoveBricks() {
        if (getConfirmWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setConfirmWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().stopRemoveBricksTitle());
        model.setMessage(ConstantsManager.getInstance().getConstants().stopRemoveBricksMessage());
        model.setHelpTag(HelpTag.volume_remove_bricks_stop);
        model.setHashName("volume_remove_bricks_stop"); //$NON-NLS-1$

        GlusterVolumeEntity volumeEntity = getVolumeEntity();
        GlusterAsyncTask volumeTask = volumeEntity.getAsyncTask();
        ArrayList<String> list = new ArrayList<String>();
        for (GlusterBrickEntity brick : volumeEntity.getBricks()) {
            if (brick.getAsyncTask() != null && volumeTask != null && brick.getAsyncTask().getTaskId() != null
                    && brick.getAsyncTask().getTaskId().equals(volumeTask.getTaskId())
                    && volumeTask.getStatus() == JobExecutionStatus.STARTED) {
                list.add(brick.getQualifiedName());
            }
        }
        model.setItems(list);

        UICommand okCommand = new UICommand("OnStopRemoveBricks", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        model.getCommands().add(okCommand);

        UICommand cancelCommand = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().close());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void onStopRemoveBricks() {
        if (getConfirmWindow() == null) {
            return;
        }

        ConfirmationModel model = (ConfirmationModel) getConfirmWindow();

        GlusterVolumeEntity volumeEntity = getVolumeEntity();

        ArrayList<GlusterBrickEntity> list = new ArrayList<GlusterBrickEntity>();
        for (Object brickName : model.getItems()) {
            GlusterBrickEntity brick = volumeEntity.getBrickWithQualifiedName((String) brickName);
            if (brick != null) {
                list.add(brick);
            }
        }

        GlusterVolumeRemoveBricksParameters parameter =
                new GlusterVolumeRemoveBricksParameters(volumeEntity.getId(), list);
        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.StopRemoveGlusterVolumeBricks, parameter, new IFrontendActionAsyncCallback() {
            @Override
            public void executed(FrontendActionAsyncResult result) {
                ConfirmationModel localModel = (ConfirmationModel) result.getState();
                localModel.stopProgress();
                setConfirmWindow(null);
                if (result.getReturnValue().getSucceeded()) {
                    showRemoveBricksStatus();
                }
            }
        }, model);
    }

    private void commitRemoveBricks() {
        if (getConfirmWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setConfirmWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().commitRemoveBricksTitle());
        model.setMessage(ConstantsManager.getInstance().getConstants().commitRemoveBricksMessage());
        model.setHelpTag(HelpTag.volume_remove_bricks_commit);
        model.setHashName("volume_remove_bricks_commit"); //$NON-NLS-1$

        GlusterVolumeEntity volumeEntity = getVolumeEntity();
        GlusterAsyncTask volumeTask = volumeEntity.getAsyncTask();
        ArrayList<String> list = new ArrayList<String>();
        for (GlusterBrickEntity brick : volumeEntity.getBricks()) {
            if (brick.getAsyncTask() != null && volumeTask != null && brick.getAsyncTask().getTaskId() != null
                    && brick.getAsyncTask().getTaskId().equals(volumeTask.getTaskId())
                    && volumeTask.getStatus() == JobExecutionStatus.FINISHED) {
                list.add(brick.getQualifiedName());
            }
        }
        model.setItems(list);

        UICommand okCommand = new UICommand("OnCommitRemoveBricks", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        model.getCommands().add(okCommand);

        UICommand cancelCommand = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void onCommitRemoveBricks() {
        if (getConfirmWindow() == null) {
            return;
        }

        ConfirmationModel model = (ConfirmationModel) getConfirmWindow();

        GlusterVolumeEntity volumeEntity = getVolumeEntity();
        ArrayList<GlusterBrickEntity> list = new ArrayList<GlusterBrickEntity>();
        for (Object brickName : model.getItems()) {
            GlusterBrickEntity brick = volumeEntity.getBrickWithQualifiedName((String) brickName);
            if (brick != null) {
                list.add(brick);
            }
        }

        GlusterVolumeRemoveBricksParameters parameter =
                new GlusterVolumeRemoveBricksParameters(volumeEntity.getId(), list);
        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.CommitRemoveGlusterVolumeBricks,
                parameter,
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.stopProgress();
                        setConfirmWindow(null);
                        if (result.getReturnValue().getSucceeded()) {
                            disableRemoveBrickStatusPopUpActions();
                        }
                    }
                },
                model);
    }

    private void showRemoveBricksStatus() {
        final GlusterVolumeEntity volumeEntity = getVolumeEntity();
        final ArrayList<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();
        for (GlusterBrickEntity brick : volumeEntity.getBricks()) {
            if (brick.getAsyncTask() != null && brick.getAsyncTask().getTaskId() != null) {
                bricks.add(brick);
            }
        }
        final ConfirmationModel cModel = new ConfirmationModel();
        cModel.setHelpTag(HelpTag.volume_remove_bricks_status);
        cModel.setHashName("volume_remove_bricks_status"); ////$NON-NLS-1$

        UICommand removeBrickStatusOk = new UICommand("CancelConfirmation", VolumeBrickListModel.this);//$NON-NLS-1$
        removeBrickStatusOk.setTitle(ConstantsManager.getInstance().getConstants().ok());
        removeBrickStatusOk.setIsCancel(true);
        cModel.startProgress(ConstantsManager.getInstance().getConstants().fetchingDataMessage());
        cModel.getCommands().add(removeBrickStatusOk);
        cModel.setTitle(ConstantsManager.getInstance().getConstants().removeBricksStatusTitle());
        setConfirmWindow(cModel);

        final UICommand stopRemoveBrickFromStatus = new UICommand("StopRemoveBricksOnStatus", this);//$NON-NLS-1$
        stopRemoveBrickFromStatus.setTitle(ConstantsManager.getInstance().getConstants().stopRemoveBricksButton());
        stopRemoveBrickFromStatus.setIsExecutionAllowed(false);

        final UICommand commitRemoveBrickFromStatus = new UICommand("CommitRemoveBricksOnStatus", this);//$NON-NLS-1$
        commitRemoveBrickFromStatus.setTitle(ConstantsManager.getInstance().getConstants().commitRemoveBricksButton());
        commitRemoveBrickFromStatus.setIsExecutionAllowed(false);

        final UICommand retainBricksFromStatus = new UICommand("RetainBricksOnStatus", this);//$NON-NLS-1$
        retainBricksFromStatus.setTitle(ConstantsManager.getInstance().getConstants().retainBricksButton());
        retainBricksFromStatus.setIsExecutionAllowed(false);

        final UICommand cancelCommand = new UICommand("CancelRemoveBricksStatus", this);//$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().close());
        cancelCommand.setIsCancel(true);

        AsyncDataProvider.getGlusterRemoveBricksStatus(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                cModel.stopProgress();
                VdcQueryReturnValue vdcValue = (VdcQueryReturnValue) returnValue;

                if (vdcValue.getSucceeded() && vdcValue.getReturnValue() != null) {
                    cancelConfirmation();

                    RemoveBrickStatusModel removeBrickStatusModel;
                    GlusterVolumeTaskStatusEntity removeBrickStatusEntity = vdcValue.getReturnValue();

                    if (getWindow() == null) {
                        removeBrickStatusModel =
                                new RemoveBrickStatusModel(volumeEntity, bricks);
                        removeBrickStatusModel.setTitle(ConstantsManager.getInstance()
                                .getConstants()
                                .removeBricksStatusTitle());
                        removeBrickStatusModel.setHelpTag(HelpTag.volume_remove_bricks_status);
                        removeBrickStatusModel.setHashName("volume_remove_bricks_status"); ////$NON-NLS-1$

                        setWindow(removeBrickStatusModel);

                        removeBrickStatusModel.getVolume().setEntity(volumeEntity.getName());
                        removeBrickStatusModel.getCluster().setEntity(volumeEntity.getVdsGroupName());

                        removeBrickStatusModel.addStopRemoveBricksCommand(stopRemoveBrickFromStatus);
                        removeBrickStatusModel.addCommitRemoveBricksCommand(commitRemoveBrickFromStatus);
                        removeBrickStatusModel.addRetainBricksCommand(retainBricksFromStatus);
                        removeBrickStatusModel.getCommands().add(cancelCommand);
                    }
                    else {
                        removeBrickStatusModel = (RemoveBrickStatusModel) getWindow();
                    }

                    removeBrickStatusModel.showStatus(removeBrickStatusEntity);

                }
                else {
                    cModel.setMessage(ConstantsManager.getInstance()
                            .getMessages()
                            .removeBrickStatusFailed(volumeEntity.getName()));
                }
            }
        }),
                volumeEntity.getClusterId(),
                volumeEntity.getId(),
                bricks);
    }

    private void cancelRemoveBrickStatus() {
        if (getWindow() == null) {
            return;
        }
        ((RemoveBrickStatusModel) getWindow()).cancelRefresh();
        cancel();

    }

    private void retainBricks() {
        if (getConfirmWindow() != null) {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setConfirmWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().retainBricksTitle());
        model.setMessage(ConstantsManager.getInstance().getConstants().retainBricksMessage());
        model.setHelpTag(HelpTag.volume_retain_brick);
        model.setHashName("volume_retain_brick"); //$NON-NLS-1$

        GlusterVolumeEntity volumeEntity = getVolumeEntity();
        GlusterAsyncTask volumeTask = volumeEntity.getAsyncTask();
        ArrayList<String> list = new ArrayList<String>();
        for (GlusterBrickEntity brick : volumeEntity.getBricks()) {
            if (brick.getAsyncTask() != null && volumeTask != null && brick.getAsyncTask().getTaskId() != null
                    && brick.getAsyncTask().getTaskId().equals(volumeTask.getTaskId())
                    && volumeTask.getStatus() == JobExecutionStatus.FINISHED) {
                list.add(brick.getQualifiedName());
            }
        }
        model.setItems(list);

        UICommand okCommand = new UICommand("OnRetainBricks", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        model.getCommands().add(okCommand);

        UICommand cancelCommand = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().close());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void onRetainBricks() {
        if (getConfirmWindow() == null) {
            return;
        }

        ConfirmationModel model = (ConfirmationModel) getConfirmWindow();

        GlusterVolumeEntity volumeEntity = getVolumeEntity();
        ArrayList<GlusterBrickEntity> list = new ArrayList<GlusterBrickEntity>();
        for (Object brickName : model.getItems()) {
            GlusterBrickEntity brick = volumeEntity.getBrickWithQualifiedName((String) brickName);
            if (brick != null) {
                list.add(brick);
            }
        }

        GlusterVolumeRemoveBricksParameters parameter =
                new GlusterVolumeRemoveBricksParameters(volumeEntity.getId(), list);
        model.startProgress(null);

        Frontend.getInstance().runAction(VdcActionType.StopRemoveGlusterVolumeBricks,
                parameter,
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.stopProgress();
                        setConfirmWindow(null);
                        if (result.getReturnValue().getSucceeded()) {
                            showRemoveBricksStatus();
                            disableRemoveBrickStatusPopUpActions();
                        }
                    }
                },
                model);
    }

    private void disableRemoveBrickStatusPopUpActions() {
        if (getWindow() != null && getWindow() instanceof RemoveBrickStatusModel) {
            RemoveBrickStatusModel statusModel = (RemoveBrickStatusModel) getWindow();
            statusModel.getCommitRemoveBricksCommand().setIsExecutionAllowed(false);
            statusModel.getRetainBricksCommand().setIsExecutionAllowed(false);
            statusModel.getStopRemoveBricksCommand().setIsExecutionAllowed(false);
        }
    }
    private void replaceBrick()
    {
        if (getWindow() != null)
        {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();

        if (volumeEntity == null)
        {
            return;
        }

        ReplaceBrickModel brickModel = new ReplaceBrickModel();

        setWindow(brickModel);
        brickModel.setTitle(ConstantsManager.getInstance().getConstants().replaceBrickTitle());
        brickModel.setHelpTag(HelpTag.replace_brick);
        brickModel.setHashName("replace_brick"); //$NON-NLS-1$

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(brickModel);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                VDSGroup cluster = (VDSGroup) result;

                AsyncQuery _asyncQueryInner = new AsyncQuery();
                _asyncQueryInner.setModel(model);
                _asyncQueryInner.asyncCallback = new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model, Object result)
                    {
                        ReplaceBrickModel brickModel = (ReplaceBrickModel) model;
                        ArrayList<VDS> hostList = (ArrayList<VDS>) result;
                        brickModel.getServers().setItems(hostList);
                    }
                };
                AsyncDataProvider.getHostListByCluster(_asyncQueryInner, cluster.getName());
            }
        };
        AsyncDataProvider.getClusterById(_asyncQuery, volumeEntity.getClusterId());

        UICommand command = new UICommand("OnReplace", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        brickModel.getCommands().add(command);

        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsDefault(true);
        brickModel.getCommands().add(command);
    }

    private void onReplaceBrick()
    {
        ReplaceBrickModel replaceBrickModel = (ReplaceBrickModel) getWindow();
        if (replaceBrickModel == null)
        {
            return;
        }

        if (!replaceBrickModel.validate())
        {
            return;
        }

        GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();
        if (volumeEntity == null)
        {
            return;
        }

        GlusterBrickEntity existingBrick = (GlusterBrickEntity) getSelectedItem();
        if (existingBrick == null)
        {
            return;
        }

        VDS server = (VDS) replaceBrickModel.getServers().getSelectedItem();

        GlusterBrickEntity newBrick = new GlusterBrickEntity();
        newBrick.setVolumeId(volumeEntity.getId());
        newBrick.setServerId(server.getId());
        newBrick.setServerName(server.getHostName());
        newBrick.setBrickDirectory((String) replaceBrickModel.getBrickDirectory().getEntity());

        replaceBrickModel.startProgress(null);

        GlusterVolumeReplaceBrickActionParameters parameter =
                new GlusterVolumeReplaceBrickActionParameters(volumeEntity.getId(),
                        GlusterTaskOperation.START,
                        existingBrick,
                        newBrick,
                        false);

        Frontend.getInstance().runAction(VdcActionType.ReplaceGlusterVolumeBrick, parameter, new IFrontendActionAsyncCallback() {

            @Override
            public void executed(FrontendActionAsyncResult result) {

                ReplaceBrickModel localModel = (ReplaceBrickModel) result.getState();
                localModel.stopProgress();
                setWindow(null);
            }
        }, replaceBrickModel);

    }

    private void showBrickAdvancedDetails() {
        final GlusterVolumeEntity volumeEntity = (GlusterVolumeEntity) getEntity();
        AsyncDataProvider.getClusterById(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                VDSGroup vdsGroup = (VDSGroup) returnValue;
                if (Version.v3_2.compareTo(vdsGroup.getcompatibility_version()) <= 0) {
                    onShowBrickAdvancedDetails(volumeEntity);
                }
                else {
                    ConfirmationModel model = new ConfirmationModel();
                    setWindow(model);
                    model.setTitle(ConstantsManager.getInstance().getConstants().advancedDetailsBrickTitle());
                    model.setMessage(ConstantsManager.getInstance()
                            .getMessages()
                            .brickDetailsNotSupportedInClusterCompatibilityVersion(vdsGroup.getcompatibility_version() != null ? vdsGroup.getcompatibility_version()
                                    .toString()
                                    : "")); //$NON-NLS-1$
                    model.setHelpTag(HelpTag.brick_details_not_supported);
                    model.setHashName("brick_details_not_supported"); //$NON-NLS-1$

                    UICommand command = new UICommand("Cancel", VolumeBrickListModel.this); //$NON-NLS-1$
                    command.setTitle(ConstantsManager.getInstance().getConstants().close());
                    command.setIsCancel(true);
                    model.getCommands().add(command);
                }
            }
        }),
                volumeEntity.getClusterId());

    }

    private void onShowBrickAdvancedDetails(GlusterVolumeEntity volumeEntity) {
        final GlusterBrickEntity brickEntity = (GlusterBrickEntity) getSelectedItem();

        final BrickAdvancedDetailsModel brickModel = new BrickAdvancedDetailsModel();
        setWindow(brickModel);
        brickModel.setTitle(ConstantsManager.getInstance().getConstants().advancedDetailsBrickTitle());
        brickModel.setHelpTag(HelpTag.brick_advanced);
        brickModel.setHashName("brick_advanced"); //$NON-NLS-1$
        brickModel.startProgress(null);

        AsyncDataProvider.getGlusterVolumeBrickDetails(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                brickModel.stopProgress();

                VdcQueryReturnValue returnValue = (VdcQueryReturnValue) result;
                if (returnValue == null || !returnValue.getSucceeded()) {
                    brickModel.setMessage(ConstantsManager.getInstance()
                            .getConstants()
                            .errorInFetchingBrickAdvancedDetails());
                    return;
                }

                GlusterVolumeAdvancedDetails advDetails = (GlusterVolumeAdvancedDetails) returnValue.getReturnValue();
                brickModel.getBrick().setEntity(brickEntity.getQualifiedName());
                if (advDetails != null && advDetails.getBrickDetails() != null
                        && advDetails.getBrickDetails().size() == 1)
                {
                    BrickDetails brickDetails = advDetails.getBrickDetails().get(0);
                    brickModel.getBrickProperties().setProperties(brickDetails.getBrickProperties());

                    ArrayList<EntityModel<GlusterClientInfo>> clients = new ArrayList<EntityModel<GlusterClientInfo>>();
                    for (GlusterClientInfo client : brickDetails.getClients()) {
                        clients.add(new EntityModel<GlusterClientInfo>(client));
                    }
                    brickModel.getClients().setItems(clients);

                    brickModel.getMemoryStatistics().updateMemoryStatistics(brickDetails.getMemoryStatus()
                            .getMallInfo());

                    ArrayList<EntityModel<Mempool>> memoryPools = new ArrayList<EntityModel<Mempool>>();
                    for (Mempool mempool : brickDetails.getMemoryStatus().getMemPools()) {
                        memoryPools.add(new EntityModel<Mempool>(mempool));
                    }
                    brickModel.getMemoryPools().setItems(memoryPools);
                }
            }
        }, true), volumeEntity.getClusterId(), volumeEntity.getId(), brickEntity.getId());

        UICommand command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().close());
        command.setIsCancel(true);
        brickModel.getCommands().add(command);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);
        if (command.equals(getAddBricksCommand())) {
            checkUpServerAndAddBricks();
        } else if (command.getName().equals("OnAddBricks")) { //$NON-NLS-1$
            onAddBricks();
        } else if (command.getName().equals("OnAddBricksInternal")) { //$NON-NLS-1$
            onAddBricksInternal();
        } else if (command.getName().equals("CancelConfirmation")) { //$NON-NLS-1$
            cancelConfirmation();
        } else if (command.equals(getRemoveBricksCommand())) {
            removeBricks();
        } else if (command.getName().equals("OnRemove")) { //$NON-NLS-1$
            onRemoveBricks();
        } else if (command.equals(getStopRemoveBricksCommand())) {
            stopRemoveBricks();
        } else if (command.getName().equals("OnStopRemoveBricks")) { //$NON-NLS-1$
            onStopRemoveBricks();
        } else if (command.equals(getCommitRemoveBricksCommand())) {
            commitRemoveBricks();
        } else if (command.getName().equals("OnCommitRemoveBricks")) { //$NON-NLS-1$
            onCommitRemoveBricks();
        } else if (command.equals(getStatusRemoveBricksCommand())) {
            showRemoveBricksStatus();
        } else if (command.getName().equals("StopRemoveBricksOnStatus")) { //$NON-NLS-1$
            getStopRemoveBricksCommand().execute();
        } else if (command.getName().equals("CommitRemoveBricksOnStatus")) { //$NON-NLS-1$
            getCommitRemoveBricksCommand().execute();
        } else if (command.getName().equals("CancelRemoveBricksStatus")) { //$NON-NLS-1$
            cancelRemoveBrickStatus();
        } else if (command.equals(getRetainBricksCommand())) {
            retainBricks();
        } else if (command.getName().equals("OnRetainBricks")) { //$NON-NLS-1$
            onRetainBricks();
        } else if (command.getName().equals("RetainBricksOnStatus")) { //$NON-NLS-1$
            getRetainBricksCommand().execute();
        } else if (command.equals(getReplaceBrickCommand())) {
            replaceBrick();
        } else if (command.getName().equals("OnReplace")) { //$NON-NLS-1$
            onReplaceBrick();
        } else if (command.equals(getBrickAdvancedDetailsCommand())) {
            showBrickAdvancedDetails();
        } else if (command.getName().equals("Cancel")) { //$NON-NLS-1$
            setWindow(null);
        }

    }
}
