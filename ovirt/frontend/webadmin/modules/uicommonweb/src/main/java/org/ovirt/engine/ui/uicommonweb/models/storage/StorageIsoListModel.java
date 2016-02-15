package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.ovirt.engine.core.common.businessentities.ImageFileType;
import org.ovirt.engine.core.common.businessentities.RepoImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.queries.GetImagesListParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class StorageIsoListModel extends SearchableListModel
{

    public StorageIsoListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().imagesTitle());
        setHelpTag(HelpTag.images);
        setHashName("images"); //$NON-NLS-1$

        setImportImagesCommand(new UICommand("Import", this)); //$NON-NLS-1$
        updateActionAvailability();

        setIsTimerDisabled(true);
    }

    private UICommand importImagesCommand;

    public UICommand getImportImagesCommand() {
        return importImagesCommand;
    }

    public void setImportImagesCommand(UICommand importImagesCommand) {
        this.importImagesCommand = importImagesCommand;
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();

        if (getIsAvailable())
        {
            getSearchCommand().execute();
        }
    }

    @Override
    public void setEntity(Object value)
    {
        if (value == null || !value.equals(getEntity())) {
            super.setEntity(value);
            updateActionAvailability();
        }
    }

    @Override
    public void search()
    {
        if (getEntity() != null)
        {
            super.search();
        }
        else
        {
            setItems(null);
        }
    }

    @Override
    protected void syncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }

        super.syncSearch();

        if (getProgress() != null)
        {
            return;
        }

        StorageDomain storageDomain = (StorageDomain) getEntity();
        if (storageDomain.getStorageDomainType() == StorageDomainType.ISO && storageDomain.getStatus() != StorageDomainStatus.Active) {
            setItems(Collections.<StorageDomain>emptyList());
            return;
        }

        GetImagesListParameters imagesListParams = new GetImagesListParameters(storageDomain.getId(), ImageFileType.All);
        imagesListParams.setForceRefresh(true);
        imagesListParams.setRefresh(getIsQueryFirstTime());

        startProgress(null);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.setHandleFailure(true);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnObject)
            {
                VdcQueryReturnValue returnValue = (VdcQueryReturnValue) returnObject;

                stopProgress();

                ArrayList<RepoImage> repoImageList = new ArrayList<RepoImage>();
                if (returnValue != null && returnValue.getReturnValue() != null && returnValue.getSucceeded()) {
                    repoImageList = returnValue.getReturnValue();

                    Collections.sort(repoImageList, new Comparator<RepoImage>() {
                        @Override
                        public int compare(RepoImage a, RepoImage b) {
                            return a.getRepoImageId().compareToIgnoreCase(b.getRepoImageId());
                        }
                    });
                }

                setItems(repoImageList);
                setIsEmpty(repoImageList.isEmpty());
            }
        };

        Frontend.getInstance().runQuery(VdcQueryType.GetImagesList, imagesListParams, _asyncQuery);
    }

    @Override
    protected String getListName() {
        return "StorageIsoListModel"; //$NON-NLS-1$
    }

    private void importImages() {
        @SuppressWarnings("unchecked")
        ArrayList<RepoImage> repoImages = (ArrayList<RepoImage>) getSelectedItems();

        if (repoImages == null || getWindow() != null) {
            return;
        }

        ImportRepoImageModel model = new ImportRepoImageModel();
        setWindow(model);

        model.setTitle(ConstantsManager.getInstance().getConstants().importImagesTitle());
        model.setHelpTag(HelpTag.import_images);
        model.setHashName("import_images"); //$NON-NLS-1$
        model.setEntity(this);
        model.init((StorageDomain) getEntity(), repoImages);

        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);

        model.setCancelCommand(cancelCommand);
        model.getCommands().add(cancelCommand);
    }

    public void cancel() {
        setWindow(null);
        Frontend.getInstance().unsubscribe();
    }

    private void updateActionAvailability() {
        StorageDomain storageDomain = (StorageDomain) getEntity();
        @SuppressWarnings("unchecked")
        ArrayList<RepoImage> selectedImages =
                getSelectedItems() != null ? (ArrayList<RepoImage>) getSelectedItems() : new ArrayList<RepoImage>();
        if (storageDomain != null && storageDomain.getStorageType() == StorageType.GLANCE) {
            getImportImagesCommand().setIsAvailable(true);
            getImportImagesCommand().setIsExecutionAllowed(selectedImages.size() > 0);
        } else {
            getImportImagesCommand().setIsAvailable(false);
        }
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (getImportImagesCommand().equals(command)) {
            importImages();
        }
        else if (command.getName().equals("Cancel")) { //$NON-NLS-1$
            cancel();
        }
    }
}
