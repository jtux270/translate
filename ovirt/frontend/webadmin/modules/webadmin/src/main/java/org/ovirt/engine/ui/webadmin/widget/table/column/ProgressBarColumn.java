package org.ovirt.engine.ui.webadmin.widget.table.column;

import java.util.Comparator;

import org.ovirt.engine.ui.common.widget.table.column.SafeHtmlColumn;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Column for displaying generic progress bar.
 *
 * @param <T>
 *            Table row data type.
 */
public abstract class ProgressBarColumn<T> extends SafeHtmlColumn<T> {

    public static enum ProgressBarColors {
        GREEN("#669966"), //$NON-NLS-1$
        ORANGE("#FF9900"), //$NON-NLS-1$
        RED("#FF0000"); //$NON-NLS-1$

        private final String colorCode;

        private ProgressBarColors(String colorCode) {
            this.colorCode = colorCode;
        }

        public String asCode() {
            return colorCode;
        }
    }

    @Override
    public final SafeHtml getValue(T object) {
        Integer progressValue = getProgressValue(object);

        int progress = progressValue != null ? progressValue : 0;
        String text = getProgressText(object);

        // Choose color by progress
        String color = getColorByProgress(progress);

        return ClientGinjectorProvider.getApplicationTemplates().progressBar(progress, text, color);
    }

    /**
     * Default color scheme for the progress bar - override if other colors are needed
     */
    protected String getColorByProgress(int progress) {
        if (progress < 70) {
            return ProgressBarColors.GREEN.asCode();
        } else if (progress < 95) {
            return ProgressBarColors.ORANGE.asCode();
        } else {
            return ProgressBarColors.RED.asCode();
        }
    }

    /**
     * Enables default <em>client-side</em> sorting for this column, by the integer value, as returned from
     * getProgressValue method.
     */
    public void makeSortable() {
        makeSortable(new Comparator<T>() {

            @Override
            public int compare(T arg0, T arg1) {
                return getProgressValue(arg0).compareTo(getProgressValue(arg1));
            }
        });
    }

    /**
     * Returns the progress value in percent ({@code null} values will be interpreted as zeroes).
     */
    protected abstract Integer getProgressValue(T object);

    /**
     * Returns the text to show within the progress bar.
     */
    protected abstract String getProgressText(T object);

}
