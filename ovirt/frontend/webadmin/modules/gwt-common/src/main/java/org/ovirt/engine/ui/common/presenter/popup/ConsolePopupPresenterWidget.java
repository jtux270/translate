package org.ovirt.engine.ui.common.presenter.popup;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.utils.DynamicMessages;
import org.ovirt.engine.ui.uicommonweb.ConsoleOptionsFrontendPersister;
import org.ovirt.engine.ui.uicommonweb.ConsoleUtils;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConsolePopupModel;
import org.ovirt.engine.ui.uicommonweb.models.ConsoleProtocol;
import org.ovirt.engine.ui.uicommonweb.models.VmConsoles;
import org.ovirt.engine.ui.uicommonweb.models.VmConsolesImpl;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpice;
import org.ovirt.engine.ui.uicommonweb.models.vms.RdpConsoleModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.SpiceConsoleModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VncConsoleModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.dispatch.annotation.GenEvent;

public class ConsolePopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<ConsolePopupModel, ConsolePopupPresenterWidget.ViewDef> {

    @GenEvent
    public class ConsoleModelChanged { }

    // long term todo - rewrite set***Visible to set***Enabled with descriptive tooltip if disabled
    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<ConsolePopupModel> {

        void setSpiceAvailable(boolean visible);

        void setRdpAvailable(boolean visible);

        void setVncAvailable(boolean visible);

        HasValueChangeHandlers<Boolean> getSpiceRadioButton();

        HasValueChangeHandlers<Boolean> getRdpRadioButton();

        HasValueChangeHandlers<Boolean> getVncRadioButton();

        HasValueChangeHandlers<Boolean> getSpiceAutoImplRadioButton();
        HasValueChangeHandlers<Boolean> getSpiceNativeImplRadioButton();
        HasValueChangeHandlers<Boolean> getSpicePluginImplRadioButton();
        HasValueChangeHandlers<Boolean> getSpiceHtml5ImplRadioButton();

        HasValueChangeHandlers<Boolean> getNoVncImplRadioButton();
        HasValueChangeHandlers<Boolean> getVncNativeImplRadioButton();

        HasValueChangeHandlers<Boolean> getRdpAutoImplRadioButton();
        HasValueChangeHandlers<Boolean> getRdpNativeImplRadioButton();
        HasValueChangeHandlers<Boolean> getRdpPluginImplRadioButton();

        HasClickHandlers getConsoleClientResourcesAnchor();

        void showRdpPanel(boolean visible);

        void showSpicePanel(boolean visible);

        void showVncPanel(boolean visible);

        void selectSpice(boolean selected);

        void selectRdp(boolean selected);

        void selectVnc(boolean selected);

        void setNoVncEnabled(boolean enabled, String reason);

        void setAdditionalConsoleAvailable(boolean hasAdditionalConsole);

        void setSpiceConsoleAvailable(boolean available);

        void selectSpiceImplementation(SpiceConsoleModel.ClientConsoleMode consoleMode);

        void setSpicePluginImplEnabled(boolean enabled, String reason);

        void setSpiceHtml5ImplEnabled(boolean enabled, String reason);

        void setRdpPluginImplEnabled(boolean enabled, String reason);

        void selectWanOptionsEnabled(boolean selected);

        void setWanOptionsVisible(boolean visible);

        void setDisableSmartcardVisible(boolean visible);

        void setSpiceProxyEnabled(boolean enabled, String reason);

        void selectVncImplementation(VncConsoleModel.ClientConsoleMode clientConsoleMode);

        void selectRdpImplementation(RdpConsoleModel.ClientConsoleMode consoleMode);

        void setVmName(String name);

        void flushToPrivateModel();

        void setCtrlAltDeleteRemapHotkey(String hotkey);
    }

    private final ConsoleUtils consoleUtils;
    private IEventListener viewUpdatingListener;
    private boolean wanOptionsAvailable = false;
    private ConsolePopupModel model;
    private final CommonApplicationConstants constants;
    private final DynamicMessages dynamicMessages;
    private final ConsoleOptionsFrontendPersister consoleOptionsPersister;

