package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.RandomUtils;
import org.ovirt.engine.core.utils.RandomUtilsSeedingRule;

@RunWith(MockitoJUnitRunner.class)
public class MetadataDiskDescriptionHandlerTest {

    private static int DISK_ALIAS_MAX_LENGTH = 194;

    private DiskImage disk;
    private MetadataDiskDescriptionHandler metadataDiskDescriptionHandler;

    @Mock
    private AuditLogDirector auditLogDirector;

    @Rule
    public RandomUtilsSeedingRule rusr = new RandomUtilsSeedingRule();

    @Before
    public void setUp() {
        disk = new DiskImage();
        metadataDiskDescriptionHandler = spy(MetadataDiskDescriptionHandler.getInstance());
        doReturn(auditLogDirector).when(metadataDiskDescriptionHandler).getAuditLogDirector();
        doNothing().when(auditLogDirector).log(any(AuditLogableBase.class), any(AuditLogType.class));
    }

    @Test
    public void testEncodingDecoding() throws Exception {
        disk.setDiskAlias("DiskAlias");
        disk.setDiskDescription("DiskDescription");
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry("DiskAlias"),
                generateDiskDescriptionJsonEntry("DiskDescription"));
        assertDiskDescriptionDecoding(disk);
    }

    @Test
    public void encodeWithNullDiskDescription() throws Exception {
        disk.setDiskAlias("DiskAlias");
        disk.setDiskDescription(null);
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry("DiskAlias"),
                generateDiskDescriptionJsonEntry(""));
    }

    @Test
    public void encodeDecodeWithEmptyDiskDescription() throws Exception {
        disk.setDiskAlias("DiskAlias");
        disk.setDiskDescription("");
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry("DiskAlias"),
                generateDiskDescriptionJsonEntry(""));
        assertDiskDescriptionDecoding(disk);
    }

    @Test
    public void encodeWhenAliasIsTruncated() throws IOException {
        String alias = generateRandomString(200);
        disk.setDiskAlias(alias); // Only 194 can be stored.
        disk.setDiskDescription("DiskDescription");
        // Only first 194 bytes were stored from the alias. The description was lost.
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(alias.substring(0, DISK_ALIAS_MAX_LENGTH)));
    }

    @Test
    public void encodeWhenDescriptionIsCompletelyTruncated() throws IOException {
        String alias = generateRandomString(DISK_ALIAS_MAX_LENGTH);
        disk.setDiskAlias(alias); // Exactly the limit.
        disk.setDiskDescription("DiskDescription");
        // Alias was completely stored. The description was lost.
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(alias));
    }

    @Test
    public void encodeWhenDescriptionIsTruncated() throws IOException {
        disk.setDiskAlias("DiskAlias"); // diskAlias.length == 9 characters.
        String diskDescription = generateRandomString(170);
        disk.setDiskDescription(diskDescription); // Only 164 can be stored.
        // Only first 164 bytes were stored from the description. The alias was completely stored.
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry("DiskAlias"),
                generateDiskDescriptionJsonEntry(diskDescription.substring(0, 164)));
    }

    @Test
    public void encodeDecodeWithNonAsciiDiskAlias() throws Exception {
        disk.setDiskAlias("áéíñ");
        disk.setDiskDescription("DiskDescription");
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(encodeString("áéíñ")),
                generateDiskDescriptionJsonEntry("DiskDescription"), generateEncodingJsonEntry(1));
        assertDiskDescriptionDecoding(disk);
    }

    @Test
    public void encodeDecodeWithNonAsciiDiskDescription() throws Exception {
        disk.setDiskAlias("DiskAlias");
        disk.setDiskDescription("áéíñ");
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry("DiskAlias"),
                generateDiskDescriptionJsonEntry(encodeString("áéíñ")), generateEncodingJsonEntry(2));
        assertDiskDescriptionDecoding(disk);
    }

    @Test
    public void encodeDecodeWithNonAsciiDiskAliasAndDescription() throws Exception {
        disk.setDiskAlias("áéíñáéíñ");
        disk.setDiskDescription("áéíñ");
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(encodeString("áéíñáéíñ")),
                generateDiskDescriptionJsonEntry(encodeString("áéíñ")), generateEncodingJsonEntry(3));
        assertDiskDescriptionDecoding(disk);
    }

    @Test
    public void encodeWhenNonAsciiAliasIsTruncated() throws IOException {
        String nonAsciiDiskAlias = "ááááááááááááááááááááááááááááááááááááááááááááááá"; // 47 characters
        // This string will be encoded to 188 bytes, but only 184 can be stored.
        disk.setDiskAlias(nonAsciiDiskAlias);
        disk.setDiskDescription("DiskDescription");
        // Only first 46 characters (which were decoded to 184 bytes) from the alias were stored.
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(encodeString(nonAsciiDiskAlias.substring(0, 46))),
                generateEncodingJsonEntry(1));
    }

    @Test
    public void encodeWhenNonAsciiDiskDescriptionIsTruncated() throws IOException {
        disk.setDiskAlias("áéíñáé"); // 6 characters -> 24 bytes
        String nonAsciiDiskDescription = "ñññññññññññññññññññññññññññññññññññññññññññññññññññññññ"; // 55 characters
        // This string will be encoded to 220 bytes, but only 139 can be stored.
        disk.setDiskDescription(nonAsciiDiskDescription);
        // Only first 34 characters (which were decoded to 136 bytes) from the description were stored.
        assertDiskDescriptionMap(disk, generateDiskAliasJsonEntry(encodeString("áéíñáé")),
                generateDiskDescriptionJsonEntry(encodeString(nonAsciiDiskDescription.substring(0, 34))),
                generateEncodingJsonEntry(3));
    }

    private static String generateDiskAliasJsonEntry(String diskAlias) {
        return MetadataDiskDescriptionHandler.getInstance().generateJsonField("DiskAlias", diskAlias);
    }

    private static String generateDiskDescriptionJsonEntry(String diskDescription) {
        return MetadataDiskDescriptionHandler.getInstance().generateJsonField("DiskDescription", diskDescription);
    }

    private static String generateEncodingJsonEntry(int encoding) {
        return MetadataDiskDescriptionHandler.getInstance().generateJsonField("Enc", String.valueOf(encoding));
    }

    private static String generateRandomString(int stringLength) {
        return RandomUtils.instance().nextPropertyString(stringLength);
    }

    private static String encodeString(String str) {
        return Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_16LE));
    }

    private void assertDiskDescriptionMap(Disk disk, String... jsonEntries)
            throws IOException {
        assertEquals(String.format("{%s}", StringUtils.join(jsonEntries, ",", 0, jsonEntries.length)),
                metadataDiskDescriptionHandler.generateJsonDiskDescription(disk));
    }

    private static void assertDiskDescriptionDecoding(Disk disk) throws Exception {
        Disk diskToEnrich = new DiskImage();
        MetadataDiskDescriptionHandler.getInstance().enrichDiskByJsonDescription(
                MetadataDiskDescriptionHandler.getInstance().generateJsonDiskDescription(disk), diskToEnrich);
        assertEquals(disk, diskToEnrich);
    }
}
