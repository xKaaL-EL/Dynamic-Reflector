package com.dynamicreflector.cli;

import com.dynamicreflector.generate.FrameworkGenerator;
import com.dynamicreflector.generate.GeneratedFile;
import com.dynamicreflector.project.AndroidProject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "apply-framework", description = "Generate the minimal runtime framework once.")
public final class ApplyFrameworkCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--minimal", description = "Generate the locked minimal MVP framework.")
    private boolean minimal;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Override
    public Integer call() {
        try {
            if (!minimal) {
                System.out.println("MVP only supports minimal framework generation; continuing with --minimal behavior.");
            }
            AndroidProject project = locateProject(projectPath, basePackage);
            List<GeneratedFile> files = new FrameworkGenerator().generateMinimalFramework(project);
            printProject(project);
            System.out.println();
            System.out.println("Framework files:");
            for (GeneratedFile file : files) {
                System.out.println("  - " + (file.isCreated() ? "created " : "exists  ") + file.getPath());
            }
            System.out.println();
            System.out.println("Gradle files were not modified. If you add premium-plugin as a module, wire it manually.");
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }
}
