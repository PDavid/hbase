package org.apache.hadoop.hbase.rest.openapi;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import org.apache.hbase.thirdparty.javax.ws.rs.ApplicationPath;
import org.apache.hbase.thirdparty.javax.ws.rs.Path;
import org.apache.yetus.audience.InterfaceAudience;
import java.util.HashSet;
import java.util.Set;

@InterfaceAudience.Private
public class HBaseRestAnnotationScanner extends JaxrsAnnotationScanner {

  @Override public Set<Class<?>> classes() {

    if (openApiConfiguration == null) {
      openApiConfiguration = new SwaggerConfiguration();
    }

    ClassGraph graph = new ClassGraph().enableAllInfo();
    Set<String> acceptablePackages = new HashSet<>();
    Set<Class<?>> output = new HashSet<>();

    // if classes are passed, use them
    if (openApiConfiguration.getResourceClasses() != null && !openApiConfiguration.getResourceClasses().isEmpty()) {
      for (String className : openApiConfiguration.getResourceClasses()) {
        if (!isIgnored(className)) {
          try {
            output.add(Class.forName(className));
          } catch (ClassNotFoundException e) {
            LOGGER.warn("error loading class from resourceClasses: " + e.getMessage(), e);
          }
        }
      }
      return output;
    }

    boolean allowAllPackages = false;
    if (openApiConfiguration.getResourcePackages() != null && !openApiConfiguration.getResourcePackages().isEmpty()) {
      for (String pkg : openApiConfiguration.getResourcePackages()) {
        if (!isIgnored(pkg)) {
          acceptablePackages.add(pkg);
          graph.whitelistPackages(pkg);
        }
      }
    } else {
      if (!onlyConsiderResourcePackages) {
        allowAllPackages = true;
      }
    }
    final Set<Class<?>> classes;
    try (ScanResult scanResult = graph.scan()) {
      classes = new HashSet<>(scanResult.getClassesWithAnnotation(Path.class.getName()).loadClasses());
      classes.addAll(new HashSet<>(scanResult.getClassesWithAnnotation(OpenAPIDefinition.class.getName()).loadClasses()));
      if (Boolean.TRUE.equals(openApiConfiguration.isAlwaysResolveAppPath())) {
        classes.addAll(new HashSet<>(scanResult.getClassesWithAnnotation(ApplicationPath.class.getName()).loadClasses()));
      }
    }

    for (Class<?> cls : classes) {
      if (allowAllPackages) {
        output.add(cls);
      } else {
        for (String pkg : acceptablePackages) {
          if (cls.getPackage().getName().startsWith(pkg)) {
            output.add(cls);
          }
        }
      }
    }
    LOGGER.trace("classes() - output size {}", output.size());
    return output;
  }
}
