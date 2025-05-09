package Tests.AcceptanceTests;

import static org.mockito.Mockito.mock;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.BeforeEach;

import Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import Domain.Adapters_and_Interfaces.IAuthentication;
import Domain.Adapters_and_Interfaces.IPayment;
import Domain.Adapters_and_Interfaces.IShipment;
import Domain.Adapters_and_Interfaces.JWTAdapter;
import Domain.Repositories.IOrderRepository;
import Domain.Repositories.IShopRepository;
import Domain.Repositories.IUserRepository;
import Infrastructure.MemoryOrderRepository;
import Infrastructure.MemoryShopRepository;
import Infrastructure.MemoryUserRepository;
import Service.OrderService;
import Service.ShopService;
import Service.UserService;

public abstract class BaseAcceptanceTests {
    protected IShopRepository shopRepository;
    protected IUserRepository userRepository;
    protected IOrderRepository orderRepository;
    protected IAuthentication jwtAdapter;
    protected IShipment shipment;
    protected IPayment payment;
    protected UserService userService;
    protected ShopService shopService;
    protected OrderService orderService;
    protected ConcurrencyHandler concurrencyHandler;
    protected AcceptanceTestFixtures fixtures;


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
        shopRepository   = new MemoryShopRepository();
        userRepository   = new MemoryUserRepository();
        orderRepository  = new MemoryOrderRepository();
        jwtAdapter       = new JWTAdapter();
        concurrencyHandler = new ConcurrencyHandler();
        shipment         = mock(IShipment.class);
        payment          = mock(IPayment.class);

        userService  = new UserService(userRepository, jwtAdapter, concurrencyHandler);
        shopService  = new ShopService(userRepository, shopRepository, orderRepository, jwtAdapter, concurrencyHandler);
        orderService = new OrderService(userRepository, shopRepository, orderRepository, jwtAdapter, payment, shipment, concurrencyHandler);
        fixtures = new AcceptanceTestFixtures(userService, shopService, orderService, payment, shipment);
    }
}
