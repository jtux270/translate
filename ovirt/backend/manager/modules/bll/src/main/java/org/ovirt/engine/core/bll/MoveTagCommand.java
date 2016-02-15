package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.MoveTagParameters;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class MoveTagCommand<T extends MoveTagParameters> extends TagsCommandBase<T> {
    private String _oldParnetTagName = "[null]";

    public MoveTagCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        if (getParameters().getNewParentId().equals(getParameters().getTagId())) {
            addCanDoActionMessage(VdcBllMessages.TAGS_SPECIFIED_TAG_CANNOT_BE_THE_PARENT_OF_ITSELF);
            returnValue = false;
        }
        if (TagsDirector.getInstance()
                .isTagDescestorOfTag(getParameters().getTagId(), getParameters().getNewParentId())) {
            addCanDoActionMessage(VdcBllMessages.TAGS_SPECIFIED_TAG_CANNOT_BE_THE_PARENT_OF_ITSELF);
            returnValue = false;
        }
        return returnValue;
    }

    public String getOldParnetTagName() {
        return _oldParnetTagName;
    }

    public String getNewParentTagName() {
        Tags newParent = TagsDirector.getInstance().GetTagById(getParameters().getNewParentId());
        if (newParent != null) {
            return newParent.gettag_name();
        }

        return "[null]";
    }

    private void InitOldParentTagName() {
        if (getTag() != null && getTag().getparent_id() != null) {
            Tags parent = TagsDirector.getInstance().GetTagById(new Guid(getTag().getparent_id().toString()));
            if (parent != null) {
                _oldParnetTagName = parent.gettag_name();
            }
        }
    }

    @Override
    protected void executeCommand() {
        InitOldParentTagName();
        TagsDirector.getInstance().MoveTag(getParameters().getTagId(), getParameters().getNewParentId());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_MOVE_TAG : AuditLogType.USER_MOVE_TAG_FAILED;
    }
}
