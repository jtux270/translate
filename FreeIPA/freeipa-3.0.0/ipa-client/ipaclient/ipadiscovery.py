# Authors: Simo Sorce <ssorce@redhat.com>
#
# Copyright (C) 2007  Red Hat
# see file 'COPYING' for use and warranty information
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

import socket
import os
import copy
from ipapython.ipa_log_manager import *
import tempfile
import ldap
from ldap import LDAPError

from ipapython.ipautil import run, CalledProcessError, valid_ip, get_ipa_basedn, \
                              realm_to_suffix, format_netloc
from ipapython.dn import DN
from ipapython import dnsclient

CACERT = '/etc/ipa/ca.crt'

NOT_FQDN = -1
NO_LDAP_SERVER = -2
REALM_NOT_FOUND = -3
NOT_IPA_SERVER = -4
NO_ACCESS_TO_LDAP = -5
NO_TLS_LDAP = -6
BAD_HOST_CONFIG = -10
UNKNOWN_ERROR = -15

error_names = {
    0: 'Success',
    NOT_FQDN: 'NOT_FQDN',
    NO_LDAP_SERVER: 'NO_LDAP_SERVER',
    REALM_NOT_FOUND: 'REALM_NOT_FOUND',
    NOT_IPA_SERVER: 'NOT_IPA_SERVER',
    NO_ACCESS_TO_LDAP: 'NO_ACCESS_TO_LDAP',
    NO_TLS_LDAP: 'NO_TLS_LDAP',
    BAD_HOST_CONFIG: 'BAD_HOST_CONFIG',
    UNKNOWN_ERROR: 'UNKNOWN_ERROR',
}

