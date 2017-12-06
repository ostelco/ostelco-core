package hello;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class HelloApplication extends Application<HelloConfiguration> {

    public static void main(final String[] args) throws Exception {
        new HelloApplication().run(args);
    }

    @Override
    public void run(final HelloConfiguration configuration, final Environment environment) {
	String template = "Hello, %s!";
	environment.jersey().register(new hello.resources.HelloResource(template));
	environment.healthChecks().register("template", new hello.health.TemplateHealthCheck(template));
    }

    @Override
    public String getName() {
        return "Hello";
    }

    @Override
    public void initialize(final Bootstrap<HelloConfiguration> bootstrap) {
        // TODO: application initialization
    }
}
