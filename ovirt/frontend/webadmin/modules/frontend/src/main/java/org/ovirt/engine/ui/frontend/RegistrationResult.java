package org.ovirt.engine.ui.frontend;

import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.ObservableCollection;

public final class RegistrationResult {
    /**
     Raised once when a first result retrievement occurs.

    */
    private Event privateRetrievedEvent;

    public Event getRetrievedEvent() {
        return privateRetrievedEvent;
    }

    private void setRetrievedEvent(Event value) {
        privateRetrievedEvent = value;
    }

    public final static EventDefinition RetrievedEventDefinition;

    private Guid privateId = Guid.Empty;

    public Guid getId() {
        return privateId;
    }

    private void setId(Guid value) {
        privateId = value;
    }

    private ObservableCollection<IVdcQueryable> privateData;

    public ObservableCollection<IVdcQueryable> getData() {
        return privateData;
    }

    private void setData(ObservableCollection<IVdcQueryable> value) {
        privateData = value;
    }

    private int privateRetrievementCount;

    public int getRetrievementCount() {
        return privateRetrievementCount;
    }

    public void setRetrievementCount(int value) {
        privateRetrievementCount = value;
    }

    static {
        RetrievedEventDefinition = new EventDefinition("RetrievedEvent", RegistrationResult.class); //$NON-NLS-1$
    }

    public RegistrationResult(Guid id, ObservableCollection<IVdcQueryable> data)
    {
        setRetrievedEvent(new Event(RetrievedEventDefinition));

        setId(id);
        setData(data);
    }

}