class IPADiscovery(object):

    def __init__(self):
        self.realm = None
        self.domain = None
        self.server = None
        self.servers = []
        self.basedn = None

        self.realm_source = None
        self.domain_source = None
        self.server_source = None
        self.basedn_source = None

    def __get_resolver_domains(self):
        """
        Read /etc/resolv.conf and return all the domains found in domain and
        search.

        Returns a list of (domain, info) pairs. The info contains a reason why
        the domain is returned.
        """
        domains = []
        domain = None
        try:
            fp = open('/etc/resolv.conf', 'r')
            lines = fp.readlines()
            fp.close()

            for line in lines:
                if line.lower().startswith('domain'):
                    domain = (line.split()[-1],
                        'local domain from /etc/resolv.conf')
                elif line.lower().startswith('search'):
                    domains += [(d, 'search domain from /etc/resolv.conf') for
                        d in line.split()[1:]]
        except:
            pass
        if domain:
            domains = [domain] + domains
        return domains

    def getServerName(self):
        return self.server

    def getDomainName(self):
        return self.domain

    def getRealmName(self):
        return self.realm

    def getKDCName(self):
        return self.kdc

    def getBaseDN(self):
        return self.basedn

    def check_domain(self, domain, tried, reason):
        """
        Given a domain search it for SRV records, breaking it down to search
        all subdomains too.

        Returns a tuple (servers, domain) or (None,None) if a SRV record
        isn't found. servers is a list of servers found. domain is a string.

        :param tried: A set of domains that were tried already
        :param reason: Reason this domain is searched (included in the log)
        """
        servers = None
        root_logger.debug('Start searching for LDAP SRV record in "%s" (%s) ' +
                          'and its sub-domains', domain, reason)
        while not servers:
            if domain in tried:
                root_logger.debug("Already searched %s; skipping", domain)
                break
            tried.add(domain)

            servers = self.ipadns_search_srv(domain, '_ldap._tcp', 389,
                break_on_first=False)
            if servers:
                return (servers, domain)
            else:
                p = domain.find(".")
                if p == -1: #no ldap server found and last component of the domain already tested
                    return (None, None)
                domain = domain[p+1:]
        return (None, None)

    def search(self, domain = "", servers = "", hostname=None, ca_cert_path=None):
        """
        Use DNS discovery to identify valid IPA servers.

        servers may contain an optional list of servers which will be used
        instead of discovering available LDAP SRV records.

        Returns a constant representing the overall search result.
        """
        root_logger.debug("[IPA Discovery]")
        root_logger.debug(
            'Starting IPA discovery with domain=%s, servers=%s, hostname=%s',
            domain, servers, hostname)

        self.server = None
        autodiscovered = False

        if not servers:

            if not domain: #domain not provided do full DNS discovery

                # get the local host name
                if not hostname:
                    hostname = socket.getfqdn()
                    root_logger.debug('Hostname: %s', hostname)
                if not hostname:
                    return BAD_HOST_CONFIG

                if valid_ip(hostname):
                    return NOT_FQDN

                # first, check for an LDAP server for the local domain
                p = hostname.find(".")
                if p == -1: #no domain name
                    return NOT_FQDN
                domain = hostname[p+1:]

                # Get the list of domains from /etc/resolv.conf, we'll search
                # them all. We search the domain of our hostname first though.
                # This is to avoid the situation where domain isn't set in
                # /etc/resolv.conf and the search list has the hostname domain
                # not first. We could end up with the wrong SRV record.
                domains = self.__get_resolver_domains()
                domains = [(domain, 'domain of the hostname')] + domains
                tried = set()
                for domain, reason in domains:
                    servers, domain = self.check_domain(domain, tried, reason)
                    if servers:
                        autodiscovered = True
                        self.domain = domain
                        self.server_source = self.domain_source = (
                            'Discovered LDAP SRV records from %s (%s)' %
                                (domain, reason))
                        break
                if not self.domain: #no ldap server found
                    root_logger.debug('No LDAP server found')
                    return NO_LDAP_SERVER
            else:
                root_logger.debug("Search for LDAP SRV record in %s", domain)
                servers = self.ipadns_search_srv(domain, '_ldap._tcp', 389,
                                                 break_on_first=False)
                if servers:
                    autodiscovered = True
                    self.domain = domain
                    self.server_source = self.domain_source = (
                        'Discovered LDAP SRV records from %s' % domain)
                else:
                    self.server = None
                    root_logger.debug('No LDAP server found')
                    return NO_LDAP_SERVER

        else:

            root_logger.debug("Server and domain forced")
            self.domain = domain
            self.domain_source = self.server_source = 'Forced'

        #search for kerberos
        root_logger.debug("[Kerberos realm search]")
        krb_realm, kdc = self.ipadnssearchkrb(self.domain)
        if not servers and not krb_realm:
            return REALM_NOT_FOUND

        self.realm = krb_realm
        self.kdc = kdc
        self.realm_source = self.kdc_source = (
            'Discovered Kerberos DNS records from %s' % self.domain)

        # We may have received multiple servers corresponding to the domain
        # Iterate through all of those to check if it is IPA LDAP server
        ldapret = [NOT_IPA_SERVER]
        ldapaccess = True
        root_logger.debug("[LDAP server check]")
        valid_servers = []
        for server in servers:
            root_logger.debug('Verifying that %s (realm %s) is an IPA server',
                server, self.realm)
            # check ldap now
            ldapret = self.ipacheckldap(server, self.realm, ca_cert_path=ca_cert_path)

            if ldapret[0] == 0:
                self.server = ldapret[1]
                self.realm = ldapret[2]
                self.server_source = self.realm_source = (
                    'Discovered from LDAP DNS records in %s' % self.server)
                valid_servers.insert(0, server)
                # verified, we actually talked to the remote server and it
                # is definetely an IPA server
                if autodiscovered:
                    # No need to keep verifying servers if we discovered them
                    # via DNS
                    break
            elif ldapret[0] == NO_ACCESS_TO_LDAP or ldapret[0] == NO_TLS_LDAP:
                ldapaccess = False
                valid_servers.insert(0, server)
                # we may set verified_servers below, we don't have it yet
                if autodiscovered:
                    # No need to keep verifying servers if we discovered them
                    # via DNS
                    break
            elif ldapret[0] == NOT_IPA_SERVER:
                root_logger.warn(
                    '%s (realm %s) is not an IPA server', server, self.realm)
            elif ldapret[0] == NO_LDAP_SERVER:
                root_logger.debug(
                    'Unable to verify that %s (realm %s) is an IPA server',
                                    server, self.realm)

        # If one of LDAP servers checked rejects access (maybe anonymous
        # bind is disabled), assume realm and basedn generated off domain.
        # Note that in case ldapret[0] == 0 and ldapaccess == False (one of
        # servers didn't provide access but another one succeeded), self.realm
        # will be set already to a proper value above, self.basdn will be
        # initialized during the LDAP check itself and we'll skip these two checks.
        if not ldapaccess and self.realm is None:
            # Assume realm is the same as domain.upper()
            self.realm = self.domain.upper()
            self.realm_source = 'Assumed same as domain'
            root_logger.debug(
                "Assuming realm is the same as domain: %s", self.realm)

        if not ldapaccess and self.basedn is None:
            # Generate suffix from realm
            self.basedn = realm_to_suffix(self.realm)
            self.basedn_source = 'Generated from Kerberos realm'
            root_logger.debug("Generated basedn from realm: %s" % self.basedn)

        root_logger.debug(
            "Discovery result: %s; server=%s, domain=%s, kdc=%s, basedn=%s",
            error_names.get(ldapret[0], ldapret[0]),
            self.server, self.domain, self.kdc, self.basedn)

        root_logger.debug("Validated servers: %s" % ','.join(valid_servers))
        self.servers = valid_servers

        # If we have any servers left then override the last return value
        # to indicate success.
        if valid_servers:
            self.server = servers[0]
            ldapret[0] = 0

        return ldapret[0]

    def ipacheckldap(self, thost, trealm, ca_cert_path=None):
        """
        Given a host and kerberos realm verify that it is an IPA LDAP
        server hosting the realm.

        Returns a list [errno, host, realm] or an empty list on error.
        Errno is an error number:
            0 means all ok
            1 means we could not check the info in LDAP (may happend when
                anonymous binds are disabled)
            2 means the server is certainly not an IPA server
        """

        lrealms = []

        i = 0

        #now verify the server is really an IPA server
        try:
            ldap_url = "ldap://" + format_netloc(thost, 389)
            root_logger.debug("Init LDAP connection with: %s", ldap_url)
            lh = ldap.initialize(ldap_url)
            if ca_cert_path:
                ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, True)
                ldap.set_option(ldap.OPT_X_TLS_CACERTFILE, ca_cert_path)
                lh.set_option(ldap.OPT_X_TLS_DEMAND, True)
                lh.start_tls_s()
            lh.set_option(ldap.OPT_PROTOCOL_VERSION, 3)
            lh.simple_bind_s("","")

            # get IPA base DN
            root_logger.debug("Search LDAP server for IPA base DN")
            basedn = get_ipa_basedn(lh)

            if basedn is None:
                root_logger.debug("The server is not an IPA server")
                return [NOT_IPA_SERVER]

            self.basedn = basedn
            self.basedn_source = 'From IPA server %s' % ldap_url

            #search and return known realms
            root_logger.debug(
                "Search for (objectClass=krbRealmContainer) in %s (sub)",
                self.basedn)
            lret = lh.search_s(str(DN(('cn', 'kerberos'), self.basedn)), ldap.SCOPE_SUBTREE, "(objectClass=krbRealmContainer)")
            if not lret:
                #something very wrong
                return [REALM_NOT_FOUND]

            for lres in lret:
                root_logger.debug("Found: %s", lres[0])
                for lattr in lres[1]:
                    if lattr.lower() == "cn":
                        lrealms.append(lres[1][lattr][0])


            if trealm:
                for r in lrealms:
                    if trealm == r:
                        return [0, thost, trealm]
                # must match or something is very wrong
                return [REALM_NOT_FOUND]
            else:
                if len(lrealms) != 1:
                    #which one? we can't attach to a multi-realm server without DNS working
                    return [REALM_NOT_FOUND]
                else:
                    return [0, thost, lrealms[0]]

            #we shouldn't get here
            return [UNKNOWN_ERROR]

        except LDAPError, err:
            if isinstance(err, ldap.TIMEOUT):
                root_logger.debug("LDAP Error: timeout")
                return [NO_LDAP_SERVER]

            if isinstance(err, ldap.SERVER_DOWN):
                root_logger.debug("LDAP Error: server down")
                return [NO_LDAP_SERVER]

            if isinstance(err, ldap.INAPPROPRIATE_AUTH):
                root_logger.debug("LDAP Error: Anonymous access not allowed")
                return [NO_ACCESS_TO_LDAP]

            # We should only get UNWILLING_TO_PERFORM if the remote LDAP server
            # has minssf > 0 and we have attempted a non-TLS connection.
            if ca_cert_path is None and isinstance(err, ldap.UNWILLING_TO_PERFORM):
                root_logger.debug("LDAP server returned UNWILLING_TO_PERFORM. This likely means that minssf is enabled")
                return [NO_TLS_LDAP]

            root_logger.error("LDAP Error: %s: %s" %
               (err.args[0]['desc'], err.args[0].get('info', '')))
            return [UNKNOWN_ERROR]


    def ipadns_search_srv(self, domain, srv_record_name, default_port,
                          break_on_first=True):
        """
        Search for SRV records in given domain. When no record is found,
        en empty list is returned

        :param domain: Search domain name
        :param srv_record_name: SRV record name, e.g. "_ldap._tcp"
        :param default_port: When default_port is not None, it is being
                    checked with the port in SRV record and if they don't
                    match, the port from SRV record is appended to
                    found hostname in this format: "hostname:port"
        :param break_on_first: break on the first find and return just one
                    entry
        """
        servers = []

        qname = '%s.%s' % (srv_record_name, domain)
        if not qname.endswith("."):
            qname += "."

        root_logger.debug("Search DNS for SRV record of %s", qname)

        results = dnsclient.query(qname, dnsclient.DNS_C_IN, dnsclient.DNS_T_SRV)
        if not results:
            root_logger.debug("No DNS record found")

        for result in results:
            if result.dns_type == dnsclient.DNS_T_SRV:
                root_logger.debug("DNS record found: %s", result)
                server = result.rdata.server.rstrip(".")
                if not server:
                    root_logger.debug("Cannot parse the hostname from SRV record: %s", result)
                    continue
                if default_port is not None and \
                        result.rdata.port and result.rdata.port != default_port:
                    server = "%s:%s" % (server, result.rdata.port)
                servers.append(server)
                if break_on_first:
                    break

        return servers

    def ipadnssearchkrb(self, tdomain):
        realm = None
        kdc = None
        # now, check for a Kerberos realm the local host or domain is in
        qname = "_kerberos." + tdomain
        if not qname.endswith("."):
            qname += "."

        root_logger.debug("Search DNS for TXT record of %s", qname)

        results = dnsclient.query(qname, dnsclient.DNS_C_IN, dnsclient.DNS_T_TXT)
        if not results:
            root_logger.debug("No DNS record found")

        for result in results:
            if result.dns_type == dnsclient.DNS_T_TXT:
                root_logger.debug("DNS record found: %s", result)
                if result.rdata.data:
                    realm = result.rdata.data
                if realm:
                    break

        if realm:
            # now fetch server information for the realm
            domain = realm.lower()

            kdc = self.ipadns_search_srv(domain, '_kerberos._udp', 88,
                    break_on_first=False)

            if kdc:
                kdc = ','.join(kdc)
            else:
                root_logger.debug("SRV record for KDC not found! Realm: %s, SRV record: %s" % (realm, qname))
                kdc = None

        return realm, kdc