    @Inject
    public ConsolePopupPresenterWidget(EventBus eventBus, ViewDef view,
            ConsoleUtils consoleUtils,
            CommonApplicationConstants constants,
            final DynamicMessages dynamicMessages,
            ConsoleOptionsFrontendPersister consoleOptionsPersister) {
        super(eventBus, view);
        this.consoleUtils = consoleUtils;
        this.constants = constants;
        this.consoleOptionsPersister = consoleOptionsPersister;
        this.dynamicMessages = dynamicMessages;
    }

    @Override
    public void init(final ConsolePopupModel model) {
        this.model = model;
        initView(model);
        initListeners(model);

        String vmName = (model.getVmConsoles() instanceof VmConsolesImpl)
                ? model.getVmConsoles().getVm().getName()
                : model.getVmConsoles().getVm().getVmPoolName(); // for pool dialogs display pool name

        getView().setVmName(vmName);
        getView().setCtrlAltDeleteRemapHotkey(consoleUtils.getRemapCtrlAltDelHotkey());

        super.init(model);
    }

    private void initListeners(final ConsolePopupModel model) {
        ISpice spice = model.getVmConsoles().getConsoleModel(SpiceConsoleModel.class).getspice();
        if (spice == null) {
            return;
        }

        viewUpdatingListener = new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getView().edit(model);
            }
        };

        spice.getUsbAutoShareChangedEvent().addListener(viewUpdatingListener);
        spice.getWANColorDepthChangedEvent().addListener(viewUpdatingListener);
        spice.getWANDisableEffectsChangeEvent().addListener(viewUpdatingListener);

    }

    private void removeListeners(ConsolePopupModel model) {
        if (viewUpdatingListener == null) {
            return;
        }

        ISpice spice = model.getVmConsoles().getConsoleModel(SpiceConsoleModel.class).getspice();
        if (spice == null) {
            return;
        }

        spice.getUsbAutoShareChangedEvent().removeListener(viewUpdatingListener);
        spice.getWANColorDepthChangedEvent().removeListener(viewUpdatingListener);
        spice.getWANDisableEffectsChangeEvent().removeListener(viewUpdatingListener);
    }

    private void initView(ConsolePopupModel model) {

        listenOnRadioButtons(model);
        VmConsoles vmConsoles = model.getVmConsoles();

        getView().setSpiceAvailable(vmConsoles.canSelectProtocol(ConsoleProtocol.SPICE));
        getView().setVncAvailable(vmConsoles.canSelectProtocol(ConsoleProtocol.VNC));
        getView().setRdpAvailable(vmConsoles.canSelectProtocol(ConsoleProtocol.RDP));

        ConsoleProtocol selectedProtocol = vmConsoles.getSelectedProcotol();

        boolean rdpPreselected = ConsoleProtocol.RDP.equals(selectedProtocol);
        boolean spicePreselected = ConsoleProtocol.SPICE.equals(selectedProtocol);
        boolean vncPreselected = ConsoleProtocol.VNC.equals(selectedProtocol);

        getView().selectSpice(spicePreselected);
        getView().selectRdp(rdpPreselected);
        getView().selectVnc(vncPreselected);

        getView().showSpicePanel(spicePreselected);
        getView().showRdpPanel(rdpPreselected);
        getView().showVncPanel(vncPreselected);

        getView().setDisableSmartcardVisible(model.getVmConsoles().getVm().isSmartcardEnabled());

        ISpice spice = model.getVmConsoles().getConsoleModel(SpiceConsoleModel.class).getspice();
        if (spice != null) {
            if (!spice.isWanOptionsEnabled()) {
                getView().selectWanOptionsEnabled(false);
            }
        }

        if (!consoleUtils.isBrowserPluginSupported(ConsoleProtocol.SPICE)) {
            getView().setSpicePluginImplEnabled(false, constants.spicePluginNotSupportedByBrowser());
        }

        getView().setSpiceHtml5ImplEnabled(consoleUtils.isWebSocketProxyDefined(), constants.spiceHtml5OnlyWhenWebsocketProxySet());

        getView().setNoVncEnabled(consoleUtils.isWebSocketProxyDefined(), constants.webSocketProxyNotSet());

        if (!consoleUtils.isBrowserPluginSupported(ConsoleProtocol.RDP)) {
            getView().setRdpPluginImplEnabled(false, constants.rdpPluginNotSupportedByBrowser());
        }

        getView().selectSpiceImplementation(vmConsoles.getConsoleModel(SpiceConsoleModel.class).getClientConsoleMode());
        getView().selectVncImplementation(vmConsoles.getConsoleModel(VncConsoleModel.class).getClientConsoleMode());
        getView().selectRdpImplementation(vmConsoles.getConsoleModel(RdpConsoleModel.class).getClientConsoleMode());

        wanOptionsAvailable = vmConsoles.getConsoleModel(SpiceConsoleModel.class).isWanOptionsAvailableForMyVm();
        if (wanOptionsAvailable) {
            getView().setWanOptionsVisible(true);
        } else {
            getView().setWanOptionsVisible(false);
        }

        getView().setAdditionalConsoleAvailable(vmConsoles.canSelectProtocol(ConsoleProtocol.RDP));
        getView().setSpiceConsoleAvailable(vmConsoles.canSelectProtocol(ConsoleProtocol.SPICE));

        boolean spiceProxyEnabled = consoleUtils.isSpiceProxyDefined(vmConsoles.getVm());

        getView().setSpiceProxyEnabled(spiceProxyEnabled, constants.spiceProxyCanBeEnabledOnlyWhenDefined());

        registerHandler(getView().getConsoleClientResourcesAnchor().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.open(dynamicMessages.consoleClientResourcesUrl(), "_blank", "resizable=yes,scrollbars=yes"); //$NON-NLS-1$ $NON-NLS-2$
            }
        }));
    }

    @Override
    protected void beforeCommandExecuted(UICommand command) {
        super.beforeCommandExecuted(command);

        if (command == model.getDefaultCommand()) {
            // remove listeners which listens to changes in model before flushing
            // data into it
            removeListeners(model);

            // now flush the model
            getView().flushToPrivateModel();

            // store to local storage
            consoleOptionsPersister.storeToLocalStorage(model.getVmConsoles());

            ConsoleModelChangedEvent.fire(this);
        }
    }

    protected void listenOnRadioButtons(final ConsolePopupModel model) {
        registerHandler(getView().getRdpRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                getView().showRdpPanel(event.getValue());
            }
        }));

        registerHandler(getView().getVncRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                getView().showVncPanel(event.getValue());
            }
        }));

        registerHandler(getView().getSpiceRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                getView().showSpicePanel(event.getValue());
            }
        }));

        registerHandler(getView().getSpiceAutoImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectSpiceImplementation(SpiceConsoleModel.ClientConsoleMode.Auto);
                    }
                }));

        registerHandler(getView().getSpiceNativeImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectSpiceImplementation(SpiceConsoleModel.ClientConsoleMode.Native);
                    }
                }));
        registerHandler(getView().getSpicePluginImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectSpiceImplementation(SpiceConsoleModel.ClientConsoleMode.Plugin);
                    }
                }));
        registerHandler(getView().getSpiceHtml5ImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectSpiceImplementation(SpiceConsoleModel.ClientConsoleMode.Html5);
                    }
                }));

         registerHandler(getView().getNoVncImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectVncImplementation(VncConsoleModel.ClientConsoleMode.NoVnc);
                    }
                }));

         registerHandler(getView().getVncNativeImplRadioButton()
                 .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                     @Override
                     public void onValueChange(ValueChangeEvent<Boolean> event) {
                         getView().selectVncImplementation(VncConsoleModel.ClientConsoleMode.Native);
                     }
                 }));

        registerHandler(getView().getRdpAutoImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectRdpImplementation(RdpConsoleModel.ClientConsoleMode.Auto);
                    }
                }));

        registerHandler(getView().getRdpNativeImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectRdpImplementation(RdpConsoleModel.ClientConsoleMode.Native);
                    }
                }));

        registerHandler(getView().getRdpPluginImplRadioButton()
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        getView().selectRdpImplementation(RdpConsoleModel.ClientConsoleMode.Plugin);
                    }
                }));
    }

}
