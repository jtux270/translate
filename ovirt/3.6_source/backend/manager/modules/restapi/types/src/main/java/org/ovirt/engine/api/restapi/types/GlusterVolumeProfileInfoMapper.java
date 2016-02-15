package org.ovirt.engine.api.restapi.types;

import static org.ovirt.engine.api.model.StatisticType.GAUGE;
import static org.ovirt.engine.api.model.StatisticUnit.BYTES;
import static org.ovirt.engine.api.model.StatisticUnit.SECONDS;

import java.util.List;

import org.ovirt.engine.api.model.BlockStatistic;
import org.ovirt.engine.api.model.BrickProfileDetail;
import org.ovirt.engine.api.model.BrickProfileDetails;
import org.ovirt.engine.api.model.EntityProfileDetail;
import org.ovirt.engine.api.model.FopStatistic;
import org.ovirt.engine.api.model.GlusterBrick;
import org.ovirt.engine.api.model.GlusterVolumeProfileDetails;
import org.ovirt.engine.api.model.NfsProfileDetail;
import org.ovirt.engine.api.model.NfsProfileDetails;
import org.ovirt.engine.api.model.ProfileDetail;
import org.ovirt.engine.api.model.Statistic;
import org.ovirt.engine.api.model.StatisticUnit;
import org.ovirt.engine.api.model.ValueType;
import org.ovirt.engine.api.restapi.utils.StatisticResourceUtils;
import org.ovirt.engine.core.common.businessentities.gluster.BlockStats;
import org.ovirt.engine.core.common.businessentities.gluster.FopStats;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeProfileInfo;
import org.ovirt.engine.core.common.businessentities.gluster.StatsInfo;

public class GlusterVolumeProfileInfoMapper {
    private final static Statistic BYTES_BLOCK_READ   = StatisticResourceUtils.create("block.bytes.read", "bytes read", GAUGE, BYTES, ValueType.INTEGER);
    private final static Statistic BYTES_BLOCK_WRITE   = StatisticResourceUtils.create("block.bytes.write", "bytes written", GAUGE, BYTES, ValueType.INTEGER);
    private final static Statistic BLOCK_SIZE   = StatisticResourceUtils.create("block.size", "block size", GAUGE, BYTES, ValueType.INTEGER);
    private final static Statistic TOTAL_READ   = StatisticResourceUtils.create("total.bytes.read", "total bytes read", GAUGE, BYTES, ValueType.INTEGER);
    private final static Statistic TOTAL_WRITE   = StatisticResourceUtils.create("total.bytes.write", "blockSize", GAUGE, BYTES, ValueType.INTEGER);
    private final static Statistic MIN_LATENCY   = StatisticResourceUtils.create("min.latency", "minimum latency", GAUGE, SECONDS, ValueType.INTEGER);
    private final static Statistic MAX_LATENCY   = StatisticResourceUtils.create("max.latency", "maximum latency", GAUGE, SECONDS, ValueType.INTEGER);
    private final static Statistic AVG_LATENCY   = StatisticResourceUtils.create("avg.latency", "average latency", GAUGE, SECONDS, ValueType.INTEGER);
    private final static Statistic HITS   = StatisticResourceUtils.create("hits", "number of hits", GAUGE, StatisticUnit.NONE, ValueType.INTEGER);


    @Mapping (from=GlusterVolumeProfileDetails.class, to=GlusterVolumeProfileInfo.class)
    public static GlusterVolumeProfileInfo map(GlusterVolumeProfileDetails model, GlusterVolumeProfileInfo toEntity) {
        //GlusterVolumeProfileInfo is  read only from server and no support for setting these.
        //Hence mapping from REST model to Business entity not required.
        GlusterVolumeProfileInfo entity = (toEntity == null) ? new GlusterVolumeProfileInfo() : toEntity;
        return entity;
    }

