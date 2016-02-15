package org.ovirt.engine.ui.common.widget.table.column;

import java.util.Comparator;

import org.ovirt.engine.core.common.businessentities.comparators.LexoNumericComparator;


/**
 * Column for displaying text using {@link TextCellWithTooltip}.
 *
 * @param <T>
 *            Table row data type.
 */
public abstract class TextColumnWithTooltip<T> extends SortableColumn<T, String> implements ColumnWithElementId {

    public TextColumnWithTooltip() {
        this(TextCellWithTooltip.UNLIMITED_LENGTH);
    }

    public TextColumnWithTooltip(int maxTextLength) {
        this(new TextCellWithTooltip(maxTextLength));
    }

    public TextColumnWithTooltip(TextCellWithTooltip cell) {
        super(cell);
    }

    @Override
    public void configureElementId(String elementIdPrefix, String columnId) {
        getCell().setElementIdPrefix(elementIdPrefix);
        getCell().setColumnId(columnId);
    }

    @Override
    public TextCellWithTooltip getCell() {
        return (TextCellWithTooltip) super.getCell();
    }

    public void setTitle(String tooltipText) {
        getCell().setTitle(tooltipText);
    }

    /**
     * Enables default <em>client-side</em> sorting for this column, by the String ordering of the displayed text.
     */
    public void makeSortable() {
        makeSortable(new Comparator<T>() {

            private LexoNumericComparator lexoNumeric = new LexoNumericComparator();

            @Override
            public int compare(T arg0, T arg1) {
                return lexoNumeric.compare(getValue(arg0), getValue(arg1));
            }
        });
    }
}
