package org.ovirt.engine.ui.uicommonweb.models.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportTemplateData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.ObservableCollection;

@SuppressWarnings("unused")
public class ImportTemplateModel extends ImportVmModel
{

    private TemplateImportDiskListModel templateImportDiskListModel;

    public ImportTemplateModel() {
        super();
        disksToConvert = null;
    }

    @Override
    protected void initDetailModels()
    {
        super.initDetailModels();

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new TemplateGeneralModel() {
            @Override
            public void setEntity(Object value) {
                super.setEntity(value == null ? null : ((ImportTemplateData) value).getTemplate());
            }
        });
        list.add(new TemplateImportInterfaceListModel());
        this.templateImportDiskListModel = new TemplateImportDiskListModel();
        list.add(templateImportDiskListModel);
        setDetailModels(list);
    }

    @Override
    public void setItems(final Collection value, final Guid storageDomainId)
    {
        String vmt_guidKey = "_VMT_ID ="; //$NON-NLS-1$
        String orKey = " or "; //$NON-NLS-1$
        StringBuilder searchPattern = new StringBuilder();
        searchPattern.append("Template: "); //$NON-NLS-1$

        final List<VmTemplate> list = (List<VmTemplate>) value;
        for (int i = 0; i < list.size(); i++) {
            VmTemplate vmTemplate = list.get(i);

            searchPattern.append(vmt_guidKey);
            searchPattern.append(vmTemplate.getId().toString());
            if (i < list.size() - 1) {
                searchPattern.append(orKey);
            }
        }

        Frontend.getInstance().runQuery(VdcQueryType.Search,
                new SearchParameters(searchPattern.toString(), SearchType.VmTemplate),
                new AsyncQuery(this, new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object model, Object returnValue) {
                        List<VmTemplate> vmtList =
                                (List<VmTemplate>) ((VdcQueryReturnValue) returnValue).getReturnValue();

                        List<ImportTemplateData> templateDataList = new ArrayList<ImportTemplateData>();
                        for (VmTemplate template : (Iterable<VmTemplate>) value) {
                            ImportTemplateData templateData = new ImportTemplateData(template);
                            boolean templateExistsInSystem = vmtList.contains(template);
                            templateData.setExistsInSystem(templateExistsInSystem);
                            if (templateExistsInSystem) {
                                templateData.getClone().setEntity(true);
                                templateData.getClone().setChangeProhibitionReason(ConstantsManager.getInstance()
                                        .getConstants()
                                        .importTemplateThatExistsInSystemMustClone());
                                templateData.getClone().setIsChangable(false);
                            }
                            templateDataList.add(templateData);
                        }
                        ImportTemplateModel.super.setSuperItems(templateDataList);
                        doInit(storageDomainId);
                    }
                }));

    }

    @Override
    protected void checkDestFormatCompatibility() {
    }

    @Override
    protected void initDisksStorageDomainsList() {
        for (Object item : getItems()) {
            VmTemplate template = ((ImportTemplateData) item).getTemplate();
            for (Disk disk : template.getDiskList()) {
                DiskImage diskImage = (DiskImage) disk;
                addDiskImportData(diskImage.getId(),
                        filteredStorageDomains, diskImage.getVolumeType(), new EntityModel(true));
            }
        }
        postInitDisks();
    }

    @Override
    protected String getListName() {
        return "ImportTemplateModel"; //$NON-NLS-1$
    }

    @Override
    public SearchableListModel getImportDiskListModel() {
        return templateImportDiskListModel;
    }
}
