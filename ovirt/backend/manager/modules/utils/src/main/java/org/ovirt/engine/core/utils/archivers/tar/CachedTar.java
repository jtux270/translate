package org.ovirt.engine.core.utils.archivers.tar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;

/**
 * Handles cache tar file based on directory.
 *
 * Cache tar based on directory structure. If files are changed
 * recreate tar file. Test file change once per interval.
 */
public class CachedTar {

    private static final Log log = LogFactory.getLog(CachedTar.class);

    private long refreshInterval = 10000;
    private long nextCheckTime = 0;

    private File archive;
    private File dir;

    private void create(long timestamp) throws IOException {
        File temp = null;
        OutputStream os = null;
        try {
            // must create within same directory
            // so rename be atomic
            temp = File.createTempFile(
                this.archive.getName(),
                "tmp",
                this.archive.getParentFile()
            );
            os = new FileOutputStream(temp);
            Tar.doTar(os, this.dir);
        }
        catch(IOException e) {
            String message = String.format("Cannot create tarball '%1$s'", this.archive);
            log.error(message, e);
            throw new IOException(message, e);
        }
        finally {
            try {
                if (os != null) {
                    os.close();
                }
            }
            catch(IOException e) {
                log.error(
                    String.format("Cannot close '%1$s'", temp),
                    e
                );
            }
        }

        try {
            if (!temp.setLastModified(timestamp)) {
                throw new IOException(
                    String.format(
                        "Cannot set last modified '%1$s' to '%2$d'",
                        temp.getCanonicalPath(),
                        timestamp
                    )
                );
            }

            if (!temp.renameTo(this.archive)) {
                throw new IOException(
                    String.format(
                        "Cannot rename '%1$s' to '%2$s'",
                        temp.getCanonicalPath(),
                        archive.getCanonicalPath()
                    )
                );
            }

            temp = null;
        }
        catch(IOException e) {
            log.error(e);
            throw e;
        }
        finally {
            if (temp != null && !temp.delete()) {
                log.error(String.format("Cannot delete '%1$s'", temp.getAbsolutePath()));
            }
        }
    }

    private void ensure() throws IOException {
        if (!this.archive.exists()) {
            log.info(
                String.format(
                    "Tarball '%1$s' is missing, creating",
                    this.archive.getAbsolutePath()
                )
            );
            this.nextCheckTime = System.currentTimeMillis() + this.refreshInterval;
            create(getTimestampRecursive(this.dir));
        }
        else if (this.nextCheckTime <= System.currentTimeMillis()) {
            this.nextCheckTime = System.currentTimeMillis() + this.refreshInterval;

            long treeTimestamp = getTimestampRecursive(this.dir);
            if (archive.lastModified() != treeTimestamp) {
                log.info(
                    String.format(
                        "Tarball '%1$s' is out of date, re-creating",
                        this.archive.getAbsolutePath()
                    )
                );
                create(treeTimestamp);
            }
        }
    }

    /**
     * Constructor.
     * @param archive name of tar to cache.
     * @param dir base directory.
     */
    public CachedTar(File archive, File dir) {
        this.archive = archive;
        this.dir = dir;

        this.refreshInterval = Config.<Integer>getValue(
            ConfigValues.BootstrapCacheRefreshInterval
        );
    }

    /**
     * Get file of archive, without enforcing cache.
     * This should be used only if file is not to be accessed (messages).
     * @return File name.
     */
    public File getFileNoUse() {
        return this.archive;
    }

    /**
     * Get file of archive.
     * @return File name.
     */
    public File getFile() throws IOException {
        ensure();
        return this.archive;
    }

    /**
     * Returns the maximum timestamp of directory tree.
     *
     * @param file
     *            directory/file name.
     * @return max timestamp.
     */
    private static long getTimestampRecursive(File file) {
        if (file.isDirectory()) {
            long m = 0;
            for (String name : file.list()) {
                m = Math.max(m, getTimestampRecursive(new File(file, name)));
            }
            return m;
        }
        else if (file.isFile()) {
            return file.lastModified();
        }
        else {
            return 0;
        }
    }
}
