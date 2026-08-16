package run.ikaros.server.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentDriverType;

/**
 * 仅扫描插件附件驱动扩展的类元数据，并校验其驱动类型声明.
 */
final class AttachmentDriverFetcherTypeValidator {
    /** 附件驱动类型方法名称. */
    private static final String DRIVER_TYPE_METHOD_NAME = "getDriverType";
    /** 附件驱动类型方法描述符. */
    private static final String DRIVER_TYPE_METHOD_DESCRIPTOR =
        Type.getMethodDescriptor(Type.getType(AttachmentDriverType.class));
    /** 附件驱动类型枚举内部名称. */
    private static final String DRIVER_TYPE_INTERNAL_NAME =
        Type.getInternalName(AttachmentDriverType.class);
    /** 附件驱动类型枚举描述符. */
    private static final String DRIVER_TYPE_DESCRIPTOR =
        Type.getDescriptor(AttachmentDriverType.class);

    /**
     * 校验指定插件声明的附件驱动类型.
     *
     * @param pluginWrapper 插件包装对象
     */
    void validate(PluginWrapper pluginWrapper) {
        Objects.requireNonNull(pluginWrapper, "'pluginWrapper' must not be null");
        ClassLoader pluginClassLoader = pluginWrapper.getPluginClassLoader();
        String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
        try {
            Class<?> pluginClass = Class.forName(pluginClassName, false, pluginClassLoader);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.setResourceLoader(new DefaultResourceLoader(pluginClassLoader));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
            var extensionClasses = scanner
                .findCandidateComponents(pluginClass.getPackageName())
                .stream()
                .map(beanDefinition -> loadClass(pluginClassLoader,
                    Objects.requireNonNull(beanDefinition.getBeanClassName())))
                .toList();
            validateExtensionClasses(pluginWrapper.getPluginId(), extensionClasses);
        } catch (ClassNotFoundException e) {
            throw new PluginValidationException(e,
                "插件 [%s] 主类加载失败，无法校验附件驱动类型", pluginWrapper.getPluginId());
        }
    }

    /**
     * 校验扩展类集合中的附件驱动类型声明.
     *
     * @param pluginId        插件标识
     * @param extensionClasses 扩展类集合
     */
    void validateExtensionClasses(String pluginId,
                                  Collection<? extends Class<?>> extensionClasses) {
        for (Class<?> extensionClass : extensionClasses) {
            if (!AttachmentDriverFetcher.class.isAssignableFrom(extensionClass)) {
                continue;
            }
            validateFetcherTypeMethod(pluginId, extensionClass);
        }
    }

    /**
     * 加载扩展类但不触发类初始化.
     *
     * @param classLoader 插件类加载器
     * @param className   扩展类名
     * @return 扩展类
     */
    private Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new PluginValidationException(e, "插件扩展类 [%s] 加载失败", className);
        }
    }

    /**
     * 校验附件驱动扩展的方法实现只能返回 CUSTOM 常量.
     *
     * @param pluginId      插件标识
     * @param extensionClass 附件驱动扩展类
     */
    private void validateFetcherTypeMethod(String pluginId, Class<?> extensionClass) {
        final Method method;
        try {
            method = extensionClass.getMethod(DRIVER_TYPE_METHOD_NAME);
        } catch (NoSuchMethodException e) {
            throw new PluginValidationException(e,
                "插件 [%s] 的附件驱动 [%s] 缺少驱动类型声明",
                pluginId, extensionClass.getName());
        }
        if (method.getDeclaringClass() == AttachmentDriverFetcher.class) {
            return;
        }
        if (!returnsCustomConstant(method)) {
            throw new PluginValidationException(
                "插件 [%s] 的附件驱动 [%s] 仅允许声明 CUSTOM 类型",
                pluginId, extensionClass.getName());
        }
    }

    /**
     * 判断方法字节码是否只返回 CUSTOM 枚举常量.
     *
     * @param method 待检查方法
     * @return 仅返回 CUSTOM 时为 true
     */
    private boolean returnsCustomConstant(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String resourceName = "/" + Type.getInternalName(declaringClass) + ".class";
        try (InputStream inputStream = declaringClass.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return false;
            }
            CustomReturnMethodVisitor visitor = new CustomReturnMethodVisitor();
            new ClassReader(inputStream).accept(visitor,
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.isValid();
        } catch (IOException e) {
            throw new PluginValidationException(e,
                "读取附件驱动类型方法 [%s#%s] 失败",
                declaringClass.getName(), method.getName());
        }
    }

    /**
     * 检查目标方法是否严格返回 CUSTOM 常量的字节码访问器.
     */
    private static final class CustomReturnMethodVisitor extends ClassVisitor {
        /** 是否找到目标方法. */
        private boolean found;
        /** 目标方法字节码是否合法. */
        private boolean valid;

        private CustomReturnMethodVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public @Nullable MethodVisitor visitMethod(int access, String name, String descriptor,
                                                   @Nullable String signature,
                                                   String @Nullable [] exceptions) {
            if (!DRIVER_TYPE_METHOD_NAME.equals(name)
                || !DRIVER_TYPE_METHOD_DESCRIPTOR.equals(descriptor)) {
                return null;
            }
            found = true;
            return new CustomConstantMethodVisitor();
        }

        private boolean isValid() {
            return found && valid;
        }

        /**
         * 记录目标方法中的有效指令并拒绝其他指令.
         */
        private final class CustomConstantMethodVisitor extends MethodVisitor {
            /** 当前有效指令数量. */
            private int instructionCount;
            /** 是否按顺序读取到 CUSTOM 常量. */
            private boolean customLoaded;
            /** 是否发现不允许的指令. */
            private boolean invalid;

            private CustomConstantMethodVisitor() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                instructionCount++;
                customLoaded = instructionCount == 1
                    && opcode == Opcodes.GETSTATIC
                    && DRIVER_TYPE_INTERNAL_NAME.equals(owner)
                    && AttachmentDriverType.CUSTOM.name().equals(name)
                    && DRIVER_TYPE_DESCRIPTOR.equals(descriptor);
                invalid |= !customLoaded;
            }

            @Override
            public void visitInsn(int opcode) {
                instructionCount++;
                boolean customReturn = instructionCount == 2
                    && customLoaded
                    && opcode == Opcodes.ARETURN;
                invalid |= !customReturn;
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                invalid = true;
            }

            @Override
            public void visitVarInsn(int opcode, int varIndex) {
                invalid = true;
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                invalid = true;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                                        String descriptor, boolean isInterface) {
                invalid = true;
            }

            @Override
            public void visitJumpInsn(int opcode, org.springframework.asm.Label label) {
                invalid = true;
            }

            @Override
            public void visitLdcInsn(Object value) {
                invalid = true;
            }

            @Override
            public void visitIincInsn(int varIndex, int increment) {
                invalid = true;
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                invalid = true;
            }

            @Override
            public void visitEnd() {
                valid = !invalid && instructionCount == 2;
            }
        }
    }
}
