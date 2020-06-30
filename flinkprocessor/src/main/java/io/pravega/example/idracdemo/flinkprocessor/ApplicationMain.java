package io.pravega.example.idracdemo.flinkprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for a flink job. It builds the AppConfiguration from command-line arguments,
 * and creates the AbstractJob subclass based on the jobClass argument.
 */
public class ApplicationMain {

    private static Logger log = LoggerFactory.getLogger( ApplicationMain.class );

    public static void main(String... args) throws Exception {
        AppConfiguration appConfiguration = new AppConfiguration(args);
        String jobClassName = appConfiguration.getJobClass();
        Class<?> jobClass = Class.forName(jobClassName);
        AbstractJob job = (AbstractJob) jobClass.getConstructor(AppConfiguration.class).newInstance(appConfiguration);
        job.run();
    }
}
