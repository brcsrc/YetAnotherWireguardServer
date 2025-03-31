package com.brcsrc.yaws.security;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtSecretKeyGeneratorTests {
    private static final Logger logger = LoggerFactory.getLogger(JwtSecretKeyGeneratorTests.class);

    /**
     * this test does is not necessarily for code coverage since the code under test is all
     * from jwt libraries that we are not responsible for. It is useful for generating a new
     * server side secret key to be used for development. you can run this in IDE or in test
     * container with:
     *      ./scripts/test-runner run-tests --test-name "*generateSecretKeyGeneratesSecretKey*"
     */
    @Test
    public void generateSecretKeyGeneratesSecretKey() {
        String secret = JwtSecretKeyGenerator.generateSecretKey();
        assert secret != null;
        logger.info(String.format("\n GENERATED NEW SECRET KEY: \n%s\n", secret));
    }
}
