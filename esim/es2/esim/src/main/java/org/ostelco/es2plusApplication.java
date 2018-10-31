package org.ostelco;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class es2plusApplication extends Application<es2plusConfiguration> {

    public static void main(final String[] args) throws Exception {
        new es2plusApplication().run(args);
    }

    @Override
    public String getName() {
        return "es2plus";
    }

    @Override
    public void initialize(final Bootstrap<es2plusConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final es2plusConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
