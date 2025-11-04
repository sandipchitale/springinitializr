package sandipchitale;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ZipUtils {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void extractZip(String zipFilePath, String extractDirectory, String projectDirName) throws IOException, ArchiveException {
        InputStream inputStream;
        Path filePath = Paths.get(zipFilePath);
        inputStream = Files.newInputStream(filePath);
        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
        ArchiveInputStream<? extends ArchiveEntry> archiveInputStream = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream);
        ArchiveEntry archiveEntry;
        while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
            String name = archiveEntry.getName();
            if (!name.startsWith(projectDirName)) {
                // adjust name
                name = name.replaceFirst("^[^/]+/", projectDirName);
            }
            Path path = Paths.get(extractDirectory, name);
            File file = path.toFile();
            if (archiveEntry.isDirectory()) {
                if (!file.isDirectory()) {
                    file.mkdirs();
                }
            } else {
                File parent = file.getParentFile();
                if (!parent.isDirectory()) {
                    parent.mkdirs();
                }
                try (OutputStream outputStream = Files.newOutputStream(path)) {
                    IOUtils.copy(archiveInputStream, outputStream);
                }
            }
        }
    }
}
