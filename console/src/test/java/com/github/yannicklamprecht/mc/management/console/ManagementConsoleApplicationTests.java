package com.github.yannicklamprecht.mc.management.console;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // "skyblock" is the server id configured in src/main/resources/application.yml; its
        // secret defaults to blank (fine for wiring up the beans) but auto-connect defaults to
        // true, so without this override the context-load test would try a real network
        // connection to whatever ws://localhost:25586 (or SKYBLOCK_MANAGEMENT_URL) resolves to.
        "minecraft.management.servers.skyblock.auto-connect=false"
})
class ManagementConsoleApplicationTests {

    @Test
    void contextLoads() {
    }
}
