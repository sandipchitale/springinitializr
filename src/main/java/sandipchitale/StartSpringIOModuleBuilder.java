package sandipchitale;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.apache.commons.compress.archivers.ArchiveException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class StartSpringIOModuleBuilder extends ModuleBuilder {
    static final Key<String> START_SPRING_IO_DOWNLOADED_ZIP_LOCATION = Key.create("start.spring.io.downloaded.zip.path");

    private WizardContext context;
    private String projectName;

    public StartSpringIOModuleBuilder() {
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

    void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public @Nullable ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        if (projectName != null) {
            // Force set Project Name to match the one entered in the start.spring.io UI Artifact Name field.
            Objects.requireNonNull(settingsStep.getModuleNameLocationSettings()).setModuleName(projectName);
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public @Nullable Project createProject(String name, String path) {
        try {
            String downloadedZipPath = context.getUserData(START_SPRING_IO_DOWNLOADED_ZIP_LOCATION);
            Path pathPath = Path.of(path);
            Path pathPathParent = pathPath.getParent();
            String projectDirName = pathPath.getFileName().toString();
            ZipUtils.extractZip(downloadedZipPath, pathPathParent.toString(), projectDirName + "/");
            Project project = super.createProject(name, Path.of(path, name).toAbsolutePath().toString());
            return project;
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException(e);
        }
    }

}