package org.ovirt.engine.ui.uicommonweb.models.quota;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.ui.uicommonweb.models.events.SubTabEventListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class QuotaEventListModel extends SubTabEventListModel
{
    public QuotaEventListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().eventsTitle());
    }

    @Override
    public Quota getEntity()
    {
        return (Quota) ((super.getEntity() instanceof Quota) ? super.getEntity() : null);
    }

    public void setEntity(Quota value)
    {
        super.setEntity(value);
    }

    @Override
    protected void onEntityContentChanged()
    {
        super.onEntityContentChanged();

        if (getEntity() != null)
        {
            getSearchCommand().execute();
        }
        else
        {
            setItems(null);
        }
    }

    @Override
    public void search()
    {
        if (getEntity() != null)
        {
            setSearchString("Events: quota=" + getEntity().getQuotaName()); //$NON-NLS-1$
            super.search();
        }
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.entityPropertyChanged(sender, e);

        if (e.propertyName.equals("name")) //$NON-NLS-1$
        {
            getSearchCommand().execute();
        }
    }
}
