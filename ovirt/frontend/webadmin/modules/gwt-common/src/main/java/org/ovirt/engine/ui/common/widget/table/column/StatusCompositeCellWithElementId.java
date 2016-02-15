package org.ovirt.engine.ui.common.widget.table.column;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.utils.ElementIdUtils;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class StatusCompositeCellWithElementId extends CompositeCellWithElementId<VM> implements CellWithElementId<VM>{
    public interface StatusCompositeCellResources extends ClientBundle {
        @ClientBundle.Source("org/ovirt/engine/ui/common/css/StatusCompositeCell.css")
        StatusCompositeCellCss statusCompositeCellCss();
    }

    public interface StatusCompositeCellCss extends CssResource {
        String divInlineBlock();
    }

    public interface ContentTemplate extends SafeHtmlTemplates {
        @Template("<div id=\"{0}\">")
        SafeHtml id(String id);
    }

    private static final StatusCompositeCellResources RESOURCES = GWT.create(StatusCompositeCellResources.class);
    private final StatusCompositeCellCss style;
    private final List<HasCell<VM, ?>> hasCells;

    private ContentTemplate template;

    public StatusCompositeCellWithElementId(List<HasCell<VM, ?>> hasCells) {
        super(hasCells);
        this.hasCells = hasCells;
        style = RESOURCES.statusCompositeCellCss();
        style.ensureInjected();
    }

    ContentTemplate getTemplate() {
        if (template == null) {
            template = GWT.create(ContentTemplate.class);
        }
        return template;
    }

    @Override
    public void render(Cell.Context context, VM value, SafeHtmlBuilder sb) {
        sb.append(getTemplate().id(ElementIdUtils.createTableCellElementId(
                getElementIdPrefix(), getColumnId(), context)));

        for (HasCell<VM, ?> hasCell : hasCells) {
            render(context, value, sb, hasCell);
        }

        sb.appendHtmlConstant("</div>"); //$NON-NLS-1$
    }

    @Override
    protected <T> void render(Cell.Context context, VM value,
                              SafeHtmlBuilder sb, HasCell<VM, T> hasCell) {
        Cell<T> cell = hasCell.getCell();
        if (cell instanceof HasStyleClass) {
            ((HasStyleClass) cell).setStyleClass(style.divInlineBlock());
        }
        cell.render(context, hasCell.getValue(value), sb);
    }
}
