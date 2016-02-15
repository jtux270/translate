package org.ovirt.engine.core.aaa;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.utils.ObjectUtils;

public class DirectoryEntry extends IVdcQueryable {
    private static final long serialVersionUID = -5689096270467866486L;

    /**
     * Reference to the directory where this entry was originated.
     */
    private String directoryName;

    // The values of the attributes:
    private String namespace;
    private String id;
    private String name;
    private DirectoryEntryStatus status;

    /**
     * The list of groups this entry belongs to.
     */
    private List<DirectoryGroup> groups;

    public DirectoryEntry(String directoryName, String namespace, String id, String name) {
        this.directoryName = directoryName;
        this.namespace = namespace;
        this.id = id;
        this.name = name;
        this.status = DirectoryEntryStatus.UNAVAILABLE;
        this.groups = new ArrayList<DirectoryGroup>(1);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DirectoryEntryStatus getStatus() {
        return status;
    }

    public void setStatus(DirectoryEntryStatus status) {
        this.status = status;
    }


    public List<DirectoryGroup> getGroups() {
        if (groups == null) {
            groups = new ArrayList<DirectoryGroup>();
        }
        return groups;
    }

    public void setGroups(List<DirectoryGroup> groups) {
        this.groups = groups;
    }

    @Override
    public Object getQueryableId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (id == null? 0: id.hashCode());
        result = prime * result + (name == null? 0: name.hashCode());
        result = prime * result + (status == null? 0: status.hashCode());
        result = prime * result + (directoryName == null ? 0 : directoryName.hashCode());
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
        DirectoryEntry other = (DirectoryEntry) obj;
        return (
                ObjectUtils.objectsEqual(id, other.id) &&
                ObjectUtils.objectsEqual(name, other.name) &&
                ObjectUtils.objectsEqual(status, other.status) &&
                ObjectUtils.objectsEqual(directoryName, other.directoryName)
               );
    }

    public String toString() {
        return name + "@" + directoryName;
    }
}
