package com.oscaruiz.mycqrs.demo.integration.support;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public final class SharedTestContainers {

    public static final MongoDBContainer MONGO =
            new MongoDBContainer(DockerImageName.parse("mongo:7"));

    static {
        MONGO.start();
    }

    private SharedTestContainers() {}
}
