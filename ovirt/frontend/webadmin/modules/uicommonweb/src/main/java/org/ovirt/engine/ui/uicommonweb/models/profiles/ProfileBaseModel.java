package org.ovirt.engine.ui.uicommonweb.models.profiles;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.ProfileParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.profiles.ProfileBase;
import org.ovirt.engine.core.common.businessentities.qos.QosBase;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.queries.QosQueryParameterBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.SpecialAsciiI18NOrNoneValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public abstract class ProfileBaseModel<P extends ProfileBase, Q extends QosBase, R extends BusinessEntity<Guid>> extends Model {

    private EntityModel<String> name;
    private EntityModel<String> description;
    private final EntityModel sourceModel;
    private ListModel<R> parentListModel;
    private ListModel<Q> qos;
    private P profile;
    private final Guid defaultQosId;
    private final VdcActionType vdcActionType;

    public EntityModel<String> getName() {
        return name;
    }

    public void setName(EntityModel<String> name) {
        this.name = name;
    }

    public EntityModel<String> getDescription() {
        return description;
    }

    public void setDescription(EntityModel<String> description) {
        this.description = description;
    }

    public ListModel<R> getParentListModel() {
        return parentListModel;
    }

    public void setParentListModel(ListModel<R> parentListModel) {
        this.parentListModel = parentListModel;
    }

    public ListModel<Q> getQos() {
        return qos;
    }

    public void setQos(ListModel<Q> qos) {
        this.qos = qos;
    }

    public P getProfile() {
        return profile;
    }

    public void setProfile(P profile) {
        this.profile = profile;
    }

    public EntityModel getSourceModel() {
        return sourceModel;
    }

    public Guid getDefaultQosId() {
        return defaultQosId;
    }

    public ProfileBaseModel(EntityModel sourceModel,
            Guid dcId,
            Guid defaultQosId,
            VdcActionType vdcActionType) {
        this.sourceModel = sourceModel;
        this.defaultQosId = defaultQosId;
        this.vdcActionType = vdcActionType;

        setName(new EntityModel<String>());
        setDescription(new EntityModel<String>());
        setParentListModel(new ListModel<R>());
        setQos(new ListModel<Q>());

        initQosList(dcId);
        initCommands();
    }

    protected void initCommands() {
        UICommand okCommand = new UICommand("OnSave", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        getCommands().add(okCommand);
        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        getCommands().add(cancelCommand);
    }

    private void onSave() {
        if (getProgress() != null) {
            return;
        }

        if (!validate()) {
            return;
        }

        // Save changes.
        flush();

        startProgress(null);

        Frontend.getInstance().runAction(vdcActionType,
                getParameters(),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        stopProgress();
                        cancel();
                    }
                },
                this);
    }

    protected abstract ProfileParametersBase<P> getParameters();

    public abstract void flush();

    private void cancel() {
        sourceModel.setWindow(null);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if ("OnSave".equals(command.getName())) { //$NON-NLS-1$
            onSave();
        }
        else if ("Cancel".equals(command.getName())) {//$NON-NLS-1$
            cancel();
        }
    }

    private void initQosList(Guid dataCenterId) {
        if (dataCenterId == null) {
            return;
        }

        Frontend.getInstance().runQuery(VdcQueryType.GetAllQosByStoragePoolIdAndType,
                new QosQueryParameterBase(dataCenterId, getQosType()),
                new AsyncQuery(new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        ProfileBaseModel.this.postInitQosList(returnValue == null ? new ArrayList<Q>()
                                : (List<Q>) ((VdcQueryReturnValue) returnValue).getReturnValue());
                    }

                }));
    }

    protected abstract QosType getQosType();

    protected abstract void postInitQosList(List<Q> list);

    public boolean validate() {
        getName().validateEntity(new IValidation[] { new NotEmptyValidation(), new SpecialAsciiI18NOrNoneValidation() });
        getDescription().validateEntity(new IValidation[] { new AsciiOrNoneValidation() });
        return getName().getIsValid();
    }

}
