package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.compat.Guid;

/**
 * A set which ensures that the unique key of permissions table will not be violated
 */
public class UniquePermissionsSet extends HashSet<PermissionWithUniqueKeyEquals> {

    public void addPermission(Guid adElementId, Guid roleId, Guid objectId, VdcObjectType objectType) {
        add(new PermissionWithUniqueKeyEquals(adElementId, roleId, objectId, objectType));
    }

    /**
     * Converts to instances of permissions class - the rest of the code expects the getClass() to be permissions
     * and not it's child
     */
    public List<Permissions> asPermissionList() {
        List<Permissions> res = new ArrayList<Permissions>();

        for (PermissionWithUniqueKeyEquals permission : this) {
            res.add(permission.asPermission());
        }

        return res;
    }

}

/**
 * Permission which offers the same equals as the unique key constraint on the permissions table
 */
class PermissionWithUniqueKeyEquals extends Permissions {

    public PermissionWithUniqueKeyEquals(Guid adElementId, Guid roleId, Guid objectId, VdcObjectType objectType) {
        super(adElementId, roleId, objectId, objectType);
    }

    public Permissions asPermission() {
        return new Permissions(getad_element_id(), getrole_id(), getObjectId(), getObjectType());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getad_element_id() == null) ? 0 : getad_element_id().hashCode());
        result = prime * result + ((getObjectId() == null) ? 0 : getObjectId().hashCode());
        result = prime * result + ((getrole_id() == null) ? 0 : getrole_id().hashCode());
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
        Permissions other = (Permissions) obj;
        return Objects.equals(getad_element_id(), other.getad_element_id())
                && Objects.equals(getrole_id(), other.getrole_id())
                && Objects.equals(getObjectId(), other.getObjectId());
    }
}
