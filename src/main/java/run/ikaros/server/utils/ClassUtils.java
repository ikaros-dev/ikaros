package run.ikaros.server.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class ClassUtils {

    public static List<Class<?>> findClassByDirectory(String packageName, String packagePath) {
        AssertUtils.notBlank(packageName, "'packageName' must not be blank");
        AssertUtils.notBlank(packagePath, "'packagePath' must not be blank");

        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>(0);
        }

        File[] dirs = dir.listFiles();
        List<Class<?>> classes = new ArrayList<Class<?>>();

        AssertUtils.notNull(dirs, "'dirs' must not be null");
        for (File file : dirs) {

            if (file.isDirectory()) {
                classes.addAll(findClassByDirectory(packageName + "." + file.getName(),
                    file.getAbsolutePath()));
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return classes;
    }

    public static List<Class<?>> findClassInJar(String packageName, URL url) throws IOException {
        AssertUtils.notBlank(packageName, "'packageName' must not be blank");
        AssertUtils.notNull(url, "'url' must not be null");

        List<Class<?>> classes = new ArrayList<Class<?>>();

        String packageDirName = packageName.replace('.', File.separatorChar);
        JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            String name = entry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }

            if (name.startsWith(packageDirName) && name.endsWith(".class")) {
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return classes;
    }

    public static List<Class<?>> findClassByPackage(String packageName)
        throws IOException {
        AssertUtils.notBlank(packageName, "'parentInterface' must not be blank");

        List<Class<?>> classes = new ArrayList<Class<?>>();

        String packageDirName = packageName.replace('.', File.separatorChar);

        Enumeration<URL> dirs = Thread.currentThread()
            .getContextClassLoader()
            .getResources(packageDirName);

        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                classes.addAll(findClassByDirectory(packageName, filePath));
            } else if ("jar".equals(protocol)) {
                classes.addAll(findClassInJar(packageName, url));
            }
        }

        return classes;
    }

}
