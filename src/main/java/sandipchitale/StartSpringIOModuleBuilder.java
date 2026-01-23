package sandipchitale;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.compress.archivers.ArchiveException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

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
            // Create the project at the selected path (do not append the name again).
            Project project = super.createProject(name, Path.of(path).toAbsolutePath().toString());

            if (project != null) {
                // Ensure VFS sees the extracted files and link/import as a Gradle project automatically.
                ApplicationManager.getApplication().invokeLater(() -> {
                    // Ensure a Java SDK is set on the project
                    if (ProjectRootManager.getInstance(project).getProjectSdk() == null) {
                        Sdk bestJavaSdk = null;
                        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                            if (sdk.getSdkType() == JavaSdk.getInstance()) {
                                if (bestJavaSdk == null) {
                                    bestJavaSdk = sdk;
                                } else {
                                    JavaSdkVersion current = JavaSdk.getInstance().getVersion(sdk);
                                    JavaSdkVersion best = JavaSdk.getInstance().getVersion(bestJavaSdk);
                                    if (current != null && best != null && current.compareTo(best) > 0) {
                                        bestJavaSdk = sdk;
                                    }
                                }
                            }
                        }
                        if (bestJavaSdk != null) {
                            Sdk finalBestJavaSdk = bestJavaSdk;
                            WriteAction.run(() -> ProjectRootManager.getInstance(project).setProjectSdk(finalBestJavaSdk));
                        }
                    }

                    String basePath = project.getBasePath();
                    if (basePath != null) {
                        VirtualFile baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
                        if (baseDir != null) {
                            baseDir.refresh(true, true);

                            boolean isGradle = baseDir.findChild("build.gradle") != null
                                    || baseDir.findChild("build.gradle.kts") != null;
                            boolean isMaven = baseDir.findChild("pom.xml") != null;

                            if (isGradle) {
                                // Link Gradle project settings
                                GradleProjectSettings gradleProjectSettings = new GradleProjectSettings();
                                gradleProjectSettings.setExternalProjectPath(basePath);
                                GradleSettings.getInstance(project).linkProject(gradleProjectSettings);
                            } else if (isMaven) {
                                // Import as Maven project
                                VirtualFile pom = baseDir.findChild("pom.xml");
                                if (pom != null) {
                                    MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
                                    mavenProjectsManager.addManagedFilesOrUnignore(java.util.List.of(pom));
                                    // Optionally trigger update
                                    mavenProjectsManager.forceUpdateProjects();
                                }
                            }
                        }
                    }
                }, ModalityState.NON_MODAL);
            }

            return project;
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException(e);
        }
    }

}