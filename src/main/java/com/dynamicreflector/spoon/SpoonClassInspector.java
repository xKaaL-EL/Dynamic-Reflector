package com.dynamicreflector.spoon;

import spoon.reflect.code.CtBlock;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.CtExtendedModifier;
import spoon.reflect.declaration.ModifierKind;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SpoonClassInspector {
    public List<JavaClassInfo> listClasses(SpoonModelContext context) {
        return context.getModel()
                .getAllTypes()
                .stream()
                .filter(type -> type.getQualifiedName() != null && !type.getQualifiedName().isEmpty())
                .map(this::toInfo)
                .sorted(Comparator.comparing(JavaClassInfo::getQualifiedName))
                .collect(Collectors.toList());
    }

    public Optional<JavaClassInfo> findClass(SpoonModelContext context, String className) {
        String normalized = className.trim();
        return listClasses(context)
                .stream()
                .filter(info -> info.getQualifiedName().equals(normalized)
                        || info.getSimpleName().equals(normalized)
                        || info.getQualifiedName().endsWith("." + normalized))
                .findFirst();
    }

    private JavaClassInfo toInfo(CtType<?> type) {
        List<MethodInfo> methods = type.getMethods()
                .stream()
                .map(this::toMethodInfo)
                .sorted(Comparator.comparing(MethodInfo::getName))
                .collect(Collectors.toList());

        List<String> interfaces = type.getSuperInterfaces()
                .stream()
                .map(CtTypeReference::getQualifiedName)
                .sorted()
                .collect(Collectors.toList());

        int fieldCount = type.getFields().size();
        int staticFinalFieldCount = 0;
        for (CtField<?> field : type.getFields()) {
            if (field.hasModifier(ModifierKind.STATIC) && field.hasModifier(ModifierKind.FINAL)) {
                staticFinalFieldCount++;
            }
        }

        CtTypeReference<?> superClass = type.getSuperclass();
        return new JavaClassInfo(
                type.getQualifiedName(),
                type.getSimpleName(),
                sourcePath(type),
                superClass == null ? "" : superClass.getQualifiedName(),
                interfaces,
                methods,
                fieldCount,
                staticFinalFieldCount
        );
    }

    private MethodInfo toMethodInfo(CtMethod<?> method) {
        List<String> parameterTypes = new ArrayList<>();
        for (CtParameter<?> parameter : method.getParameters()) {
            CtTypeReference<?> type = parameter.getType();
            parameterTypes.add(type == null ? "unknown" : type.getQualifiedName());
        }
        CtTypeReference<?> returnType = method.getType();
        CtBlock<?> body = method.getBody();
        return new MethodInfo(
                method.getSimpleName(),
                returnType == null ? "void" : returnType.getQualifiedName(),
                parameterTypes,
                method.hasModifier(ModifierKind.STATIC),
                method.hasModifier(ModifierKind.PUBLIC),
                method.hasModifier(ModifierKind.PROTECTED),
                method.hasModifier(ModifierKind.PRIVATE),
                body != null
        );
    }

    private Path sourcePath(CtType<?> type) {
        SourcePosition position = type.getPosition();
        if (position == null || !position.isValidPosition()) {
            return null;
        }
        File file = position.getFile();
        return file == null ? null : file.toPath().toAbsolutePath().normalize();
    }
}