    @Mapping (from=GlusterVolumeProfileInfo.class, to=GlusterVolumeProfileDetails.class)
    public static GlusterVolumeProfileDetails map(GlusterVolumeProfileInfo fromEntity, GlusterVolumeProfileDetails toModel) {
        GlusterVolumeProfileDetails model = new GlusterVolumeProfileDetails();
        BrickProfileDetails brickprofileDetails = new BrickProfileDetails();
        if (fromEntity.getBrickProfileDetails() != null) {
            for (org.ovirt.engine.core.common.businessentities.gluster.BrickProfileDetails brickDetailEntity:
                fromEntity.getBrickProfileDetails()) {
                BrickProfileDetail brickprofileDetail = new BrickProfileDetail();
                brickprofileDetail.setBrick(new GlusterBrick());
                brickprofileDetail.getBrick().setBrickDir(brickDetailEntity.getName());
                mapProfileDetails(brickDetailEntity.getProfileStats(), brickprofileDetail);
                brickprofileDetails.getBrickProfileDetail().add(brickprofileDetail);
            }
        }
        model.setBrickProfileDetails(brickprofileDetails);

        NfsProfileDetails nfsprofileDetails = new NfsProfileDetails();
        if (fromEntity.getNfsProfileDetails() != null) {
            for (org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeProfileStats nfsDetailEntity:
                fromEntity.getNfsProfileDetails()) {
                NfsProfileDetail nfsprofileDetail = new NfsProfileDetail();
                nfsprofileDetail.setNfsServerIp(nfsDetailEntity.getName());
                mapProfileDetails(nfsDetailEntity.getProfileStats(), nfsprofileDetail);
                nfsprofileDetails.getNfsProfileDetail().add(nfsprofileDetail);
            }
        }
        model.setNfsProfileDetails(nfsprofileDetails);
        return model;
    }

    private static void mapProfileDetails(List<StatsInfo> statsInfoList,
            EntityProfileDetail entityprofileDetail) {
        for (StatsInfo statsInfo: statsInfoList) {
            ProfileDetail profileDetail = new ProfileDetail();
            profileDetail.setProfileType(statsInfo.getProfileStatsType().name());
            profileDetail.setDuration(statsInfo.getDuration());
            profileDetail.getStatistic().add(StatisticResourceUtils.setDatum(clone(TOTAL_READ), statsInfo.getTotalRead()));
            profileDetail.getStatistic().add(StatisticResourceUtils.setDatum(clone(TOTAL_WRITE), statsInfo.getTotalWrite()));
            mapBlockStats(profileDetail, statsInfo);
            mapFopStats(profileDetail, statsInfo);
            entityprofileDetail.getProfileDetail().add(profileDetail);

        }
    }

    private static void mapFopStats(ProfileDetail profileDetail, StatsInfo statsInfo) {
        for (FopStats fopStat: statsInfo.getFopStats()) {
            FopStatistic fStat = new FopStatistic();
            fStat.setName(fopStat.getName());
            fStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(MIN_LATENCY), fopStat.getMinLatency()));
            fStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(MAX_LATENCY), fopStat.getMaxLatency()));
            fStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(AVG_LATENCY), fopStat.getAvgLatency()));
            fStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(HITS), fopStat.getHits()));
            profileDetail.getFopStatistic().add(fStat);
        }
    }

    private static void mapBlockStats(ProfileDetail profileDetail, StatsInfo statsInfo) {
        for (BlockStats blockStat: statsInfo.getBlockStats()) {
            BlockStatistic bStat = new BlockStatistic();
            bStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(BLOCK_SIZE), blockStat.getSize()));
            bStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(BYTES_BLOCK_READ), blockStat.getBlockRead()));
            bStat.getStatistic().add(StatisticResourceUtils.setDatum(clone(BYTES_BLOCK_WRITE), blockStat.getBlockWrite()));
            profileDetail.getBlockStatistic().add(bStat);
        }
    }

    public static Statistic clone(Statistic s) {
        return StatisticResourceUtils.create(s.getName(), s.getDescription(), s.getType(), s.getUnit(), s.getValues().getType());
    }

}
