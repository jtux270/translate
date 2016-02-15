package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.compat.Guid;

public class Tags implements Serializable {
    private static final long serialVersionUID = -6566155246916011274L;

    private Guid id;

    @Size(min = 1, max = BusinessEntitiesDefinitions.TAG_NAME_SIZE)
    @Pattern(regexp = ValidationUtils.NO_SPECIAL_CHARACTERS_I18N, message = "VALIDATION.TAGS.INVALID_TAG_NAME")
    private String name;

    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    private String description;

    private Guid parent;

    private Boolean readonly;

    private TagsType type;

    private List<Tags> _children;

    public Tags() {
        _children = new ArrayList<Tags>();
        type = TagsType.GeneralTag;
    }

    public Tags(String description, Guid parent_id, Boolean isReadonly, Guid tag_id, String tag_name) {
        this();
        this.description = description;
        this.parent = parent_id;
        this.readonly = isReadonly;
        this.id = tag_id;
        this.name = tag_name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((_children == null) ? 0 : _children.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((readonly == null) ? 0 : readonly.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Tags other = (Tags) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(_children, other._children)
                && ObjectUtils.objectsEqual(description, other.description)
                && ObjectUtils.objectsEqual(parent, other.parent)
                && ObjectUtils.objectsEqual(readonly, other.readonly)
                && ObjectUtils.objectsEqual(name, other.name)
                && type == other.type);
    }

    public String getdescription() {
        return this.description;
    }

    public void setdescription(String description) {
        this.description = description;
    }

    public Guid getparent_id() {
        return this.parent;
    }

    public void setparent_id(Guid parent) {
        this.parent = parent;
    }

    public Boolean getIsReadonly() {
        return this.readonly;
    }

    public void setIsReadonly(Boolean readOnly) {
        this.readonly = readOnly;
    }

    public Guid gettag_id() {
        return this.id;
    }

    public void settag_id(Guid id) {
        this.id = id;
    }

    public String gettag_name() {
        return this.name;
    }

    public void settag_name(String name) {
        this.name = name;
    }

    public TagsType gettype() {
        return this.type;
    }

    public void settype(TagsType type) {
        this.type = type;
    }

    public List<Tags> getChildren() {
        return _children;
    }

    public void setChildren(List<Tags> children) {
        _children = children;
    }

    public StringBuilder getTagIdAndChildrenIds() {
        StringBuilder builder = new StringBuilder();
        builder.append("'").append(gettag_id()).append("'");

        for (Tags tag : _children) {
            builder.append(",").append(tag.getTagIdAndChildrenIds());
        }
        return builder;
    }

    public StringBuilder getTagNameAndChildrenNames() {
        StringBuilder builder = new StringBuilder();
        builder.append("'").append(gettag_name()).append("'");

        for (Tags tag : _children) {
            builder.append("," + tag.getTagNameAndChildrenNames());
        }
        return builder;
    }

    public void getTagIdAndChildrenIdsAsList(HashSet<Guid> tagIds) {
        tagIds.add(gettag_id());
        for (Tags tag : _children) {
            tag.getTagIdAndChildrenIdsAsList(tagIds);
        }
    }
}
