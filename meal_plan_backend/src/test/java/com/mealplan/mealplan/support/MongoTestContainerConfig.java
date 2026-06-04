package com.mealplan.mealplan.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합/E2E 테스트용 MongoDB Testcontainer 설정.
 *
 * <p>도커로 실제 mongod 컨테이너를 띄우고, {@link ServiceConnection} 이 spring.data.mongodb.uri 를
 * 컨테이너 주소로 자동 주입한다. 도커 데몬이 실행 중이어야 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestContainerConfig {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
    }
}
