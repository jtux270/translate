package org.ovirt.engine.ui.webadmin.section.main.presenter;

import org.ovirt.engine.ui.common.presenter.DynamicTabContainerPresenter;
import org.ovirt.engine.ui.common.presenter.DynamicTabContainerPresenter.DynamicTabPanel;
import org.ovirt.engine.ui.common.presenter.ScrollableTabBarPresenterWidget;
import org.ovirt.engine.ui.common.widget.tab.TabWidgetHandler;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.user.client.ui.IsWidget;
import com.gwtplatform.mvp.client.ChangeTabHandler;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.RequestTabsHandler;
import com.gwtplatform.mvp.client.TabView;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;

/**
 * Base class for sub tab panel presenters.
 *
 * @param <V>
 *            View type.
 * @param <P>
 *            Proxy type.
 */
public abstract class AbstractSubTabPanelPresenter<V extends AbstractSubTabPanelPresenter.ViewDef &
    DynamicTabPanel, P extends Proxy<?>> extends DynamicTabContainerPresenter<V, P> implements TabWidgetHandler {

    public interface ViewDef extends TabView, HasUiHandlers<TabWidgetHandler> {
    }

    @ContentSlot
    public static final Type<RevealContentHandler<?>> TYPE_SetTabBar = new Type<RevealContentHandler<?>>();

    protected final ScrollableTabBarPresenterWidget tabBar;

    public AbstractSubTabPanelPresenter(EventBus eventBus, V view, P proxy,
            Object tabContentSlot,
            Type<RequestTabsHandler> requestTabsEventType,
            Type<ChangeTabHandler> changeTabEventType,
            ScrollableTabBarPresenterWidget tabBar) {
        this(eventBus, view, proxy, tabContentSlot, requestTabsEventType, changeTabEventType,
                tabBar, MainContentPresenter.TYPE_SetSubTabPanelContent);
    }

    public AbstractSubTabPanelPresenter(EventBus eventBus, V view, P proxy,
            Object tabContentSlot,
            Type<RequestTabsHandler> requestTabsEventType,
            Type<ChangeTabHandler> changeTabEventType,
            ScrollableTabBarPresenterWidget tabBar,
            Type<RevealContentHandler<?>> slot) {
        super(eventBus, view, proxy, tabContentSlot, requestTabsEventType, changeTabEventType,
                slot);
        getView().setUiHandlers(tabBar);
        this.tabBar = tabBar;
        this.tabBar.setWantsOffset(false);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        setInSlot(TYPE_SetTabBar, tabBar);
        // Show sub tab panel when revealing sub tab presenter
        UpdateMainContentLayoutEvent.fire(this, true);
    }

    @Override
    public void addTabWidget(IsWidget tabWidget, int index) {
        tabBar.addTabWidget(tabWidget, index);
    }

    @Override
    public void removeTabWidget(IsWidget tabWidget) {
        tabBar.removeTabWidget(tabWidget);
    }
}
