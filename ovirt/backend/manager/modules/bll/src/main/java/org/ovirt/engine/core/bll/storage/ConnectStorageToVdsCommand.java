package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.context.CommandContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.errors.VdcFault;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.validation.NfsMountPointConstraint;
import org.ovirt.engine.core.common.vdscommands.StorageServerConnectionManagementVDSParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStringUtils;

@InternalCommandAttribute
public class ConnectStorageToVdsCommand<T extends StorageServerConnectionParametersBase> extends
        StorageServerConnectionCommandBase<T> {
    public ConnectStorageToVdsCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    public ConnectStorageToVdsCommand(T parameters) {
        this(parameters, null);
    }

    @Override
    protected void executeCommand() {
        Pair<Boolean, Integer> result = connectHostToStorage();
        setSucceeded(result.getFirst());
        if (!result.getFirst()) {
            setErrorMessageAtReturn(result);
        }
    }

    private void setErrorMessageAtReturn(Pair<Boolean, Integer> result) {
        VdcFault fault = new VdcFault();
        fault.setError(result.getSecond());
        if (fault.getError() != null) {
            fault.setMessage(
                    Backend.getInstance()
                            .getVdsErrorsTranslator()
                            .TranslateErrorTextSingle(fault.getError().toString()));
        }
        getReturnValue().setFault(fault);
    }

    protected Pair<Boolean, Integer> connectHostToStorage() {
        List<StorageServerConnections> connections = Arrays.asList(getConnection());

        if (getConnection().getstorage_type() == StorageType.ISCSI) {
            connections = ISCSIStorageHelper.updateIfaces(connections, getVds().getId());
        }

        Map<String, String> result = (HashMap<String, String>) runVdsCommand(
                VDSCommandType.ConnectStorageServer,
                new StorageServerConnectionManagementVDSParameters(getVds().getId(), Guid.Empty,
                        getConnection().getstorage_type(), connections)).getReturnValue();

        return new Pair<>(StorageHelperDirector.getInstance()
                .getItem(getConnection().getstorage_type())
                .isConnectSucceeded(result, connections),
                Integer.parseInt(result.values().iterator().next()));
    }

    protected boolean isValidStorageConnectionPort(String port) {
         return !StringUtils.isEmpty(port) && StringUtils.isNumeric(port) && Integer.parseInt(port) > 0;
    }

    protected boolean isValidConnection(StorageServerConnections conn) {
        StorageType storageType = conn.getstorage_type();

        if (storageType == StorageType.NFS && !new NfsMountPointConstraint().isValid(conn.getconnection(), null)) {
            return failCanDoAction(VdcBllMessages.VALIDATION_STORAGE_CONNECTION_INVALID);
        }

        if (storageType == StorageType.POSIXFS && (StringUtils.isEmpty(conn.getVfsType()))) {
            return failCanDoAction(VdcBllMessages.VALIDATION_STORAGE_CONNECTION_EMPTY_VFSTYPE);
        }

        if ((storageType == StorageType.POSIXFS || storageType == StorageType.NFS) && !validate(validateMountOptions())) {
            return false;
        }

        if (storageType == StorageType.ISCSI) {
            if (StringUtils.isEmpty(conn.getiqn())) {
                return failCanDoAction(VdcBllMessages.VALIDATION_STORAGE_CONNECTION_EMPTY_IQN);
            }
            if (!isValidStorageConnectionPort(conn.getport())) {
                return failCanDoAction(VdcBllMessages.VALIDATION_STORAGE_CONNECTION_INVALID_PORT);
            }
        }

        if (checkIsConnectionFieldEmpty(conn)) {
            return false;
        }

        return true;
    }

    private static final List<String> NFS_MANAGED_OPTIONS = Arrays.asList("timeo", "retrans", "vfs_type", "protocol_version", "nfsvers", "vers", "minorversion", "addr", "clientaddr");
    private static final List<String> POSIX_MANAGED_OPTIONS = Arrays.asList("vfs_type", "addr", "clientaddr");

    protected ValidationResult validateMountOptions() {
        String mountOptions = getConnection().getMountOptions();
        if (StringUtils.isBlank(mountOptions)) {
            return ValidationResult.VALID;
        }

        List<String> disallowedOptions =
                getConnection().getstorage_type() == StorageType.POSIXFS ? POSIX_MANAGED_OPTIONS : NFS_MANAGED_OPTIONS;
        Map<String, String> optionsMap = XmlRpcStringUtils.string2Map(mountOptions);

        Set<String> optionsKeys = new HashSet<>();
        for (String option : optionsMap.keySet()) {
            optionsKeys.add(option.toLowerCase());
        }

        optionsKeys.retainAll(disallowedOptions);

        if (!optionsKeys.isEmpty()) {
            addCanDoActionMessageVariable("invalidOptions", StringUtils.join(optionsKeys, ", "));
            return new ValidationResult(VdcBllMessages.VALIDATION_STORAGE_CONNECTION_MOUNT_OPTIONS_CONTAINS_MANAGED_PROPERTY);
        }

        return ValidationResult.VALID;
    }
}
