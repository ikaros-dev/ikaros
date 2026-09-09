package run.ikaros.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * 将 Maven POM 中的模块依赖方向作为可执行的边界契约。
 */
class ModuleDependencyBoundaryTest {
    private static final Set<String> LEGACY_IMPLEMENTATION_EDGES = Set.of(
        "integration->common",
        "operations->common",
        "storage->common",
        "storage->integration"
    );

    @Test
    void keepsApiAndCompositionRootDependencyDirections() throws Exception {
        Path root = Path.of(System.getProperty("maven.multiModuleProjectDirectory", "."));
        Set<String> modules = readModules(root.resolve("pom.xml"));
        Map<String, Set<String>> dependencies = new java.util.HashMap<>();
        for (String module : modules) {
            dependencies.put(module, readInternalDependencies(root.resolve(module).resolve("pom.xml")));
        }

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                assertDependencyAllowed(source, target, modules);
            }
        }
    }

    @Test
    void rejectsNewImplementationToImplementationDependency() {
        assertThatThrownBy(() -> assertDependencyAllowed("resource", "storage", Set.of("resource", "storage")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("resource -> storage");
    }

    private static void assertDependencyAllowed(String source, String target, Set<String> modules) {
        assertThat(target).isIn(modules);
        assertThat(source).isNotEqualTo(target);
        assertThat(target).isNotEqualTo("application");

        if (source.endsWith("-api")) {
            assertThat(target).as("public API %s must not depend on implementation %s", source, target)
                .endsWith("-api");
        } else if (!source.equals("application") && !target.endsWith("-api")) {
            assertThat(LEGACY_IMPLEMENTATION_EDGES)
                .as("new implementation dependency %s -> %s requires an explicit migration decision", source, target)
                .contains(source + "->" + target);
        }
    }

    private static Set<String> readModules(Path rootPom) throws Exception {
        Element project = parse(rootPom);
        Set<String> modules = new HashSet<>();
        for (Element module : children(project, "modules", "module")) {
            modules.add(module.getTextContent().trim());
        }
        return modules;
    }

    private static Set<String> readInternalDependencies(Path pom) throws Exception {
        Element project = parse(pom);
        Set<String> dependencies = new HashSet<>();
        for (Element dependency : children(project, "dependencies", "dependency")) {
            String groupId = childText(dependency, "groupId");
            if ("run.ikaros".equals(groupId)) {
                dependencies.add(childText(dependency, "artifactId"));
            }
        }
        return dependencies;
    }

    private static Element parse(Path path) throws Exception {
        assertThat(Files.exists(path)).as("missing Maven module POM: %s", path).isTrue();
        var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(path.toFile()).getDocumentElement();
    }

    private static Set<Element> children(Element parent, String containerName, String childName) {
        Set<Element> result = new HashSet<>();
        for (Element container : elements(parent, containerName)) {
            result.addAll(elements(container, childName));
        }
        return result;
    }

    private static Set<Element> elements(Element parent, String name) {
        Set<Element> result = new HashSet<>();
        var nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && name.equals(element.getLocalName() != null
                ? element.getLocalName() : element.getNodeName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static String childText(Element parent, String name) {
        return elements(parent, name).stream().findFirst().map(Element::getTextContent).orElse("").trim();
    }
}
