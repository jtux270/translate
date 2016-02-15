package org.ovirt.engine.ui.common.widget.action;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import org.ovirt.engine.ui.common.CommonApplicationResources;

import java.util.List;

public class DropdownActionButton<T> extends AbstractActionButton {

    interface WidgetUiBinder extends UiBinder<FocusPanel, DropdownActionButton> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    @UiField
    Style style;

    @UiField
    FocusPanel container;

    @UiField(provided = true)
    ToggleButton dropdownButton;

    MenuPanelPopup menuPopup;

    private final static CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);

    public DropdownActionButton(List<ActionButtonDefinition<T>> actions, List selectedItems) {
        initDropdownButton();
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        initMenuPopup(actions, selectedItems);
        addMouseHandlers();
    }

    private void initDropdownButton() {
        dropdownButton = new ToggleButton(new Image(resources.triangle_down()));
        dropdownButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (dropdownButton.isDown()) {
                    menuPopup.asPopupPanel().showRelativeToAndFitToScreen(container);
                } else {
                    menuPopup.asPopupPanel().hide();
                }
            }
        });
    }

    private void initMenuPopup(List<ActionButtonDefinition<T>> actions, final List selectedItems) {
        menuPopup = new MenuPanelPopup(true);

        for (final ActionButtonDefinition<T> buttonDef : actions) {
            MenuItem menuItem = new MenuItem(buttonDef.getTitle(), new Command() {
                @Override
                public void execute() {
                    menuPopup.asPopupPanel().hide();
                    buttonDef.onClick(selectedItems);
                }
            });
            menuItem.addStyleName(style.menuItem());
            updateMenuItem(menuItem, buttonDef, selectedItems);
            menuPopup.getMenuBar().addItem(menuItem);
        }

        menuPopup.asPopupPanel().setAutoHideEnabled(true);
        menuPopup.asPopupPanel().addAutoHidePartner(dropdownButton.getElement());
        menuPopup.asPopupPanel().addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                dropdownButton.setDown(false);
            }
        });
    }

    /**
     * Ensures that the specified action button is visible or hidden and enabled or disabled as it should.
     */
    void updateMenuItem(MenuItem item, ActionButtonDefinition<T> buttonDef, List selectedItems) {
        item.setVisible(buttonDef.isAccessible(selectedItems) && buttonDef.isVisible(selectedItems));
        item.setEnabled(buttonDef.isEnabled(selectedItems));
        item.setTitle(buttonDef.getButtonToolTip() != null ? buttonDef.getButtonToolTip() : null);
    }

    private void addMouseHandlers() {
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                container.removeStyleName(style.buttonMouseOver());
                container.addStyleName(style.buttonMouseOut());
            }
        });

        container.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (isEnabled()) {
                    container.removeStyleName(style.buttonMouseOut());
                    container.addStyleName(style.buttonMouseOver());
                }
            }
        });

        container.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                container.removeStyleName(style.buttonMouseOver());
                container.addStyleName(style.buttonMouseOut());
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        button.setEnabled(enabled);
        dropdownButton.setEnabled(enabled);
    }

    interface Style extends CssResource {
        String buttonMouseOver();

        String buttonMouseOut();

        String menuItem();
    }

}
