package org.ovirt.engine.ui.uicompat;

public final class ProvideCollectionChangedEvent
{
    public static final EventDefinition Definition;

    static
    {
        Definition = new EventDefinition("CollectionChanged", IProvideCollectionChangedEvent.class);
    }
}
