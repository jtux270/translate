package org.ovirt.engine.ui.uicompat;

public final class ProvidePropertyChangedEvent
{
    public static EventDefinition definition;

    static
    {
        definition = new EventDefinition("PropertyChanged", IProvidePropertyChangedEvent.class);
    }
}
