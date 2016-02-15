package org.ovirt.engine.ui.common.widget.refresh;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.ovirt.engine.ui.common.system.ApplicationFocusChangeEvent;
import org.ovirt.engine.ui.common.system.ApplicationFocusChangeEvent.ApplicationFocusChangeHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.ModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.GridController;
import org.ovirt.engine.ui.uicommonweb.models.GridTimer;
import org.ovirt.engine.ui.uicommonweb.models.GridTimerStateChangeEvent;
import org.ovirt.engine.ui.uicommonweb.models.GridTimerStateChangeEvent.GridTimerStateChangeEventHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Provides refresh rate management for a {@link GridController}.
 */
public abstract class AbstractRefreshManager<T extends BaseRefreshPanel> implements HasHandlers {

    /**
     * Callback triggered when the user clicks the refresh button.
     */
    public interface ManualRefreshCallback {

        void onManualRefresh();

    }

    // Key used to store refresh rate of all data grids
    private static final String GRID_REFRESH_RATE_KEY = "GridRefreshRate"; //$NON-NLS-1$

    private static final Integer DEFAULT_REFRESH_RATE = GridTimer.DEFAULT_NORMAL_RATE;
    private static final Integer OUT_OF_FOCUS_REFRESH_RATE = Integer.valueOf(60000);
    private static final Set<Integer> REFRESH_RATES = new LinkedHashSet<Integer>();

    static {
        REFRESH_RATES.add(DEFAULT_REFRESH_RATE);
        REFRESH_RATES.add(10000);
        REFRESH_RATES.add(20000);
        REFRESH_RATES.add(30000);
        REFRESH_RATES.add(OUT_OF_FOCUS_REFRESH_RATE);
    }

    /**
     * Returns acceptable refresh rates.
     */
    public static Set<Integer> getRefreshRates() {
        return Collections.unmodifiableSet(REFRESH_RATES);
    }

    private final ModelProvider<? extends GridController> modelProvider;
    private final ClientStorage clientStorage;
    private final T refreshPanel;
    private final EventBus eventBus;
    private ManualRefreshCallback manualRefreshCallback;
    private HandlerRegistration statusUpdateHandlerRegistration;

    private GridController controller;

    public AbstractRefreshManager(ModelProvider<? extends GridController> modelProvider,
            EventBus eventBus, ClientStorage clientStorage) {
        this.modelProvider = modelProvider;
        this.clientStorage = clientStorage;
        this.eventBus = eventBus;
        this.refreshPanel = createRefreshPanel();
        listenOnManualRefresh();
        updateController();

        // Add handler to be notified when the application window gains or looses its focus
        eventBus.addHandler(ApplicationFocusChangeEvent.getType(), new ApplicationFocusChangeHandler() {
            @Override
            public void onApplicationFocusChange(ApplicationFocusChangeEvent event) {
                onWindowFocusChange(event.isInFocus());
            }
        });
    }

    private void updateController() {
        this.controller = modelProvider.getModel();

        updateTimer();
    }

    private void updateTimer() {
        final GridTimer modelTimer = getModelTimer();
        modelTimer.setRefreshRate(readRefreshRate());

        if (statusUpdateHandlerRegistration != null) {
            statusUpdateHandlerRegistration.removeHandler();
        }
        statusUpdateHandlerRegistration =
                modelTimer.addGridTimerStateChangeEventHandler(new GridTimerStateChangeEventHandler() {

            @Override
            public void onGridTimerStateChange(GridTimerStateChangeEvent event) {
                onRefresh(modelTimer.getTimerRefreshStatus());
            }
        });

        modelTimer.resume();
    }

    /**
     * Returns the refresh timer used by the {@link GridController}.
     */
    GridTimer getModelTimer() {
        return controller.getTimer();
    }

    /**
     * Callback fired when the application window gains or looses its focus.
     */
    void onWindowFocusChange(boolean inFocus) {
        GridTimer modelTimer = getModelTimer();

        // Change refresh rate only when the model timer is currently active and not paused
        if (modelTimer.isActive() && !modelTimer.isPaused()) {
            modelTimer.stop();
            if (inFocus) {
                modelTimer.setRefreshRate(readRefreshRate());
            } else {
                modelTimer.setRefreshRate(OUT_OF_FOCUS_REFRESH_RATE);
            }
            modelTimer.start();
        }
    }

    /**
     * When the user clicks the refresh button, enforce the refresh without even asking the timer.
     */
    protected void listenOnManualRefresh() {
        refreshPanel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (manualRefreshCallback != null) {
                    manualRefreshCallback.onManualRefresh();
                }
                ManualRefreshEvent.fire(AbstractRefreshManager.this);
                controller.refresh();
            }
        });
    }

    protected abstract T createRefreshPanel();

    protected void onRefresh(String status) {
        refreshPanel.showStatus(status);
    }

    public T getRefreshPanel() {
        return refreshPanel;
    }

    public void setCurrentRefreshRate(int newRefreshRate) {
        saveRefreshRate(newRefreshRate);
        updateTimer();
    }

    public int getCurrentRefreshRate() {
        return getModelTimer().getRefreshRate();
    }

    String getRefreshRateItemKey() {
        return GRID_REFRESH_RATE_KEY;
    }

    void saveRefreshRate(int newRefreshRate) {
        clientStorage.setLocalItem(getRefreshRateItemKey(), String.valueOf(newRefreshRate));
    }

    /**
     * Returns refresh rate value if it exists. Otherwise, returns default refresh rate.
     */
    int readRefreshRate() {
        String refreshRate = clientStorage.getLocalItem(getRefreshRateItemKey());

        try {
            return Integer.parseInt(refreshRate);
        } catch (NumberFormatException e) {
            return getDefaultRefreshRate();
        }
    }

    protected int getDefaultRefreshRate() {
        return DEFAULT_REFRESH_RATE;
    }

    public void setManualRefreshCallback(ManualRefreshCallback manualRefreshCallback) {
        this.manualRefreshCallback = manualRefreshCallback;
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEventFromSource(event, modelProvider.getModel());
    }

}
