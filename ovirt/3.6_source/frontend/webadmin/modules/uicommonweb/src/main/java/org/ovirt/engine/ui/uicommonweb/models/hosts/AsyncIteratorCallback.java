package org.ovirt.engine.ui.uicommonweb.models.hosts;

import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventDefinition;

public class AsyncIteratorCallback<T> {

    public static final EventDefinition notifyEventDefinition;

    private Event<ValueEventArgs<T>> notifyEvent;

    /**
     * Notifies iterator about item retrieval completion.
     */
    public Event<ValueEventArgs<T>> getNotifyEvent() {
        return notifyEvent;
    }

    private void setNotifyEvent(Event<ValueEventArgs<T>> value) {
        notifyEvent = value;
    }

    AsyncQuery asyncQuery;

    /**
     * Returns instance of AsyncQuery type that can be used in AsyncDataProvider.
     */
    public AsyncQuery getAsyncQuery() {
        return asyncQuery;
    }

    private void setAsyncQuery(AsyncQuery value) {
        asyncQuery = value;
    }

    static {
        notifyEventDefinition = new EventDefinition("Notify", AsyncIteratorCallback.class); //$NON-NLS-1$
    }

    public AsyncIteratorCallback() {

        setNotifyEvent(new Event<ValueEventArgs<T>>(notifyEventDefinition));

        // Set a stub method calling notify event on AsyncQuery complete.
        setAsyncQuery(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        notifyEvent.raise(this, new ValueEventArgs<T>((T) returnValue));
                    }
                }));
    }
}
