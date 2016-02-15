package org.ovirt.engine.core.utils.archivers.tar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class TarInMemoryExport implements AutoCloseable {
    protected final static Log log = LogFactory.getLog(TarInMemoryExport.class);

    private TarArchiveInputStream tarInputStream;

    public TarInMemoryExport(InputStream inputStream) {
        tarInputStream = new TarArchiveInputStream(inputStream);

    }

    /**
     * Extracting tarInputStream files and returns map with the file name as the key and the file content as the value
     * as string. This function does not support tar with directories.
     *
     * @return Map with the file names as the keys, and the file content as string value.
     * @throws IOException
     */
    public Map<String, ByteBuffer> unTar() throws IOException {
        Map<String, ByteBuffer> fileContent = new HashMap<>();
        TarArchiveEntry tarEntry = null;
        tarEntry = tarInputStream.getNextTarEntry();
        while (tarEntry != null) {
            // Get Size of the file and create a byte array for the size.
            byte[] content = new byte[(int) tarEntry.getSize()];

            // Read file from the archive into byte array.
            if (tarInputStream.read(content) == -1) {
                log.errorFormat("File {0} could not be read ", tarEntry.getFile());
                continue;
            }
            fileContent.put(tarEntry.getName(), ByteBuffer.wrap(content));
            tarEntry = tarInputStream.getNextTarEntry();
        }
        return fileContent;
    }

    @Override
    public void close() throws IOException {
        tarInputStream.close();
    }
}
