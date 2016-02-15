package org.ovirt.engine.ui.uicommonweb.models.quota;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class QuotaTemplateListModel extends SearchableListModel {

    public QuotaTemplateListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().templatesTitle());
        setHelpTag(HelpTag.templates);
        setHashName("templates"); //$NON-NLS-1$
        setIsTimerDisabled(true);
    }

    @Override
    protected void syncSearch() {
        if (getEntity() == null)
        {
            return;
        }

        super.syncSearch();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                QuotaTemplateListModel vmModel = (QuotaTemplateListModel) model;
                vmModel.setItems((ArrayList<VM>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
                vmModel.setIsEmpty(((List) vmModel.getItems()).size() == 0);
            }
        };

        IdQueryParameters tempVar = new IdQueryParameters(((Quota) getEntity()).getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.getInstance().runQuery(VdcQueryType.GetTemplatesRelatedToQuotaId, tempVar, _asyncQuery);
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    protected String getListName() {
        return "QuotaTemplateListModel"; //$NON-NLS-1$
    }

}
