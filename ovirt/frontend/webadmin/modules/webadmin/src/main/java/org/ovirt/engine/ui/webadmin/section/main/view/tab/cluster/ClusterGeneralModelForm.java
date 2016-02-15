package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;

import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.ui.common.uicommon.model.ModelProvider;
import org.ovirt.engine.ui.common.widget.form.FormItem;
import org.ovirt.engine.ui.common.widget.form.FormItem.DefaultValueCondition;
import org.ovirt.engine.ui.common.widget.label.BooleanLabel;
import org.ovirt.engine.ui.common.widget.label.ClusterTypeLabel;
import org.ovirt.engine.ui.common.widget.label.ResiliencePolicyLabel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundFormWidget;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterGeneralModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.widget.label.PercentLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;

public class ClusterGeneralModelForm extends AbstractModelBoundFormWidget<ClusterGeneralModel> {

    interface Driver extends SimpleBeanEditorDriver<ClusterGeneralModel, ClusterGeneralModelForm> {
    }

    TextBoxLabel name = new TextBoxLabel();
    TextBoxLabel description = new TextBoxLabel();
    TextBoxLabel dataCenterName = new TextBoxLabel();
    TextBoxLabel cpuType = new TextBoxLabel();
    BooleanLabel cpuThreads;
    PercentLabel<Integer> memoryOverCommit;
    ResiliencePolicyLabel resiliencePolicy;
    ClusterTypeLabel clusterType;
    TextBoxLabel noOfVolumesTotal = new TextBoxLabel();
    TextBoxLabel noOfVolumesUp = new TextBoxLabel();
    TextBoxLabel noOfVolumesDown = new TextBoxLabel();
    TextBoxLabel compatibilityVersion = new TextBoxLabel();
    TextBoxLabel emulatedMachine = new TextBoxLabel();
    TextBoxLabel numberOfVms = new TextBoxLabel();

    private final Driver driver = GWT.create(Driver.class);

    public ClusterGeneralModelForm(ModelProvider<ClusterGeneralModel> modelProvider,
            final ApplicationConstants constants) {
        super(modelProvider, 3, 6);
        cpuThreads = new BooleanLabel(constants.yes(), constants.no());
        memoryOverCommit = new PercentLabel<Integer>();
        resiliencePolicy = new ResiliencePolicyLabel(constants);
        clusterType = new ClusterTypeLabel(constants);

        driver.initialize(this);

        DefaultValueCondition virtServiceNotSupported = new DefaultValueCondition() {
            @Override
            public boolean showDefaultValue() {
                boolean supportsVirtService = getModel().getEntity() != null
                        && getModel().getEntity().supportsVirtService();
                return !supportsVirtService;
            }
        };

        DefaultValueCondition glusterServiceNotSupported = new DefaultValueCondition() {
            @Override
            public boolean showDefaultValue() {
                boolean supportsGlusterService = getModel().getEntity() != null
                        && getModel().getEntity().supportsGlusterService();
                return !supportsGlusterService;
            }
        };

        boolean virtSupported = ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly);
        boolean glusterSupported = ApplicationModeHelper.isModeSupported(ApplicationMode.GlusterOnly);

        formBuilder.addFormItem(new FormItem(constants.nameCluster(), name, 0, 0));
        formBuilder.addFormItem(new FormItem(constants.descriptionCluster(), description, 1, 0));
        formBuilder.addFormItem(new FormItem(constants.dcCluster(), dataCenterName, 2, 0, virtSupported));
        formBuilder.addFormItem(new FormItem(constants.compatibilityVersionCluster(), compatibilityVersion, 3, 0));

        // Show the cluster type only if the application is running in both the modes
        formBuilder.addFormItem(new FormItem(constants.clusterType(), clusterType, 4, 0, virtSupported
                && glusterSupported));

        // properties for virt support
        formBuilder.addFormItem(new FormItem(constants.cpuTypeCluster(), cpuType, 0, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.cpuThreadsCluster(), cpuThreads, 1, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.memoryOptimizationCluster(), memoryOverCommit, 2, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.resiliencePolicyCluster(), resiliencePolicy, 3, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.emulatedMachine(), emulatedMachine, 4, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.numberOfVmsCluster(), numberOfVms, 5, 1, virtSupported)
                .withDefaultValue(constants.notAvailableLabel(), virtServiceNotSupported));

        // properties for gluster support
        formBuilder.addFormItem(new FormItem(constants.clusterVolumesTotalLabel(), noOfVolumesTotal, 0, 2, glusterSupported)
                .withDefaultValue(constants.notAvailableLabel(), glusterServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.clusterVolumesUpLabel(), noOfVolumesUp, 1, 2, glusterSupported)
                .withDefaultValue(constants.notAvailableLabel(), glusterServiceNotSupported));
        formBuilder.addFormItem(new FormItem(constants.clusterVolumesDownLabel(), noOfVolumesDown, 2, 2, glusterSupported)
                .withDefaultValue(constants.notAvailableLabel(), glusterServiceNotSupported));
    }

    @Override
    protected void doEdit(ClusterGeneralModel model) {
        driver.edit(model);
    }

}
