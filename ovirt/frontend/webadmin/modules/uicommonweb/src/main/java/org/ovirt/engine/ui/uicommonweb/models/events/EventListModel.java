package org.ovirt.engine.ui.uicommonweb.models.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.ui.frontend.communication.RefreshActiveModelEvent;
import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.NotImplementedException;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.ObservableCollection;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class EventListModel extends ListWithDetailsModel
{
    private UICommand privateRefreshCommand;

    public UICommand getRefreshCommand()
    {
        return privateRefreshCommand;
    }

    private void setRefreshCommand(UICommand value)
    {
        privateRefreshCommand = value;
    }

    private UICommand detailsCommand;

    public UICommand getDetailsCommand() {
        return detailsCommand;
    }

    private void setDetailsCommand(UICommand value) {
        detailsCommand = value;
    }

    private AuditLog lastEvent;

    public AuditLog getLastEvent()
    {
        return lastEvent;
    }

    private void setLastEvent(AuditLog value)
    {
        if (lastEvent != value)
        {
            lastEvent = value;
            onPropertyChanged(new PropertyChangedEventArgs("LastEvent")); //$NON-NLS-1$
        }
    }

    private boolean isAdvancedView;

    public boolean getIsAdvancedView()
    {
        return isAdvancedView;
    }

    public void setIsAdvancedView(boolean value)
    {
        if (isAdvancedView != value)
        {
            isAdvancedView = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsAdvancedView")); //$NON-NLS-1$
        }
    }

    private boolean requestingData;

    public boolean isRequestingData() {
        return requestingData;
    }

    public EventListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().eventsTitle());
        setHelpTag(HelpTag.events);
        setApplicationPlace(WebAdminApplicationPlaces.eventMainTabPlace);
        setHashName("events"); //$NON-NLS-1$

        setRefreshCommand(new UICommand("Refresh", this)); //$NON-NLS-1$
        setDetailsCommand(new UICommand("Details", this)); //$NON-NLS-1$

        setDefaultSearchString("Events:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.AUDIT_OBJ_NAME, SearchObjects.AUDIT_PLU_OBJ_NAME });

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
    }

    @Override
    protected void doGridTimerExecute() {
        getRefreshCommand().execute();
    }

    @Override
    public boolean isSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("event"); //$NON-NLS-1$
    }

    @Override
    protected void syncSearch()
    {
        super.syncSearch();

        requestingData = true;
        setItems(new ObservableCollection<AuditLog>());
        setLastEvent(null);
    }

    @Override
    public void search() {
        super.search();

        // Force refresh of the event list when the event tab is shown
        // without waiting to the timer. This is invoked only the first
        // time the Events tab is shown - than the timer takes care of this.
        forceRefreshWithoutTimers();
    }

    protected void forceRefreshWithoutTimers() {
        getRefreshCommand().execute();
    }

    @Override
    protected boolean handleRefreshActiveModel(RefreshActiveModelEvent event) {
        return false;
    }

    @Override
    public boolean supportsServerSideSorting() {
        return true;
    }

    protected void refreshModel()
    {
        AsyncQuery query = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object returnValue) {
                EventListModel eventListModel = (EventListModel) model;
                ArrayList<AuditLog> list = (ArrayList<AuditLog>) ((VdcQueryReturnValue) returnValue).getReturnValue();
                requestingData = false;
                for (AuditLog auditLog : list) {
                    // in case the corr_id is created in client,
                    // remove unnecessary data (leave only the corr_id).
                    if (auditLog.getCorrelationId() != null
                        && auditLog.getCorrelationId().startsWith(TaskListModel._WEBADMIN_)) {
                        auditLog.setCorrelationId(auditLog.getCorrelationId().split("_")[2]); //$NON-NLS-1$
                    }
                }
                eventListModel.updateItems(list);
            }
        });

        SearchParameters params = new SearchParameters(applySortOptions(getSearchString()), SearchType.AuditLog,
                isCaseSensitiveSearch());
        params.setMaxCount(getSearchPageSize());
        params.setSearchFrom(getLastEvent() != null ? getLastEvent().getaudit_log_id() : 0);
        params.setRefresh(false);

        Frontend.getInstance().runQuery(VdcQueryType.Search, params, query);
    }

    private void details() {

        AuditLog event = (AuditLog) getSelectedItem();

        if (getWindow() != null || event == null) {
            return;
        }

        EventModel model = new EventModel();
        model.setEvent(event);
        model.setTitle(ConstantsManager.getInstance().getConstants().eventDetailsTitle());
        model.setHelpTag(HelpTag.event_details);
        model.setHashName("event_details"); //$NON-NLS-1$
        setWindow(model);

        UICommand command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().close());
        command.setIsCancel(true);
        model.getCommands().add(command);
    }

    private void cancel() {
        setWindow(null);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getRefreshCommand()) {
            refreshModel();
            updatePagingAvailability();
        } else if (command == getDetailsCommand()) {
            details();
        } else if ("Cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        }
    }

    @Override
    public UICommand getDoubleClickCommand() {
        return getDetailsCommand();
    }

    private void updateItems(ArrayList<AuditLog> source)
    {
        if (getItems() == null)
        {
            return;
        }

        List<AuditLog> list = (List<AuditLog>) getItems();

        Collections.sort(source, new Linq.AuditLogComparer());
        if (!source.isEmpty() && !list.contains(source.get(source.size() - 1))) {
            //We received some new events, tell the active models to update.
            RefreshActiveModelEvent.fire(this, false);
        }

        for (AuditLog item : source)
        {
            if (list.size() == getSearchPageSize())
            {
                list.remove(list.size() - 1);
            }
            // This check is for an issue in FF where somehow the event lists gets items twice. This
            // simply checks if the event is already there and only adds it if it is not there.
            if (!list.contains(item)) {
                list.add(0, item);
            }
        }
        getItemsChangedEvent().raise(this, EventArgs.EMPTY);
        setLastEvent(Linq.firstOrDefault(list));

        // If there are no data for this entity, the LastEvent has to be fired in order
        // to stop the progress animation (SearchableTabModelProvider).
        if (Linq.firstOrDefault(list) == null) {
            onPropertyChanged(new PropertyChangedEventArgs("LastEvent")); //$NON-NLS-1$
        }
    }

    private boolean entitiesChanged = true;

    @Override
    protected void entityChanging(Object newValue, Object oldValue) {
        super.entityChanging(newValue, oldValue);
        entitiesChanged = calculateEntitiesChanged(newValue, oldValue);
    }

    /**
     * Returns true if and only if the two entities: <li>are not null <li>implement the IVdcQueryable.getQueryableId()
     * method <li>the old.getQueryableId().equals(new.getQueryableId())
     */
    private boolean calculateEntitiesChanged(Object newValue, Object oldValue) {
        if (newValue == null || oldValue == null) {
            return true;
        }

        if (!(newValue instanceof IVdcQueryable && oldValue instanceof IVdcQueryable)) {
            return true;
        }

        Object oldValueQueriable = null;
        Object newValueQueriable = null;
        try {
            oldValueQueriable = ((IVdcQueryable) oldValue).getQueryableId();
            newValueQueriable = ((IVdcQueryable) newValue).getQueryableId();
        } catch (NotImplementedException e) {
            return true;
        }

        if (oldValueQueriable == null || newValueQueriable == null) {
            return true;
        }

        return !oldValueQueriable.equals(newValueQueriable);
    }

    /**
     * Runs the onEntityContentChanged() only when the calculateEntitiesChanged(new, old) returns true
     */
    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();

        if (entitiesChanged) {
            onEntityContentChanged();
        }
    }

    /**
     * Called when the OnEntityChanged() ensures, that the new entity is different than the old one (based on the
     * IVdcQueryable). Override it in child classes to refresh your model.
     */
    protected void onEntityContentChanged() {
    }

    @Override
    protected String getListName() {
        return "EventListModel"; //$NON-NLS-1$
    }

}
