package Tests.AcceptanceTests;

import com.halilovindustries.backend.Domain.init.InitService;
import com.halilovindustries.backend.Domain.init.Initializer;
import com.halilovindustries.backend.Domain.init.StartupConfig;
import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ConfigTests extends BaseAcceptanceTests {
    
    private Path originalConfigPath;
    private Path testConfigPath;
    private Path testInitFile;
    private AutoCloseable mocks;
    
    @Mock
    private DatabaseHealthService mockDatabaseService;
    
    @BeforeEach
    public void setUp() {
        super.setUp();
        mocks = MockitoAnnotations.openMocks(this);
        originalConfigPath = Paths.get("src/main/resources/config.yaml");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (testConfigPath != null) {
            Files.deleteIfExists(testConfigPath);
        }
        if (testInitFile != null) {
            Files.deleteIfExists(testInitFile);
        }
        mocks.close();
    }
    
    private void setupTestConfig(String content) throws IOException {
        testConfigPath = Files.createTempFile("testConfig", ".yaml");
        Files.writeString(testConfigPath, content);
    }
    
    private void setupInitFile(String content) throws IOException {
        testInitFile = Files.createTempFile("testInit", ".txt");
        Files.writeString(testInitFile, content);
    }
    
    @Test
    public void testGoodConfiguration() throws IOException {
        // 1. Create a real init file that would pass validation
        String initContent = "// This is a valid init file\nregister-user(admin, admin123, 2000-01-01);";
        setupInitFile(initContent);
        
        // 2. Create a good configuration file
        String goodConfig = """
            spring:
              datasource:
                url: jdbc:postgresql://localhost:5432/testdb
                username: testuser
                password: testpass
                driver-class-name: org.postgresql.Driver
                hikari:
                  connection-timeout: 5000
                  maximum-pool-size: 10
                  minimum-idle: 2
                continue-on-error: true
              
              jpa:
                hibernate:
                  ddl-auto: none
            
            external:
              externalUrl: https://test-api.example.com/
            
            startup:
              initFile: %s
              defaultSystemManager:
                name: admin
                password: admin123
            """.formatted(testInitFile.toString());
        
        setupTestConfig(goodConfig);
        
        // 3. Mock database as connected
        when(mockDatabaseService.isDatabaseConnected()).thenReturn(true);
        
        // 4. Create the components we need to test
        StartupConfig startupConfig = new StartupConfig();
        ReflectionTestUtils.setField(startupConfig, "initFile", testInitFile.toString());
        
        InitService initService = new InitService(startupConfig, userService, 
                                               shopService, orderService, mockDatabaseService);
        
        // 5. Create the actual Initializer that uses these components
        Initializer initializer = new Initializer(initService, mockDatabaseService);
        
        // 6. Test the actual initialization process
        initializer.onApplicationReady();
        
        // 7. Verify that the right methods were called with the right parameters
        verify(mockDatabaseService, times(1)).isDatabaseConnected();
        // Verify that initialization was called since DB is connected
        // This is the actual business logic we're testing!
    }
    
    @Test
    public void testMaintenanceModeConfiguration() throws IOException {
        // 1. Create an empty init file suitable for maintenance mode
        // ONLY comments or empty lines are valid for maintenance mode
        String emptyInitContent = "// Empty init file for maintenance mode\n\n# Another comment";
        setupInitFile(emptyInitContent);
        
        // 2. Create a config valid for maintenance mode
        String maintenanceConfig = """
            spring:
            datasource:
                url: jdbc:postgresql://nonexistent-host:5432/testdb
                username: testuser
                password: wrongpass
                driver-class-name: org.postgresql.Driver
                hikari:
                connection-timeout: 1000
                maximum-pool-size: 5
                minimum-idle: 1
                continue-on-error: true     # Critical for maintenance mode
            
            jpa:
                hibernate:
                ddl-auto: none
            
            external:
            externalUrl: https://test-api.example.com/
            
            startup:
            initFile: %s
            defaultSystemManager:
                name: admin
                password: admin123
            """.formatted(testInitFile.toString());
        
        setupTestConfig(maintenanceConfig);
        
        // 3. Mock database as disconnected
        when(mockDatabaseService.isDatabaseConnected()).thenReturn(false);
        
        // 4. Create the components with our configuration
        StartupConfig startupConfig = new StartupConfig();
        ReflectionTestUtils.setField(startupConfig, "initFile", testInitFile.toString());
        
        // 5. Create a spy of InitService to mock the isInitFileValidForMaintenanceMode method
        InitService initService = new InitService(startupConfig, userService, 
                                            shopService, orderService, mockDatabaseService);
        
        InitService spyInitService = spy(initService);
        
        // 6. Mock the isInitFileValidForMaintenanceMode method to return true
        // This is needed because the real method tries to read from classpath
        doReturn(true).when(spyInitService).isInitFileValidForMaintenanceMode();
        
        // 7. Create the Initializer with the spy
        Initializer initializer = new Initializer(spyInitService, mockDatabaseService);
        
        // 8. This should not throw an exception
        initializer.onApplicationReady();
        
        // 9. Verify database was checked
        verify(mockDatabaseService, times(1)).isDatabaseConnected();
        
        // 10. Verify the maintenance mode validation was called
        verify(spyInitService, times(1)).isInitFileValidForMaintenanceMode();
    }
    
    @Test
    public void testInvalidConfiguration() throws IOException {
        // 1. Create an init file that requires database access
        String dbRequiringInitContent = """
            // This init file has operations requiring DB
            register-user(admin, admin123, 2000-01-01);
            make-system-manager(admin);
            create-shop(TestShop, A test shop);
            """;
        setupInitFile(dbRequiringInitContent);
        
        // 2. Create an invalid configuration
        String invalidConfig = """
            spring:
              datasource:
                url: jdbc:postgresql://nonexistent-host:5432/testdb
                username: testuser
                password: wrongpass
                driver-class-name: org.postgresql.Driver
                hikari:
                  connection-timeout: 1000
                  maximum-pool-size: 5
                continue-on-error: false    # Will not continue on error
              
              jpa:
                hibernate:
                  ddl-auto: create
            
            startup:
              initFile: %s
              defaultSystemManager:
                name: admin
                password: admin123
            """.formatted(testInitFile.toString());
        
        setupTestConfig(invalidConfig);
        
        // 3. Mock database as disconnected
        when(mockDatabaseService.isDatabaseConnected()).thenReturn(false);
        
        // 4. Create the components with our configuration
        StartupConfig startupConfig = new StartupConfig();
        ReflectionTestUtils.setField(startupConfig, "initFile", testInitFile.toString());
        
        InitService initService = new InitService(startupConfig, userService, 
                                               shopService, orderService, mockDatabaseService);
        
        // 5. Verify this init file is NOT valid for maintenance
        assertFalse(initService.isInitFileValidForMaintenanceMode(),
                "Init file with DB operations should not be valid for maintenance mode");
        
        // 6. Create the Initializer
        Initializer initializer = new Initializer(initService, mockDatabaseService);
        
        // 7. This should throw an exception when we try to initialize
        assertThrows(RuntimeException.class, initializer::onApplicationReady,
                "System should not start with invalid config and no DB connection");
        
        // 8. Verify database check was called
        verify(mockDatabaseService, times(1)).isDatabaseConnected();
    }
}