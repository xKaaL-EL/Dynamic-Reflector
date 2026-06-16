package com.dynamicreflector.refactor;

import com.dynamicreflector.spoon.MethodInfo;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Locale;
import java.util.Set;

public final class StatelessMethodSafetyAnalyzer {
    private static final Set<String> PRIMITIVES = Set.of(
            "void", "boolean", "byte", "char", "short", "int", "long", "float", "double"
    );
    private static final Set<String> ANDROID_SIMPLE_TYPES = Set.of(
            "Context", "Intent", "View", "Activity", "Service", "BroadcastReceiver",
            "Fragment", "Application", "Worker", "PendingIntent", "Notification"
    );

    public String unsafeReason(CtMethod<?> method, MethodInfo methodInfo) {
        if (!methodInfo.isPublicMethod() || methodInfo.isStaticMethod()) {
            return "is not a public instance method";
        }
        if (methodInfo.isAbstractMethod() || methodInfo.isNativeMethod()) {
            return "is abstract or native";
        }
        if (methodInfo.getTypeParameterCount() > 0) {
            return "uses generic method type parameters";
        }
        if (method.hasModifier(ModifierKind.SYNCHRONIZED)) {
            return "uses synchronized method state";
        }
        if (method.getBody() == null) {
            return "has no copyable method body";
        }
        String signatureTypeReason = unsafeSignatureTypeReason(methodInfo);
        if (signatureTypeReason != null) {
            return signatureTypeReason;
        }
        if (!method.getElements(new TypeFilter<>(CtSynchronized.class)).isEmpty()) {
            return "uses synchronized blocks";
        }
        if (!method.getElements(new TypeFilter<>(CtFieldAccess.class)).isEmpty()) {
            return "uses instance fields/state";
        }
        if (!method.getElements(new TypeFilter<>(CtThisAccess.class)).isEmpty()) {
            return "uses this";
        }
        if (!method.getElements(new TypeFilter<>(CtSuperAccess.class)).isEmpty()) {
            return "uses super";
        }
        if (!method.getElements(new TypeFilter<>(CtConstructorCall.class)).isEmpty()) {
            return "uses constructors/state initialization";
        }
        if (!method.getElements(new TypeFilter<>(CtNewClass.class)).isEmpty()) {
            return "uses anonymous classes";
        }
        if (!method.getElements(new TypeFilter<>(CtInvocation.class)).isEmpty()) {
            return "depends on another method or package-private member";
        }
        if (!method.getBody().getElements(new TypeFilter<>(CtType.class)).isEmpty()) {
            return "uses local or anonymous classes";
        }
        for (CtTypeReference<?> typeReference : method.getElements(new TypeFilter<>(CtTypeReference.class))) {
            String reason = unsafeTypeReferenceReason(typeReference);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    private String unsafeSignatureTypeReason(MethodInfo methodInfo) {
        String returnReason = unsafeTypeNameReason(methodInfo.getReturnType());
        if (returnReason != null) {
            return returnReason;
        }
        for (String parameterType : methodInfo.getParameterTypes()) {
            String reason = unsafeTypeNameReason(parameterType);
            if (reason != null) {
                return reason;
            }
        }
        for (String thrownType : methodInfo.getThrownTypes()) {
            String reason = unsafeTypeNameReason(thrownType);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    private String unsafeTypeReferenceReason(CtTypeReference<?> reference) {
        String qualifiedName = reference == null ? null : reference.getQualifiedName();
        return unsafeTypeNameReason(qualifiedName);
    }

    private String unsafeTypeNameReason(String typeName) {
        if (typeName == null || typeName.isBlank() || typeName.equals("unknown")) {
            return "uses unresolved types";
        }
        String normalized = stripArray(typeName);
        String simpleName = simpleName(normalized);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("android.") || ANDROID_SIMPLE_TYPES.contains(simpleName)) {
            return "uses Android framework/runtime types";
        }
        if (normalized.contains("<") || normalized.contains(">") || normalized.contains("?") || normalized.contains("$")) {
            return "uses unresolved or complex types";
        }
        if (PRIMITIVES.contains(normalized) || normalized.startsWith("java.lang.")) {
            return null;
        }
        if (!normalized.contains(".")) {
            return "uses unresolved types";
        }
        return "uses non-primitive type dependency: " + normalized;
    }

    private String stripArray(String typeName) {
        String value = typeName;
        while (value.endsWith("[]")) {
            value = value.substring(0, value.length() - 2);
        }
        return value;
    }

    private String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }
}
