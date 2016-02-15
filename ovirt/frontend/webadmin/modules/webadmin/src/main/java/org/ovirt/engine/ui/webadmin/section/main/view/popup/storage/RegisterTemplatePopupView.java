package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.storage.RegisterEntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportTemplateData;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.RegisterTemplatePopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class RegisterTemplatePopupView extends RegisterEntityPopupView
        implements RegisterTemplatePopupPresenterWidget.ViewDef {

    @Inject
    public RegisterTemplatePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources, constants);
    }

    private VmTemplate getEntity(Object object) {
        return ((ImportTemplateData) object).getTemplate();
    }

    @Override
    protected void createEntityTable(RegisterEntityModel model) {
        TextColumnWithTooltip<Object> nameColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                return getEntity(object).getName();
            }
        };
        entityTable.addColumn(nameColumn, constants.nameVm(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> originColumn = new EnumColumn<Object, OriginType>() {
            @Override
            protected OriginType getRawValue(Object object) {
                return getEntity(object).getOrigin();
            }
        };
        entityTable.addColumn(originColumn, constants.originVm(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> memoryColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                int size = getEntity(object).getMemSizeMb();
                return size + " MB"; //$NON-NLS-1$
            }
        };
        entityTable.addColumn(memoryColumn, constants.memoryVm(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> cpuColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                int numOfCpus = getEntity(object).getNumOfCpus();
                return String.valueOf(numOfCpus);
            }
        };
        entityTable.addColumn(cpuColumn, constants.cpusVm(), "50px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> archColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                ArchitectureType clusterArch = getEntity(object).getClusterArch();
                return String.valueOf(clusterArch);
            }
        };
        entityTable.addColumn(archColumn, constants.architectureVm(), "100px"); //$NON-NLS-1$

        TextColumnWithTooltip<Object> diskColumn = new TextColumnWithTooltip<Object>() {
            @Override
            public String getValue(Object object) {
                int numOfDisks = getEntity(object).getDiskTemplateMap().size();
                return String.valueOf(numOfDisks);

            }
        };
        entityTable.addColumn(diskColumn, constants.disksVm(), "50px"); //$NON-NLS-1$

        entityTable.addColumn(getClusterColumn(), constants.clusterVm(), "150px"); //$NON-NLS-1$

        if (model.isQuotaEnabled()) {
            entityTable.addColumn(getClusterQuotaColumn(), constants.quotaVm(), "150px"); //$NON-NLS-1$
        }
    }

    @Override
    protected void createInfoPanel(RegisterEntityModel model) {
        registerEntityInfoPanel = new RegisterTemplateInfoPanel(model);
        entityInfoContainer.add(registerEntityInfoPanel);
    }

    @Override
    public void edit(RegisterEntityModel object) {
        super.edit(object);
    }

    @Override
    public RegisterEntityModel flush() {
        return super.flush();
    }
}
