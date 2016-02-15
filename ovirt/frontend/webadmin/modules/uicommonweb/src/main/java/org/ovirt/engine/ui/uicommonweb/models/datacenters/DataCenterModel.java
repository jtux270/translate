package org.ovirt.engine.ui.uicommonweb.models.datacenters;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.SpecialAsciiI18NOrNoneValidation;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;

@SuppressWarnings("unused")
public class DataCenterModel extends Model
{
    private StoragePool privateEntity;

    public StoragePool getEntity()
    {
        return privateEntity;
    }

    public void setEntity(StoragePool value)
    {
        privateEntity = value;
    }

    private Guid privateDataCenterId;

    public Guid getDataCenterId()
    {
        return privateDataCenterId;
    }

    public void setDataCenterId(Guid value)
    {
        privateDataCenterId = value;
    }

    private boolean privateIsNew;

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private String privateOriginalName;

    public String getOriginalName()
    {
        return privateOriginalName;
    }

    public void setOriginalName(String value)
    {
        privateOriginalName = value;
    }

    private EntityModel<String> privateName;

    public EntityModel<String> getName()
    {
        return privateName;
    }

    public void setName(EntityModel<String> value)
    {
        privateName = value;
    }

    private EntityModel<String> privateDescription;

    public EntityModel<String> getDescription()
    {
        return privateDescription;
    }

    public void setDescription(EntityModel<String> value)
    {
        privateDescription = value;
    }

    private EntityModel<String> privateComment;

    public EntityModel<String> getComment()
    {
        return privateComment;
    }

    public void setComment(EntityModel<String> value)
    {
        privateComment = value;
    }

    private ListModel<Boolean> storagePoolType;

    public ListModel<Boolean> getStoragePoolType()
    {
        return storagePoolType;
    }

    public void setStoragePoolType(ListModel<Boolean> value)
    {
        this.storagePoolType = value;
    }

    private ListModel<Version> privateVersion;

    public ListModel<Version> getVersion()
    {
        return privateVersion;
    }

    public void setVersion(ListModel<Version> value)
    {
        privateVersion = value;
    }

    private int privateMaxNameLength;

    public int getMaxNameLength()
    {
        return privateMaxNameLength;
    }

    public void setMaxNameLength(int value)
    {
        privateMaxNameLength = value;
    }

    ListModel<QuotaEnforcementTypeEnum> quotaEnforceTypeListModel;

    public ListModel<QuotaEnforcementTypeEnum> getQuotaEnforceTypeListModel() {
        return quotaEnforceTypeListModel;
    }

    public void setQuotaEnforceTypeListModel(ListModel<QuotaEnforcementTypeEnum> quotaEnforceTypeListModel) {
        this.quotaEnforceTypeListModel = quotaEnforceTypeListModel;
    }

    public DataCenterModel()
    {
        setName(new EntityModel<String>());
        setDescription(new EntityModel<String>());
        setComment(new EntityModel<String>());
        setVersion(new ListModel<Version>());

        setStoragePoolType(new ListModel<Boolean>());
        getStoragePoolType().getSelectedItemChangedEvent().addListener(this);
        getStoragePoolType().setItems(Arrays.asList(Boolean.FALSE, Boolean.TRUE));

        setQuotaEnforceTypeListModel(new ListModel<QuotaEnforcementTypeEnum>());
        List<QuotaEnforcementTypeEnum> list = AsyncDataProvider.getQuotaEnforcmentTypes();
        getQuotaEnforceTypeListModel().setItems(list);
        getQuotaEnforceTypeListModel().setSelectedItem(list.get(0));

        setMaxNameLength(1);
        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                DataCenterModel dataCenterModel = (DataCenterModel) model;
                dataCenterModel.setMaxNameLength((Integer) result);
            }
        };
        AsyncDataProvider.getDataCenterMaxNameLength(_asyncQuery);

    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition) && sender == getStoragePoolType())
        {
            storagePoolType_SelectedItemChanged();
        }
    }

    private void storagePoolType_SelectedItemChanged()
    {
        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                DataCenterModel dataCenterModel = (DataCenterModel) model;
                ArrayList<Version> versions = (ArrayList<Version>) result;

                // Rebuild version items.
                ArrayList<Version> list = new ArrayList<Version>();
                Boolean isLocalType = dataCenterModel.getStoragePoolType().getSelectedItem();

                for (Version item : versions)
                {
                    if (AsyncDataProvider.isVersionMatchStorageType(item, isLocalType))
                    {
                        list.add(item);
                    }
                }

                Version selectedVersion = null;
                if (dataCenterModel.getVersion().getSelectedItem() != null)
                {
                    selectedVersion = dataCenterModel.getVersion().getSelectedItem();
                    boolean hasSelectedVersion = false;
                    for (Version version : list)
                    {
                        if (selectedVersion.equals(version))
                        {
                            selectedVersion = version;
                            hasSelectedVersion = true;
                            break;
                        }
                    }
                    if (!hasSelectedVersion)
                    {
                        selectedVersion = null;
                    }
                }

                dataCenterModel.getVersion().setItems(list);

                if (selectedVersion == null)
                {
                    dataCenterModel.getVersion().setSelectedItem(Linq.selectHighestVersion(list));
                    if (getEntity() != null)
                    {
                        initVersion();
                    }
                }
                else
                {
                    dataCenterModel.getVersion().setSelectedItem(selectedVersion);
                }

            }
        };
        AsyncDataProvider.getDataCenterVersions(_asyncQuery, getDataCenterId());
    }

    private boolean isVersionInit = false;

    private void initVersion()
    {
        if (!isVersionInit)
        {
            isVersionInit = true;
            for (Version item : getVersion().getItems())
            {
                if (item.equals(getEntity().getcompatibility_version()))
                {
                    getVersion().setSelectedItem(item);
                    break;
                }
            }
        }
    }

    public boolean validate()
    {
        getName().validateEntity(new IValidation[] {
                new NotEmptyValidation(),
                new LengthValidation(40),
                new LengthValidation(getMaxNameLength()),
                new AsciiNameValidation() });

        getVersion().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });

        getDescription().validateEntity(new IValidation[] { new AsciiOrNoneValidation() });

        getComment().validateEntity(new IValidation[] { new SpecialAsciiI18NOrNoneValidation() });

        return getName().getIsValid() && getDescription().getIsValid() && getComment().getIsValid()
                && getVersion().getIsValid();
    }

}
