package sandipchitale;

import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StartSpringIOModuleType extends ModuleType<StartSpringIOModuleBuilder> {

    private static final String ID = "START_SPRING_IO_MODULE_TYPE";

    private static final StartSpringIOModuleType INSTANCE = new StartSpringIOModuleType();

    StartSpringIOModuleType() {
        super(ID);
    }

    public static StartSpringIOModuleType getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public StartSpringIOModuleBuilder createModuleBuilder() {
        return new StartSpringIOModuleBuilder();
    }

    @NotNull
    @Override
    public String getName() {
        return "start.spring.io";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Use start.spring.io to create a new Spring Boot project";
    }

    @NotNull
    @Override
    public Icon getNodeIcon(@Deprecated boolean b) {
        return SpringInitializrIcons.ToolWindow;
    }
}
