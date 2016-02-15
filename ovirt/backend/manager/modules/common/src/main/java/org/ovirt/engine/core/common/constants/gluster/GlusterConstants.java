package org.ovirt.engine.core.common.constants.gluster;

public class GlusterConstants {
    public static final int CODE_SUCCESS = 0;
    public static final String ON = "on";
    public static final String OFF = "off";

    public static final String ENABLE = "enable";
    public static final String DISABLE = "disable";

    public static final String OPTION_AUTH_ALLOW = "auth.allow";
    public static final String OPTION_NFS_DISABLE = "nfs.disable";
    public static final String OPTION_USER_CIFS = "user.cifs";
    public static final String OPTION_GROUP = "group";

    public static final String NO_OF_BRICKS = "NoOfBricks";
    public static final String BRICK_PATH = "brickpath";
    public static final String SERVER_NAME = "servername";
    public static final String VOLUME_NAME = "glustervolumename";

    // Variables used in audit messages.
    // Keep the values lowercase to avoid call to String#toLowerCase()
    public static final String CLUSTER = "cluster";
    public static final String VOLUME = "glustervolume";
    public static final String BRICK = "brick";
    public static final String OPTION_KEY = "key";
    public static final String OPTION_VALUE = "value";
    public static final String OPTION_OLD_VALUE = "oldvalue";
    public static final String OPTION_NEW_VALUE = "newvalue";
    public static final String OLD_STATUS = "oldstatus";
    public static final String NEW_STATUS = "newstatus";
    public static final String SERVICE_TYPE = "servicetype";
    public static final String SERVICE_NAME = "servicename";
    public static final String MANAGE_GLUSTER_SERVICE_ACTION_TYPE_START = "start";
    public static final String MANAGE_GLUSTER_SERVICE_ACTION_TYPE_STOP = "stop";
    public static final String MANAGE_GLUSTER_SERVICE_ACTION_TYPE_RESTART = "restart";
    public static final String COMMAND = "command";

    public static final String HOOK_NAME = "glusterhookname";
    public static final String FAILURE_MESSAGE = "failuremessage";
    public static final String JOB_STATUS = "status";
    public static final String JOB_INFO = "info";
}
