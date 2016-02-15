package org.ovirt.engine.core.vdsbroker;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.protocol.Protocol;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.VdsProtocol;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.vdsbroker.irsbroker.IIrsServer;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsServerConnector;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsServerWrapper;
import org.ovirt.engine.core.vdsbroker.jsonrpc.JsonRpcIIrsServer;
import org.ovirt.engine.core.vdsbroker.jsonrpc.JsonRpcUtils;
import org.ovirt.engine.core.vdsbroker.jsonrpc.JsonRpcVdsServer;
import org.ovirt.engine.core.vdsbroker.vdsbroker.IVdsServer;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsServerConnector;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsServerWrapper;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcUtils;

public class TransportFactory {
    public static IIrsServer createIrsServer(VdsProtocol vdsProtocol,
            Version version,
            String hostname,
            int port,
            int clientTimeOut,
            int connectionTimeOut,
            int clientRetries,
            int heartbeat) {
        IIrsServer irsServer = null;
        if (VdsProtocol.STOMP == vdsProtocol) {
            irsServer = new JsonRpcIIrsServer(JsonRpcUtils.createStompClient(hostname,
                    port, connectionTimeOut, clientTimeOut, clientRetries, heartbeat,
                    Config.<Boolean> getValue(ConfigValues.EncryptHostCommunication),
                    Config.<String> getValue(ConfigValues.VdsmSSLProtocol),
                    Config.<Integer> getValue(ConfigValues.EventProcessingPoolSize),
                    Config.<String> getValue(ConfigValues.IrsRequestQueueName),
                    Config.<String> getValue(ConfigValues.IrsResponseQueueName),
                    null));
        } else if (VdsProtocol.XML == vdsProtocol){
            Pair<IrsServerConnector, HttpClient> returnValue =
                    XmlRpcUtils.getConnection(hostname, port, clientTimeOut, connectionTimeOut,
                            clientRetries,
                            Config.<Integer> getValue(ConfigValues.IrsMaxConnectionsPerHost),
                            Config.<Integer> getValue(ConfigValues.MaxTotalConnections),
                            IrsServerConnector.class, Config.<Boolean> getValue(ConfigValues.EncryptHostCommunication));
            irsServer = new IrsServerWrapper(returnValue.getFirst(), returnValue.getSecond());
        }
        return irsServer;
    }

    public static IVdsServer createVdsServer(VdsProtocol vdsProtocol,
            Version version,
            String hostname,
            int port,
            int clientTimeOut,
            int connectionTimeOut,
            int clientRetries,
            int heartbeat) {
        IVdsServer vdsServer = null;
        Pair<VdsServerConnector, HttpClient> returnValue =
                XmlRpcUtils.getConnection(hostname, port, clientTimeOut, connectionTimeOut,
                        clientRetries,
                        Config.<Integer> getValue(ConfigValues.VdsMaxConnectionsPerHost),
                        Config.<Integer> getValue(ConfigValues.MaxTotalConnections),
                        VdsServerConnector.class,
                        Config.<Boolean> getValue(ConfigValues.EncryptHostCommunication));

        if (VdsProtocol.STOMP == vdsProtocol) {
            String eventQueue = getEventsQueue(version);
            vdsServer = new JsonRpcVdsServer(JsonRpcUtils.createStompClient(hostname,
                    port, connectionTimeOut, clientTimeOut, clientRetries, heartbeat,
                    Config.<Boolean> getValue(ConfigValues.EncryptHostCommunication),
                    Config.<String> getValue(ConfigValues.VdsmSSLProtocol),
                    Config.<Integer> getValue(ConfigValues.EventProcessingPoolSize),
                    Config.<String> getValue(ConfigValues.VdsRequestQueueName),
                    Config.<String> getValue(ConfigValues.VdsResponseQueueName),
                    eventQueue),
                    returnValue.getSecond());
        } else if (VdsProtocol.XML == vdsProtocol) {
            HttpClient httpClient = returnValue.getSecond();
            String protocol = Config.<Boolean> getValue(ConfigValues.EncryptHostCommunication) ? "https" : "http";
            httpClient.getHostConfiguration().setHost(hostname, port, Protocol.getProtocol(protocol));
            vdsServer = new VdsServerWrapper(returnValue.getFirst(), httpClient);
        }
        return vdsServer;
    }

    private static String getEventsQueue(Version version) {
        String eventsQueue = null;
        if (FeatureSupported.events(version)) {
            eventsQueue = Config.<String> getValue(ConfigValues.EventQueueName);
        }
        return eventsQueue;
    }
}
