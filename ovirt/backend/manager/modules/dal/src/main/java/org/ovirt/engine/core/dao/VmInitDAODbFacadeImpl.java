package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.VmInit;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.ovirt.engine.core.utils.VmInitUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class VmInitDAODbFacadeImpl extends BaseDAODbFacade implements VmInitDAO {

    @Override
    public VmInit get(Guid id) {
        VmInit vmInit = getCallsHandler().executeRead("GetVmInitByVmId",
                VMInitRowMapper.instance,
                getIdParamterSource(id));
        return vmInit;
    }

    @Override
    public List<VmInit> getAll() {
        throw new NotImplementedException();
    }

    @Override
    public void save(VmInit entity) {
        getCallsHandler().executeModification("InsertVmInit", getFullParameterSource(entity));
    }

    @Override
    public void update(VmInit entity) {
        getCallsHandler().executeModification("UpdateVmInit", getFullParameterSource(entity));
    }

    @Override
    public void remove(Guid id) {
        getCallsHandler().executeModification("DeleteVmInit", getIdParamterSource(id));
    }

    @Override
    public List<VmInit> getVmInitByIds(List<Guid> ids) {
        return getCallsHandler().executeReadList("GetVmInitByids",
                VMInitRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("vm_init_ids", StringUtils.join(ids, ',')));
    }

    private MapSqlParameterSource getIdParamterSource(Guid id) {
        return getCustomMapSqlParameterSource()
                .addValue("vm_id", id);
    }

    private MapSqlParameterSource getFullParameterSource(VmInit vmInit) {
        return getCustomMapSqlParameterSource()
                .addValue("vm_id", vmInit.getId())
                .addValue("host_name", vmInit.getHostname())
                .addValue("domain", vmInit.getDomain())
                .addValue("authorized_keys", vmInit.getAuthorizedKeys())
                .addValue("regenerate_keys", vmInit.getRegenerateKeys())
                .addValue("dns_servers", vmInit.getDnsServers())
                .addValue("dns_search_domains", vmInit.getDnsSearch())
                .addValue("time_zone", vmInit.getTimeZone())
                .addValue("networks", VmInitUtils.networkListToJson(vmInit.getNetworks()))
                .addValue("password", DbFacadeUtils.encryptPassword(vmInit.getRootPassword()))
                .addValue("winkey", vmInit.getWinKey())
                .addValue("custom_script", vmInit.getCustomScript())
                .addValue("input_locale", vmInit.getInputLocale())
                .addValue("ui_language", vmInit.getUiLanguage())
                .addValue("system_locale", vmInit.getSystemLocale())
                .addValue("user_locale", vmInit.getUserLocale())
                .addValue("user_name", vmInit.getUserName())
                .addValue("active_directory_ou", vmInit.getActiveDirectoryOU())
                .addValue("org_name", vmInit.getOrgName());
    }

    private static class VMInitRowMapper implements RowMapper<VmInit> {
        public static final VMInitRowMapper instance = new VMInitRowMapper();

        @Override
        public VmInit mapRow(ResultSet rs, int rowNum) throws SQLException {
            final VmInit entity = new VmInit();

            entity.setId(getGuidDefaultEmpty(rs, "vm_id"));
            entity.setHostname(rs.getString("host_name"));
            entity.setDomain(rs.getString("domain"));
            entity.setAuthorizedKeys(rs.getString("authorized_keys"));
            entity.setRegenerateKeys(rs.getBoolean("regenerate_keys"));
            entity.setTimeZone(rs.getString("time_zone"));
            entity.setNetworks(VmInitUtils.jsonNetworksToList((rs.getString("networks"))));
            entity.setRootPassword(DbFacadeUtils.decryptPassword(rs.getString("password")));
            entity.setWinKey(rs.getString("winkey"));
            entity.setDnsServers(rs.getString("dns_servers"));
            entity.setDnsSearch(rs.getString("dns_search_domains"));
            entity.setCustomScript(rs.getString("custom_script"));
            entity.setInputLocale(rs.getString("input_locale"));
            entity.setUiLanguage(rs.getString("ui_language"));
            entity.setSystemLocale(rs.getString("system_locale"));
            entity.setUserLocale(rs.getString("user_locale"));
            entity.setUserName(rs.getString("user_name"));
            entity.setActiveDirectoryOU(rs.getString("active_directory_ou"));
            entity.setOrgName(rs.getString("org_name"));

            return entity;
        }
    }
}
