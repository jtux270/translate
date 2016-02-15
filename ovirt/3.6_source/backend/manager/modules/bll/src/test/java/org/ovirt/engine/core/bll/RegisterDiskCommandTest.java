package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

public class RegisterDiskCommandTest {

    @Test
    public void aliasGeneration() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1981);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 34);
        calendar.set(Calendar.SECOND, 56);

        assertEquals("Wrong generated alias",
                "RegisteredDisk_1981-11-16_12-34-56",
                RegisterDiskCommand.generateDefaultAliasForRegiteredDisk(calendar));
    }
}
