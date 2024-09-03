package sandipchitale;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Key;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StartSpringIOModuleBuilder extends ModuleBuilder {
    static final Key<String> START_SPRING_IO_DOWNLOADED_ZIP_LOCATION = Key.create("start.spring.io.downloaded.zip.path");

    private @NotNull ModifiableRootModel model;

    private WizardContext context;

    public StartSpringIOModuleBuilder() {
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel model) {
        this.model = model;
    }

    @Override
    public ModuleType<StartSpringIOModuleBuilder> getModuleType() {
        return StartSpringIOModuleType.getInstance();
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        this.context = context;
        return new StartSpringIOModuleWizardStep(this, context, parentDisposable);
    }

    @Override
    public @Nullable Project createProject(String name, String path) {
        try {
            String downloadedZipPath = context.getUserData(START_SPRING_IO_DOWNLOADED_ZIP_LOCATION);
            extractZip(downloadedZipPath, Path.of(path).getParent().toString());
            return ProjectManager.getInstance().loadAndOpenProject(path);
        } catch (IOException | ArchiveException | JDOMException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void extractZip(String zipFilePath, String extractDirectory) throws IOException, ArchiveException {
        InputStream inputStream;
        Path filePath = Paths.get(zipFilePath);
        inputStream = Files.newInputStream(filePath);
        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
        ArchiveInputStream archiveInputStream = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream);
        ArchiveEntry archiveEntry;
        while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
            Path path = Paths.get(extractDirectory, archiveEntry.getName());
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