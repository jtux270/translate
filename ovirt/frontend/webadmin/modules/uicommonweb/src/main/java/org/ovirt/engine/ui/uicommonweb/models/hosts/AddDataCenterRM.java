package org.ovirt.engine.ui.uicommonweb.models.hosts;

import org.ovirt.engine.core.common.action.ChangeVDSClusterParameters;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.action.StoragePoolParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VdsGroupParametersBase;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterModel;
import org.ovirt.engine.ui.uicompat.Enlistment;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IEnlistmentNotification;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PreparingEnlistment;

@SuppressWarnings("unused")
public class AddDataCenterRM extends IEnlistmentNotification {

    public AddDataCenterRM(String correlationId) {
        super(correlationId);
    }

    @Override
    public void prepare(PreparingEnlistment enlistment) {

        context.enlistment = enlistment;

        // Fetch all necessary data to make code flat.
        prepare1();
    }

    private void prepare1() {

        EnlistmentContext enlistmentContext = (EnlistmentContext) context.enlistment.getContext();
        HostListModel model = enlistmentContext.getModel();
        ConfigureLocalStorageModel configureModel = (ConfigureLocalStorageModel) model.getWindow();

        DataCenterModel dataCenterModel = configureModel.getDataCenter();
        String dataCenterName = dataCenterModel.getName().getEntity();

        if (!StringHelper.isNullOrEmpty(dataCenterName)) {

            AsyncDataProvider.getDataCenterListByName(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object model, Object returnValue) {

                            context.dataCenterFoundByName = Linq.firstOrDefault((Iterable<StoragePool>) returnValue);
                            prepare2();
                        }
                    }),
                    dataCenterName);
        } else {
            prepare2();
        }
    }

    private void prepare2() {

        PreparingEnlistment enlistment = (PreparingEnlistment) context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();
        HostListModel model = enlistmentContext.getModel();
        ConfigureLocalStorageModel configureModel = (ConfigureLocalStorageModel) model.getWindow();

        StoragePool candidate = configureModel.getCandidateDataCenter();
        DataCenterModel dataCenterModel = configureModel.getDataCenter();
        String dataCenterName = dataCenterModel.getName().getEntity();

        if (candidate == null || !ObjectUtils.objectsEqual(candidate.getName(), dataCenterName)) {

            // Try to find existing data center with the specified name.
            StoragePool dataCenter = context.dataCenterFoundByName;

            if (dataCenter != null) {

                enlistmentContext.setDataCenterId(dataCenter.getId());

                context.enlistment = null;
                enlistment.prepared();
            } else {

                dataCenter = new StoragePool();
                dataCenter.setName(dataCenterName);
                dataCenter.setdescription(dataCenterModel.getDescription().getEntity());
                dataCenter.setComment(dataCenterModel.getComment().getEntity());
                dataCenter.setIsLocal(dataCenterModel.getStoragePoolType().getSelectedItem());
                dataCenter.setcompatibility_version(dataCenterModel.getVersion().getSelectedItem());

                StoragePoolManagementParameter parameters = new StoragePoolManagementParameter(dataCenter);
                parameters.setCorrelationId(getCorrelationId());
                Frontend.getInstance().runAction(VdcActionType.AddEmptyStoragePool, parameters,
                        new IFrontendActionAsyncCallback() {
                            @Override
                            public void executed(FrontendActionAsyncResult result) {

                                context.addDataCenterReturnValue = result.getReturnValue();
                                prepare3();
                            }
                        });
            }
        } else {
            enlistmentContext.setDataCenterId(configureModel.getDataCenter().getDataCenterId());

            context.enlistment = null;
            enlistment.prepared();
        }
    }

    private void prepare3() {

        PreparingEnlistment enlistment = (PreparingEnlistment) context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();
        VdcReturnValueBase returnValue = context.addDataCenterReturnValue;

        context.enlistment = null;

        if (returnValue != null && returnValue.getSucceeded()) {

            enlistmentContext.setDataCenterId((Guid) returnValue.getActionReturnValue());
            enlistment.prepared();

        } else {
            enlistment.forceRollback();
        }
    }

    @Override
    public void commit(Enlistment enlistment) {
        enlistment.done();
    }

    @Override
    public void rollback(Enlistment enlistment) {
        context.enlistment = enlistment;

        // Fetch all necessary data to make code flat.
        rollback1();
    }

    public void rollback1() {

        Enlistment enlistment = context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();

        if (enlistmentContext.getDataCenterId() != null) {

            AsyncDataProvider.getDataCenterById(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object model, Object returnValue) {

                            context.dataCenterFoundById = (StoragePool) returnValue;
                            rollback2();
                        }
                    }),
                    enlistmentContext.getDataCenterId());
        } else {
            rollback3();
        }
    }

    public void rollback2() {

        Enlistment enlistment = context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();
        HostListModel model = enlistmentContext.getModel();

        VDS host = (VDS) model.getSelectedItem();

        // Retrieve host to make sure we have an updated status etc.
        AsyncDataProvider.getHostById(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object model, Object returnValue) {

                        context.hostFoundById = (VDS) returnValue;
                        rollback3();
                    }
                }),
                host.getId());
    }

    public void rollback3() {

        Enlistment enlistment = context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();
        HostListModel model = enlistmentContext.getModel();

        VDS host = context.hostFoundById;

        boolean abort = false;
        if (model.getSelectedItem() != null) {

            // Perform rollback only when the host is in maintenance.
            if (host.getStatus() != VDSStatus.Maintenance) {
                abort = true;
            }
        } else {
            abort = true;
        }

        if (abort) {

            context.enlistment = null;
            enlistment.done();
            return;
        }

        StoragePool dataCenter = context.dataCenterFoundById;

        // Perform rollback only when the Data Center is un uninitialized.
        if (dataCenter.getStatus() != StoragePoolStatus.Uninitialized) {

            context.enlistment = null;
            enlistment.done();
            return;
        }

        if (enlistmentContext.getOldClusterId() != null) {

            // Switch host back to previous cluster.
            Frontend.getInstance().runAction(VdcActionType.ChangeVDSCluster,
                    new ChangeVDSClusterParameters(enlistmentContext.getOldClusterId(), host.getId()),
                    new IFrontendActionAsyncCallback() {
                        @Override
                        public void executed(FrontendActionAsyncResult result) {

                            VdcReturnValueBase returnValue = result.getReturnValue();

                            context.changeVDSClusterReturnValue = returnValue;
                            rollback4();
                        }
                    });

        } else {
            context.enlistment = null;
            enlistment.done();
        }
    }

    private void rollback4() {

        Enlistment enlistment = context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();
        VdcReturnValueBase returnValue = context.changeVDSClusterReturnValue;

        if (returnValue != null && returnValue.getSucceeded()) {

            // Remove cluster.
            if (enlistmentContext.getClusterId() != null) {

                Frontend.getInstance().runAction(VdcActionType.RemoveVdsGroup,
                        new VdsGroupParametersBase(enlistmentContext.getClusterId()),
                        new IFrontendActionAsyncCallback() {
                            @Override
                            public void executed(FrontendActionAsyncResult result) {
                                rollback5();
                            }
                        });
            }
        } else {
            context.enlistment = null;
            enlistment.done();
        }
    }

    private void rollback5() {

        Enlistment enlistment = context.enlistment;
        EnlistmentContext enlistmentContext = (EnlistmentContext) enlistment.getContext();

        // Try to remove data center.
        if (enlistmentContext.getDataCenterId() != null) {
            Frontend.getInstance().runAction(VdcActionType.RemoveStoragePool,
                    new StoragePoolParametersBase(enlistmentContext.getDataCenterId()));
        }

        // Call done, no matter whether the data center deletion was successful.
        context.enlistment = null;
        enlistment.done();
    }

    private final Context context = new Context();

    public static final class Context {

        public Enlistment enlistment;
        public StoragePool dataCenterFoundByName;
        public StoragePool dataCenterFoundById;
        public VDS hostFoundById;
        public VdcReturnValueBase addDataCenterReturnValue;
        public VdcReturnValueBase changeVDSClusterReturnValue;
    }
}
