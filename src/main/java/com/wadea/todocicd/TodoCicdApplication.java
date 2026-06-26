package com.wadea.todocicd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TodoCicdApplication - the single entry point that starts the entire app.
 *
 * WHY this class needs to live at the ROOT of our package tree
 * (com.wadea.todocicd, directly above controller/service/repository/etc.):
 * @SpringBootApplication implicitly enables @ComponentScan, which by default
 * scans the package this class lives in AND every sub-package beneath it.
 * If this class lived inside, say, com.wadea.todocicd.controller instead,
 * Spring would never discover our service/repository/exception classes
 * automatically.
 */
@SpringBootApplication
// @SpringBootApplication is a convenience annotation that bundles together
// three separate annotations into one:
//   1. @Configuration       - this class can define Spring beans itself.
//   2. @EnableAutoConfiguration - Spring Boot inspects the dependencies on
//      our classpath (e.g. it sees spring-boot-starter-web is present) and
//      automatically configures an embedded Tomcat server, Jackson JSON
//      conversion, a DataSource for H2, etc. - all without us writing any
//      manual configuration classes ourselves.
//   3. @ComponentScan       - automatically finds and registers every
//      @Component/@Service/@Repository/@RestController in this package and
//      below as Spring-managed beans.
public class TodoCicdApplication {

    /**
     * Standard Java application entry point.
     *
     * @param args command-line arguments (unused here, but Spring Boot can
     *             accept things like --server.port=9090 through them to
     *             override application.properties values at launch time).
     */
    public static void main(String[] args) {
        // SpringApplication.run(...) bootstraps the entire Spring container:
        // it creates the ApplicationContext, triggers component scanning and
        // auto-configuration, starts the embedded Tomcat server on the
        // configured port (8080, per application.properties), and blocks,
        // keeping the JVM alive to serve incoming HTTP requests.
        SpringApplication.run(TodoCicdApplication.class, args);
    }
}
