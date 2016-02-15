package org.ovirt.engine.core.utils.serialization.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.StorageDomainDynamic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.VdsStatistics;
import org.ovirt.engine.core.common.businessentities.VdsTransparentHugePagesState;
import org.ovirt.engine.core.common.businessentities.vds_spm_id_map;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.RandomUtils;

/**
 * This test is designed to test that our business entities can be serialized/deserialized by Jackson correctly.
 */
@RunWith(Parameterized.class)
public class JsonObjectSerializationEntitiesTest {
    private final BusinessEntity<?> entity;

    public JsonObjectSerializationEntitiesTest(BusinessEntity<?> entity) {
        this.entity = entity;
    }

    @Parameters
    public static Collection<Object[]> data() {
        RandomUtils random = RandomUtils.instance();
        VdsStatic vdsStatic = new VdsStatic(random.nextString(10),
                                    random.nextString(10),
                                    random.nextString(10),
                                    random.nextInt(),
                                    random.nextInt(),
                                    random.nextString(10),
                                    Guid.newGuid(),
                                    Guid.newGuid(),
                                    random.nextString(10),
                                    random.nextBoolean(),
                                    random.nextEnum(VDSType.class),
                                    Guid.newGuid());
        vdsStatic.setPmOptions("option1=value1,option2=value2");
        Object[][] data =
                new Object[][] {
                        { vdsStatic },
                        { randomVdsDynamic() },
                        { randomVdsStatistics() },
                        { new vds_spm_id_map(Guid.newGuid(), Guid.newGuid(), random.nextInt()) },
                        { randomStorageDomainStatic() },
                        { new StorageDomainDynamic(random.nextInt(), Guid.newGuid(), random.nextInt()) },
                        { randomStoragePool() },
                        { new StoragePoolIsoMap(Guid.newGuid(),
                                Guid.newGuid(),
                                random.nextEnum(StorageDomainStatus.class)) },
                        { randomRole() },
                        { new IdContainerClass<vds_spm_id_map>(new vds_spm_id_map(Guid.newGuid(),
                                Guid.newGuid(),
                                random.nextInt())) },
                        { new IdContainerClass<Guid>(Guid.newGuid()) }
                };
        return Arrays.asList(data);
    }

    private static StoragePool randomStoragePool() {
        RandomUtils random = RandomUtils.instance();
        StoragePool sp = new StoragePool();
        sp.setdescription(random.nextString(10));
        sp.setComment(random.nextString(10));
        sp.setName(random.nextString(10));
        sp.setId(Guid.newGuid());
        sp.setIsLocal(random.nextBoolean());
        sp.setStatus(random.nextEnum(StoragePoolStatus.class));
        return sp;
    }

    private static StorageDomainStatic randomStorageDomainStatic() {
        RandomUtils random = RandomUtils.instance();
        StorageDomainStatic sds = new StorageDomainStatic();
        sds.setId(Guid.newGuid());
        sds.setStorage(random.nextString(10));
        sds.setStorageType(random.nextEnum(StorageType.class));
        sds.setStorageName(random.nextString(10));
        sds.setDescription(random.nextString(10));
        return sds;
    }

    private static VdsDynamic randomVdsDynamic() {
        RandomUtils random = RandomUtils.instance();
        VdsDynamic vdsDynamic = new VdsDynamic();
        vdsDynamic.setcpu_cores(random.nextInt());
        vdsDynamic.setCpuThreads(random.nextInt());
        vdsDynamic.setcpu_model(random.nextString(10));
        vdsDynamic.setcpu_speed_mh(random.nextDouble());
        vdsDynamic.setif_total_speed(random.nextString(10));
        vdsDynamic.setkvm_enabled(random.nextBoolean());
        vdsDynamic.setmem_commited(random.nextInt());
        vdsDynamic.setphysical_mem_mb(random.nextInt());
        vdsDynamic.setStatus(random.nextEnum(VDSStatus.class));
        vdsDynamic.setId(Guid.newGuid());
        vdsDynamic.setvm_active(random.nextInt());
        vdsDynamic.setvm_count(random.nextInt());
        vdsDynamic.setvm_migrating(random.nextInt());
        vdsDynamic.setreserved_mem(random.nextInt());
        vdsDynamic.setguest_overhead(random.nextInt());
        vdsDynamic.setprevious_status(random.nextEnum(VDSStatus.class));
        vdsDynamic.setsoftware_version(random.nextNumericString(5) + '.' + random.nextNumericString(5));
        vdsDynamic.setversion_name(random.nextString(10));
        vdsDynamic.setpending_vcpus_count(random.nextInt());
        vdsDynamic.setpending_vmem_size(random.nextInt());
        vdsDynamic.setnet_config_dirty(random.nextBoolean());
        vdsDynamic.setTransparentHugePagesState(random.nextEnum(VdsTransparentHugePagesState.class));
        vdsDynamic.setHardwareUUID(Guid.newGuid().toString());
        vdsDynamic.setHardwareFamily(random.nextString(10));
        vdsDynamic.setHardwareSerialNumber(random.nextString(10));
        vdsDynamic.setHardwareVersion(random.nextString(10));
        vdsDynamic.setHardwareProductName(random.nextString(10));
        vdsDynamic.setHardwareManufacturer(random.nextString(10));

        return vdsDynamic;
    }

    private static VdsStatistics randomVdsStatistics() {
        RandomUtils random = RandomUtils.instance();
        VdsStatistics vdsStatistics = new VdsStatistics();
        vdsStatistics.setcpu_idle(random.nextDouble());
        vdsStatistics.setcpu_load(random.nextDouble());
        vdsStatistics.setcpu_sys(random.nextDouble());
        vdsStatistics.setcpu_user(random.nextDouble());
        vdsStatistics.setmem_available(random.nextLong());
        vdsStatistics.setMemFree(random.nextLong());
        vdsStatistics.setmem_shared(random.nextLong());
        vdsStatistics.setusage_cpu_percent(random.nextInt());
        vdsStatistics.setusage_mem_percent(random.nextInt());
        vdsStatistics.setusage_network_percent(random.nextInt());
        vdsStatistics.setcpu_over_commit_time_stamp(new Date(random.nextLong()));
        return vdsStatistics;
    }

    private static Role randomRole() {
        RandomUtils random = RandomUtils.instance();
        Role role = new Role();
        role.setdescription(random.nextString(10));
        role.setId(Guid.newGuid());
        role.setname(random.nextString(10));
        return role;
    }

    @Test
    public void serializeAndDesrializeEntity() throws Exception {
        String serializedEntity = new JsonObjectSerializer().serialize(entity);
        assertNotNull(serializedEntity);
        Serializable deserializedEntity =
                new JsonObjectDeserializer().deserialize(serializedEntity, entity.getClass());
        assertNotNull(deserializedEntity);
        assertEquals(entity, deserializedEntity);
    }

    /**
     * This class is used to test that a container class with a field with no concrete type information gets serialized
     * an deserializde normally.
     *
     * @param <ID>
     *            The type of the id.
     */
    @SuppressWarnings("serial")
    public static class IdContainerClass<ID extends Serializable> implements BusinessEntity<ID> {
        ID id;

        @SuppressWarnings("unused")
        private IdContainerClass() {
        }

        public IdContainerClass(ID id) {
            this.id = id;
        }

        /**
         * @return the id
         */
        @Override
        public ID getId() {
            return id;
        }

        /**
         * @param id
         *            the id to set
         */
        @Override
        public void setId(ID id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @SuppressWarnings("rawtypes")
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
            IdContainerClass other = (IdContainerClass) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            return true;
        }
    }
}
