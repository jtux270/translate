package org.ovirt.engine.ui.webadmin.section.main.view.tab.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeParameterListModel;
import org.ovirt.engine.ui.uicommonweb.models.volumes.VolumeListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.gluster.SubTabVolumeParameterPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class SubTabVolumeParameterView extends AbstractSubTabTableView<GlusterVolumeEntity, GlusterVolumeOptionEntity, VolumeListModel, VolumeParameterListModel> implements SubTabVolumeParameterPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabVolumeParameterView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @Inject
    public SubTabVolumeParameterView(SearchableDetailModelProvider<GlusterVolumeOptionEntity, VolumeListModel, VolumeParameterListModel> modelProvider) {
        super(modelProvider);
        initTable();
        initWidget(getTable());
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    void initTable() {
        getTable().enableColumnResizing();

        AbstractTextColumn<GlusterVolumeOptionEntity> optionKeyColumn = new AbstractTextColumn<GlusterVolumeOptionEntity>() {
            @Override
            public String getValue(GlusterVolumeOptionEntity option) {
                return option.getKey();
            }
        };
        optionKeyColumn.makeSortable();
        getTable().addColumn(optionKeyColumn, constants.optionKeyVolumeParameter(), "400px"); //$NON-NLS-1$
        AbstractTextColumn<GlusterVolumeOptionEntity> optionValueColumn =
                new AbstractTextColumn<GlusterVolumeOptionEntity>() {
                    @Override
                    public String getValue(GlusterVolumeOptionEntity option) {
                        return option.getValue();
                    }
                };
        optionValueColumn.makeSortable();
        getTable().addColumn(optionValueColumn, constants.optionValueVolumeParameter(), "400px"); //$NON-NLS-1$;

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.addVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getAddParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.editVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getEditParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.resetVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getResetParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.resetAllVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getResetAllParameterCommand();
            }
        });
    }

}
