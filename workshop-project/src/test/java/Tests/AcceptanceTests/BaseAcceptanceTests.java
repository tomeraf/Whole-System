package Tests.AcceptanceTests;

import static org.mockito.Mockito.mock;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Base64;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IAuthentication;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IExternalSystems;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.Repositories.INotificationRepository;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;
import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import com.halilovindustries.backend.Infrastructure.DBOrderRepository;
import com.halilovindustries.backend.Infrastructure.DBUserRepository;
import com.halilovindustries.backend.Infrastructure.DbNotificationRepository;
import com.halilovindustries.backend.Infrastructure.DbShopRepository;
import com.halilovindustries.backend.Infrastructure.JpaNotificationRepository;
import com.halilovindustries.backend.Infrastructure.JpaOrderRepository;
import com.halilovindustries.backend.Infrastructure.JpaShopRepository;
import com.halilovindustries.backend.Infrastructure.JpaUserAdapter;
import com.halilovindustries.backend.Infrastructure.MemoryNotificationRepository;
import com.halilovindustries.backend.Infrastructure.MemoryOrderRepository;
import com.halilovindustries.backend.Infrastructure.MemoryShopRepository;
import com.halilovindustries.backend.Infrastructure.MemoryUserRepository;
import com.halilovindustries.backend.Service.NotificationHandler;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.halilovindustries.websocket.INotifier;
import com.halilovindustries.websocket.VaadinNotifier;

@SpringBootTest(classes = com.halilovindustries.Application.class)
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
@Rollback
public abstract class BaseAcceptanceTests {
    @Autowired
    protected IShopRepository shopRepository;
    
    @Autowired
    protected IUserRepository userRepository;
    
    @Autowired
    protected IOrderRepository orderRepository;
    
    @Autowired
    protected INotificationRepository notificationRepository;
    
    @Autowired
    protected UserService userService;
    
    @Autowired
    protected ShopService shopService;
    
    @Autowired
    protected OrderService orderService;


    protected IAuthentication jwtAdapter;
    @MockitoBean protected IShipment shipment;
    @MockitoBean protected IPayment payment;
    @MockitoBean protected IExternalSystems externalSystems;
    protected INotifier notifier;

    
    protected ConcurrencyHandler concurrencyHandler;
    protected NotificationHandler notificationHandler;
    protected AcceptanceTestFixtures fixtures;

    @Autowired
    private JpaShopRepository JpaShopRepository;
    @Autowired
    private JpaUserAdapter JpaUserRepository;
    @Autowired
    private JpaOrderRepository JpaOrderRepository;
    @Autowired
    private JpaNotificationRepository JpaNotificationRepository;


    static {
        // 1) Reconfigure JUL so INFO logs don’t print timestamps
        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        LogManager.getLogManager().reset();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });
        root.addHandler(handler);

        // 2) Wrap System.err in a PrintStream that drops Byte-Buddy warnings
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new FilterOutputStream(originalErr) {
            private final String[] blacklist = new String[] {
              "WARNING: A Java agent has been loaded dynamically",
              "WARNING: Dynamic loading of agents will be disallowed"
            };

            @Override
            public void write(byte[] b, int off, int len) throws IOException, IndexOutOfBoundsException   {
                String s = new String(b, off, len);
                for (String block : blacklist) {
                    if (s.startsWith(block) || s.contains(block)) {
                        return; // swallow
                    }
                }
                super.write(b, off, len);
            }
        }));
    }

    @BeforeEach
    public void setUp() {
        // Only mock what can't be autowired
        // externalSystems = mock(IExternalSystems.class);
        // payment = mock(IPayment.class); 
        // shipment = mock(IShipment.class);
        // notifier = mock(INotifier.class);
        
        // Initialize the JWT adapter with a secret key
        jwtAdapter = new JWTAdapter(); 
        ((JWTAdapter)jwtAdapter).setSecret("TDNkeEc4qPSBelk6gSaCcc629o5XdyrX0ZmmWh/3LoQ=");
        
        // Initialize other handlers
        concurrencyHandler = new ConcurrencyHandler();
        notificationHandler = new NotificationHandler(notificationRepository, notifier);
        
        // Initialize fixtures
        fixtures = new AcceptanceTestFixtures(userService, shopService, orderService, 
                                            payment, shipment, externalSystems);
    }
}
