package org.ovirt.engine.ui.common.widget.form;

import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;

import com.google.gwt.user.client.ui.Widget;

/**
 * Represents a name/value item rendered within an {@link AbstractFormPanel}.
 */
public class FormItem {

    /**
     * If {@link #showDefaultValue} returns {@code true}, show the default value, otherwise show the actual value.
     */
    public interface DefaultValueCondition {

        boolean showDefaultValue();

    }

    private static final int UNASSIGNED_ROW = -1;

    private AbstractFormPanel formPanel;

    private int row;
    private final int column;

    private final String isAvailablePropertyName;
    private boolean isAvailable = true;

    private String name;
    private Widget valueWidget;

    private TextBoxLabel defaultValueLabel;
    private DefaultValueCondition defaultValueCondition;

    private boolean autoPlacement = false;
    private int autoPlacementRow = UNASSIGNED_ROW;

    public FormItem(int row, int column) {
        this("", new TextBoxLabel(), row, column); //$NON-NLS-1$
    }

    public FormItem(String name, Widget valueWidget, int column) {
        this(name, valueWidget, 0 , column, null, true);
        withAutoPlacement();
    }

    public FormItem(String name, Widget valueWidget, int column, boolean isAvailable) {
        this(name, valueWidget, 0 , column, null, isAvailable);
        withAutoPlacement();
    }

    public FormItem(String name, Widget valueWidget, int row, int column) {
        this(name, valueWidget, row, column, null);
    }

    public FormItem(String name, Widget valueWidget, int row, int column, boolean isAvailable) {
        this(name, valueWidget, row, column, null, isAvailable);
    }

    public FormItem(String name, Widget valueWidget, int row, int column, String isAvailablePropertyName) {
        this(name, valueWidget, row, column, isAvailablePropertyName, true);
    }

    public FormItem(String name,
            Widget valueWidget,
            int row,
            int column,
            String isAvailablePropertyName,
            boolean isAvailable) {
        this.row = row;
        this.column = column;
        this.isAvailablePropertyName = isAvailablePropertyName;
        this.valueWidget = valueWidget;
        this.isAvailable = isAvailable;

        // Add trailing ':' to the item name, if necessary
        if (name != null && !name.trim().isEmpty() && !name.endsWith(":")) { //$NON-NLS-1$
            this.name = name + ":"; //$NON-NLS-1$
        } else {
            this.name = name;
        }
    }

    /**
     * Use {@code defaultValue} based on dynamic {@code defaultValueCondition}.
     */
    public FormItem withDefaultValue(String defaultValue, DefaultValueCondition defaultValueCondition) {
        this.defaultValueLabel = new TextBoxLabel(defaultValue);
        this.defaultValueCondition = defaultValueCondition;
        return this;
    }

    /**
     * Automatically place this form item into next available row for the given column.
     * <p>
     * This works only when the form item is {@linkplain #getIsAvailable available} when being added to the
     * {@link AbstractFormPanel}. Otherwise, the form item is marked as dead and will be discarded by the form panel.
     *
     * @see #setFormPanel(AbstractFormPanel)
     */
    public FormItem withAutoPlacement() {
        this.autoPlacement = true;
        return this;
    }

    public void setFormPanel(AbstractFormPanel formPanel) {
        this.formPanel = formPanel;

        // Compute autoPlacementRow if this item is available
        if (autoPlacement && getIsAvailable()) {
            this.autoPlacementRow = formPanel.getNextAvailableRow(column);
        }
    }

    /**
     * If {@code false}, this form item shouldn't be processed by the {@link AbstractFormPanel}.
     */
    public boolean isValid() {
        if (autoPlacement && autoPlacementRow == UNASSIGNED_ROW) {
            // Failed to assign autoPlacementRow
            return false;
        }
        return true;
    }

    public void update() {
        assert formPanel != null : "FormItem must be adopted by FormPanel"; //$NON-NLS-1$
        formPanel.updateFormItem(this);
    }

    public int getRow() {
        return (autoPlacement && autoPlacementRow != UNASSIGNED_ROW) ? autoPlacementRow : row;
    }

    public int getColumn() {
        return column;
    }

    public String getIsAvailablePropertyName() {
        return isAvailablePropertyName;
    }

    public boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
        update();
    }

    public String getName() {
        return name;
    }

    /**
     * Resolves the appropriate value widget for this form item based on {@link DefaultValueCondition} (if any).
     */
    public Widget resolveValueWidget() {
        boolean showDefaultValue = defaultValueCondition != null && defaultValueCondition.showDefaultValue();
        return showDefaultValue ? defaultValueLabel : valueWidget;
    }

}
