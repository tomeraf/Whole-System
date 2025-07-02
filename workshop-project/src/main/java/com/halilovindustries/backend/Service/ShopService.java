package com.halilovindustries.backend.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BasketDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.User.*;

import jakarta.transaction.Transactional;

import com.halilovindustries.backend.Domain.Message;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IAuthentication;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.MaintenanceModeException;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.UserDTO;
import com.halilovindustries.backend.Domain.DomainServices.InteractionService;

import com.halilovindustries.backend.Domain.DomainServices.ManagementService;
import com.halilovindustries.backend.Domain.DomainServices.ShoppingService;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;
import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ShopService extends DatabaseAwareService {

    private IUserRepository userRepository;
    private IShopRepository shopRepository;
    private IOrderRepository orderRepository;
    private ManagementService managementService;
    private ShoppingService shoppingService;
    private IAuthentication authenticationAdapter;
    private InteractionService interactionService = InteractionService.getInstance();
    private final NotificationHandler notificationHandler;

    private static final Logger logger = LoggerFactory.getLogger(ShopService.class);

    @Autowired
    public ShopService(IUserRepository userRepository, IShopRepository shopRepository, IOrderRepository orderRepository,
            IAuthentication authenticationAdapter, ConcurrencyHandler concurrencyHandler,NotificationHandler notificationHandler) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
        this.authenticationAdapter = authenticationAdapter;
        this.shoppingService = new ShoppingService();
        this.managementService = ManagementService.getInstance(concurrencyHandler);
        this.notificationHandler = notificationHandler;
    }

    // show and filter
    @Transactional
    public Response<List<ShopDTO>> showAllShops(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                return Response.error("User not logged in");
            }
            ArrayList<Shop> s = new ArrayList<Shop>(
                    shopRepository.getAllShops().values().stream().filter((shop) -> shop.isOpen()).toList());
            List<ShopDTO> shopDTOs = new ArrayList<>();
            for (Shop shop : s) {
                List<Item> items = shop.getItems();
                HashMap<Integer, ItemDTO> itemDTOs = new HashMap<>();
                for (Item item : items) {
                    ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                            item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                            item.getNumOfOrders());
                    itemDTOs.put(item.getId(), itemDTO);
                }
                ShopDTO shopDTO = new ShopDTO(shop.getId(), shop.getName(), shop.getDescription(), itemDTOs,
                        shop.getRating(), shop.getRatingCount());
                shopDTOs.add(shopDTO);
            }
            return Response.ok(shopDTOs);
        }
        
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error showing all shops: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<List<ShopDTO>> showUserShops(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                return Response.error("User not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
    
            List<ShopDTO> shopDTOs = new ArrayList<>();
            List<Shop> userShops = shopRepository.getUserShops(userID).stream()
                    .filter(Shop::isOpen)
                    .toList();
            for (Shop shop : userShops) {
                List<Item> items = shop.getItems();
                HashMap<Integer, ItemDTO> itemDTOs = new HashMap<>();
                for (Item item : items) {
                    ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                            item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                            item.getNumOfOrders());
                    itemDTOs.put(item.getId(), itemDTO);
                }
                ShopDTO shopDTO = new ShopDTO(shop.getId(), shop.getName(), shop.getDescription(), itemDTOs,
                        shop.getRating(), shop.getRatingCount());
                shopDTOs.add(shopDTO);
            }
            return Response.ok(shopDTOs);
        }
        
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error showing user shops: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<List<ItemDTO>> showShopItems(String sessionToken, int shopId) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            Shop shop = shopRepository.getShopById(shopId);
            List<Item> items = shop.getItems();
            List<ItemDTO> itemDTOs = new ArrayList<>();
            for (Item item : items) {
                ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                        item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                        item.getNumOfOrders());
                itemDTOs.add(itemDTO);
            }
            return Response.ok(itemDTOs);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error showing shop items: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<ItemDTO>> filterItemsAllShops(String sessionToken, HashMap<String, String> filters) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            String category = filters.get("category");
            String name = filters.get("name");
            double minPrice = filters.get("minPrice") != null ? Double.parseDouble(filters.get("minPrice")) : 0;
            double maxPrice = filters.get("maxPrice") != null ? Double.parseDouble(filters.get("maxPrice")) : 0;
            int minRating = filters.get("minRating") != null ? Integer.parseInt(filters.get("minRating")) : 0;
            double shopRating = filters.get("shopRating") != null ? Double.parseDouble(filters.get("shopRating")) : 0;
            List<Item> filteredItems = new ArrayList<>();
            List<ItemDTO> itemDTOs = new ArrayList<>();
            for (Shop shop : shopRepository.getAllShops().values()) {
                for (Item item : shop.filter(name, category, minPrice, maxPrice, minRating, shopRating)) {
                    ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                            item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                            item.getNumOfOrders());
                    itemDTOs.add(itemDTO);
                }
            }
            return Response.ok(itemDTOs);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error filtering items in all shops: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<ItemDTO>> filterItemsInShop(String sessionToken, int shopId, HashMap<String, String> filters) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            String category = filters.get("category");
            String name = filters.get("name");
            double minPrice = filters.get("minPrice") != null ? Double.parseDouble(filters.get("minPrice")) : 0;
            double maxPrice = filters.get("maxPrice") != null ? Double.parseDouble(filters.get("maxPrice")) : 0;
            int minRating = filters.get("minRating") != null ? Integer.parseInt(filters.get("minRating")) : 0;
            List<Item> filteredItems = new ArrayList<>();
            Shop shop = shopRepository.getShopById(shopId);
            filteredItems.addAll(shop.filter(name, category, minPrice, maxPrice, minRating, 0));
            List<ItemDTO> itemDTOs = new ArrayList<>();
            for (Item item : filteredItems) {
                ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                        item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                        item.getNumOfOrders());
                itemDTOs.add(itemDTO);
            }
            return Response.ok(itemDTOs);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error filtering items in shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<ShopDTO> getShopInfo(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest user = this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            HashMap<Integer, ItemDTO> itemDTOs = new HashMap<>();
            for (Item item : shop.getItems()) {
                ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                        item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                        item.getNumOfOrders());
                itemDTOs.put(item.getId(), itemDTO);
            }
            ShopDTO shopDto = new ShopDTO(shop.getId(), shop.getName(), shop.getDescription(), itemDTOs,
                    shop.getRating(), shop.getRatingCount());
            logger.info(() -> "Shop info retrieved: " + shopDto.getName() + " by user: "
                    + userRepository.getUserById(userID).getUsername());
            return Response.ok(shopDto);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving shop info: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<UserDTO>> getShopMembers(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<UserDTO> members = new ArrayList<>();
            List<Integer> ownerIDs = new ArrayList<>(shop.getOwnerIDs());
            ownerIDs.removeIf(id -> (((Registered)userRepository.getUserById(id)).getAppointer(shopID) != -1));
            int founderID = ownerIDs.get(0); //should be the only one
            members.add(new UserDTO(founderID, userRepository.getUserById(founderID).getUsername()));
            
            members.addAll(
                shop.getOwnerIDs().stream()
                    .map(userRepository::getUserById)
                    .filter(u -> (((Registered)u).getAppointer(shopID) != -1))
                    .map(u -> new UserDTO(u.getUserID(), u.getUsername()))
                    .toList()
            );

            members.addAll(
                shop.getManagerIDs().stream()
                    .map(userRepository::getUserById)
                    .map(u -> new UserDTO(u.getUserID(), u.getUsername()))
                    .toList()
            );

            return Response.ok(members);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving shop members: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    // shop management
    @Transactional
    public Response<ShopDTO> createShop(String sessionToken, String name, String description) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            if (shopRepository.getShopByName(name) != null) {
                return Response.error("Shop name already exists");
            }
            Shop shop = new Shop(shopRepository.getNextId(),user.getUserID(), name, description);
            shopRepository.addShop(shop);
            user.setRoleToShop(shop.getId(), new Founder(shop.getId()));
            List<Item> items = shop.getItems();
            HashMap<Integer, ItemDTO> itemDTOs = new HashMap<>();
            for (Item item : items) {
                ItemDTO itemDTO = new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(),
                        item.getId(), item.getQuantity(), item.getRating(), item.getDescription(),
                        item.getNumOfOrders());
                itemDTOs.put(item.getId(), itemDTO);
            }
            ShopDTO shopDto = new ShopDTO(shop.getId(), shop.getName(), shop.getDescription(), itemDTOs,
                    shop.getRating(), shop.getRatingCount());
            logger.info(() -> "Shop created: " + shopDto.getName() + " by user: " + userID);
            return Response.ok(shopDto);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error creating shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<Void> closeShop(String sessionToken, int shopID) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, close the shop with the provided details
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
                // now exclusive: no reads or other writes
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Registered founder= (Registered) this.userRepository.getUserById(shopRepository.getShopById(shopID).getFounderID());
            if(user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            List<Integer> membersIDs = shop.getMembersIDs();
            managementService.closeShop(user, shop,founder);
            notificationHandler.notifyUsers(membersIDs, "Shop " + shop.getName() + " is closed");
            logger.info(() -> "Shop closed: " + shop.getName() + " by user: " + userID);
            return Response.ok();
        }
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error closing shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<ItemDTO> addItemToShop(String sessionToken, int shopID, String itemName, Category category,
            double itemPrice, String description) {
        // need to add the Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, add the item to the shop with the provided details
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            Item newItem = managementService.addItemToShop(user, shop, itemName, category, itemPrice, description);
            ItemDTO itemDto = new ItemDTO(newItem.getName(), newItem.getCategory(), newItem.getPrice(),
                    shop.getId(), newItem.getId(), newItem.getQuantity(), newItem.getRating(),
                    newItem.getDescription(), newItem.getNumOfOrders());
            logger.info(
                    () -> "Item added to shop: " + itemName + " in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(itemDto);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error adding item to shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<Void> removeItemFromShop(String sessionToken, int shopID, int itemID) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, remove the item from the shop with the provided details
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
                if (!authenticationAdapter.validateToken(sessionToken)) {
                    throw new Exception("User is not logged in");
                }
                int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
                Registered user = (Registered) this.userRepository.getUserById(userID);
                if (user.isSuspended()) {
                    return Response.error("User is suspended");
                }
                Shop shop = shopRepository.getShopById(shopID);
                managementService.removeItemFromShop(user, shop, itemID);
                logger.info(() -> "Item removed from shop: " + itemID + " in shop: " + shop.getName() + " by user: "
                        + userID);
                return Response.ok();
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
                logger.error(() -> "Error removing item from shop: " + e.getMessage());
                return Response.error("Error: " + e.getMessage());
            }
    }

    @Transactional
    public Response<Void> changeItemName(String sessionToken, int shopID, int itemID, String newName) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, change the item name in the shop with the provided details
        try {
            checkDatabaseHealth("current method");
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.updateItemName(user, shop, itemID, newName);
            logger.info(() -> "Item name changed in shop: " + itemID + " in shop: " + shop.getName() + " by user: "
                    + userID);
            return Response.ok();
        }
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error changing item name in shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<Void> changeItemQuantityInShop(String sessionToken, int shopID, int itemID, int newQuantity) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, change the item quantity in the shop with the provided details
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.updateItemQuantity(user, shop, itemID, newQuantity);
            logger.info(() -> "Item quantity changed in shop: " + itemID + " in shop: " + shop.getName()
                    + " by user: " + userID);
            return Response.ok();
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error changing item quantity in shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<Void> changeItemPriceInShop(String sessionToken, int shopID, int itemID, double newPrice) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, change the item price in the shop with the provided details
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.updateItemPrice(user, shop, itemID, newPrice);
            logger.info(() -> "Item price changed in shop: " + itemID + " in shop: " + shop.getName() + " by user: "
                    + userID);
            return Response.ok();
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error changing item price in shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<Void> changeItemDescriptionInShop(String sessionToken, int shopID, int itemID,
            String newDescription) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, change the item name in the shop with the provided details
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.updateItemDescription(user, shop, itemID, newDescription);
            logger.info(() -> "Item description changed in shop: " + itemID + " in shop: " + shop.getName()
                    + " by user: " + userID);
            return Response.ok();
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error changing item description in shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    // customer interaction
    @Transactional
    public Response<Void> rateShop(String sessionToken, int shopID, int rating) {
        // If logged in, rate the shop with the provided rating
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }

            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            List<Order> orders = orderRepository.getOrdersByCustomerId(userID);
            shoppingService.RateShop(shop, orders, userID, rating);
            logger.info(() -> "Shop rated: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error rating shop: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> rateItem(String sessionToken, int shopID, int itemID, int rating) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, rate the item with the provided rating
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            List<Order> orders = orderRepository.getOrdersByCustomerId(userID);
            shoppingService.RateItem(shop, userID, itemID, orders, rating);
            logger.info(() -> "Item rated: " + itemID + " in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error rating item: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> sendMessage(String sessionToken, int shopId, String title, String content) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopId);
            interactionService.sendMessage(user, shop, title, content, shopRepository.getNextMessageId());
            for (int reciverId : shop.getOwnerIDs()) {
                notificationHandler.notifyUser(reciverId+"", "You have a new message from customer " + userRepository.getUserById(userID).getUsername());
            }
            for (int reciverId : shop.getManagerIDs()) {
                notificationHandler.notifyUser(reciverId+"", "You have a new message from customer " + userRepository.getUserById(userID).getUsername());
            }
            logger.info(() -> "Message sent: " + title + " in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error sending message: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<Void> respondToMessage(String sessionToken, int shopId, int messageId, String title,
            String content) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopId);
            Message response = interactionService.respondToMessage(user, shop, messageId, title, content, shopRepository.getNextMessageId());
            Registered reciver = userRepository.getUserByName(response.getUserName());
            reciver.addMessage(response);
            notificationHandler.notifyUser(reciver.getUserID()+"", "You have a new message from shop " + shop.getName());
            logger.info(() -> "Message responded: " + title + " in shop: " + shop.getName() + " by user: "
                    + user.getUsername());
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error responding to message: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<List<Message>> getInbox(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<Message> inbox = shop.getInbox();
            return Response.ok(inbox);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error getting inbox: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    // management
    @Transactional
    public Response<Void> addShopOwner(String sessionToken, int shopID, String appointeeName) {
            try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
                if (!authenticationAdapter.validateToken(sessionToken)) {
                    throw new Exception("User is not logged in");
                }
                int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
                Registered user = (Registered) this.userRepository.getUserById(userID);
                if (user.isSuspended()) {
                    return Response.error("User is suspended");
                }
                Registered appointee = this.userRepository.getUserByName(appointeeName);
                if (appointee.isSuspended()) {
                    return Response.error("User is suspended");
                }
                Shop shop = this.shopRepository.getShopById(shopID);
                this.managementService.addOwner(user, shop, appointee);
                logger.info(() -> "Shop owner added: " + appointeeName + " in shop: " + shop.getName() + " by user: "
                        + userID);
            } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
                logger.error(() -> "Error adding shop owner: " + e.getMessage());
                return Response.error("Error: " + e.getMessage());
            }
        return Response.ok();
    }

    @Transactional
    public Response<Void> removeAppointment(String sessionToken, int shopID, String appointeeName) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            
            List<IRole> appointedRoles = userRepository.getAppointmentsOfUserInShop(userID, shopID);
            IRole roleInShop = user.getRoleInShop(shopID);
            roleInShop.setAppointments(appointedRoles);

            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Registered appointee = userRepository.getUserByName(appointeeName);

            appointedRoles = userRepository.getAppointmentsOfUserInShop(appointee.getUserID(), shopID);
            roleInShop = appointee.getRoleInShop(shopID);
            roleInShop.setAppointments(appointedRoles);
            
            if (appointee.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.removeAppointment(user, shop, appointee);
            
            notificationHandler.notifyUser(appointee.getUserID() + "",
                    "You no longer have your role in shop: " + shop.getName());
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error removing appointment: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> addShopManager(String sessionToken, int shopID, String appointeeName,
            Set<Permission> permission) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Registered appointee = userRepository.getUserByName(appointeeName);
            if (appointee.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.addManager(user, shopID, appointee, permission, (Integer id) -> shopRepository.getShopByIdWithLock(id));
            logger.info(() -> "Shop manager added: " + appointeeName + " in shop: " + shop.getName() + " by user: "
                    + userID);
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error adding shop manager: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }

    @Transactional
    public Response<Void> addShopManagerPermission(String sessionToken, int shopID, String appointeeName,
            Permission permission) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Registered appointee = userRepository.getUserByName(appointeeName);
            if (appointee.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.addPermission(user, shop, appointee, permission);
            logger.info(() -> "Shop manager permission added: " + appointeeName + " in shop: " + shop.getName()
                    + " by user: " + userID);
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error adding shop manager permission: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }

    @Transactional
    public Response<Void> removeShopManagerPermission(String sessionToken, int shopID, String appointeeName,
            Permission permission) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Registered appointee = userRepository.getUserByName(appointeeName);
            if (appointee.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.removePermission(user, shop, appointee, permission);
            logger.info(() -> "Shop manager permission removed: " + appointeeName + " in shop: " + shop.getName()
                    + " by user: " + userID);
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error removing shop manager permission: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }

    @Transactional
    public Response<List<Permission>> getMemberPermissions(String sessionToken, int shopID, String memberName) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = shopRepository.getShopById(shopID);
            Registered member = userRepository.getUserByName(memberName);
            List<Permission> permissions = managementService.getMembersPermissions(user, shop, member);
            System.out.println(permissions.toString());
            logger.info(() -> "Members permissions retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(permissions);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving members permissions: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    // bids and auctions
    public Response<Void> answerBid(String sessionToken, int shopID, int bidID, boolean accept) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            List<Integer> members = userRepository.getAllRegisteredsByShopAndPermission(shopID, Permission.ANSWER_BID);
            Pair<Integer,String> notification=managementService.answerBid(user, shop, bidID, accept,members);
            if(notification!=null){
                notificationHandler.notifyUser(notification.getKey()+"",notification.getValue());
            }
            logger.info(() -> "Bid answered: " + bidID + " in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok();
        }
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error answering bid: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<Void> submitCounterBid(String sessionToken, int shopID, int bidID, double offerAmount) {
        try {
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = shopRepository.getShopById(shopID);
            managementService.submitCounterBid(user, shop, bidID, offerAmount);
            // Notify the customer about the counter bid
            String customerName = userRepository.getUserById(shop.getBidPurchase(bidID).getSubmitterId()).getUsername();
            notificationHandler.notifyUser(""+customerName, "You have got a counter bid of " + offerAmount
                    + " in shop: " + shop.getName());
            logger.info(() -> "Counter bid submitted: " + bidID + " in shop: " + shop.getName() + " by user: "
                    + userID);
            return Response.ok();
        } catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error submitting counter bid: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    
    @Transactional
    public Response<Void> openAuction(String sessionToken, int shopID, int itemID, double startingPrice,
            LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.openAuction(user, shop, itemID, startingPrice, startDate, endDate);
            logger.info(() -> "Auction opened: " + itemID + " in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error opening auction: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    // policies
    public Response<Void> addDiscount(String sessionToken, int shopID, DiscountDTO discountDetails) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.addDiscount(user, shop, discountDetails);
            logger.info(() -> "Discount added in shop: " + shop.getName() + " by user: "
                    + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error adding discount: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> removeDiscount(String sessionToken, int shopID, String discountID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.removeDiscount(user, shop, discountID);
            logger.info(() -> "Discount removed in shop: " + shop.getName() + " by user: "
                    + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error removing discount: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> updateDiscountType(String sessionToken, int shopID, DiscountType discountType) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, update the discount type for the item in the shop with the
        // provided details
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            this.managementService.updateDiscountType(user, shop, discountType);
            logger.info(() -> "Discount type updated in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error updating discount type: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    // purchase policy
    public Response<Void> updatePurchaseType(String sessionToken, int shopID, PurchaseType purchaseType) {
        // Check if the user is logged in
        // If not, prompt to log in or register
        // If logged in, update the purchase type for the item in the shop with the
        // provided details
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            this.managementService.updatePurchaseType(user, shop, purchaseType);
            logger.info(() -> "Purchase type updated in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error updating purchase type: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> addPurchaseCondition(String sessionToken, int shopID, ConditionDTO condition) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.addPurchaseCondition(user, shop, condition);
            logger.info(() -> "Purchase condition added in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error adding purchase condition: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<Void> removePurchaseCondition(String sessionToken, int shopID, String conditionID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            if (user.isSuspended()) {
                return Response.error("User is suspended");
            }
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.removePurchaseCondition(user, shop, conditionID);
            logger.info(() -> "Purchase condition removed in shop: " + shop.getName() + " by user: " + userID);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error removing purchase condition: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
        return Response.ok();
    }
    @Transactional
    public Response<List<ConditionDTO>> getPurchaseConditions(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<ConditionDTO> conditions = managementService.getPurchaseConditions(user, shop);
            logger.info(() -> "Purchase conditions retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(conditions);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving purchase conditions: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<DiscountDTO>> getDiscounts(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<DiscountDTO> discounts = managementService.getDiscounts(user, shop);
            logger.info(() -> "Discounts retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(discounts);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving discounts: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional 
    public Response<List<AuctionDTO>> getActiveAuctions(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<AuctionDTO> auctions = shop.getActiveAuctions();
            logger.info(() -> "Active auctions retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(auctions);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving active auctions: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<AuctionDTO>> getFutureAuctions(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<AuctionDTO> auctions = shop.getFutureAuctions();
            logger.info(() -> "future auctions retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(auctions);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving future auctions: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<BidDTO>> getBids(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<BidDTO> bids = managementService.getBids(user, shop).stream()
                .filter(bid -> !bid.isDone())
                .toList();
            logger.info(() -> "Active bids retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(bids);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving active bids: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    public Response<List<BidDTO>> getUserBids(String sessionToken, int shopID, String flag) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
             
            if (shop.getBids() == null) {
                throw new IllegalArgumentException("No bids found for the shop");
            }
            if (flag == null || flag.isEmpty()) {
                throw new IllegalArgumentException("Flag cannot be null or empty");
            }
            if (!flag.equals("Counter") && !flag.equals("Accepted") && !flag.equals("Rejected") && !flag.equals("In Progress")) {
                throw new IllegalArgumentException("Invalid flag value. Use 'Counter', 'In Progress', 'Accepted', or 'Rejected'.");
            }
            List<BidDTO> bids = shop.getBids().stream()
                .filter(bid -> bid.getBuyerId() == userID && bid.getCounterBidId() == -1)
                .toList();
            switch (flag) {
                case "Counter":
                    bids = bids.stream()
                        .filter(bid -> bid.getCounterAmount() != -1 && bid.getIsAccepted() == 0)
                        .toList();
                    break;
                case "Accepted":
                    bids = bids.stream()
                        .filter(bid -> bid.getIsAccepted() == 1 && !bid.isDone())
                        .toList();
                    break;
                case "Rejected":
                    bids = bids.stream()
                        .filter(bid -> bid.getIsAccepted() == -1)
                        .toList();
                    break;
                case "In Progress":
                    bids = bids.stream()
                        .filter(bid -> bid.getIsAccepted() == 0 && bid.getCounterAmount() == -1)
                        .toList();
                    break;
            }

            logger.info(() -> "Active user bids retrieved in shop: " + shop.getName() + " for user: " + userID);
            return Response.ok(bids);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving active bids: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Transactional
    public Response<Integer> getShopId(String sessionToken, String shopName) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopByName(shopName);
            return Response.ok(shop.getId());
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving shop ID: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }   
    }
    @Transactional
    public Response<List<AuctionDTO>> getWonAuctions(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            List<AuctionDTO> auctions = shop.getWonAuctions(userID);
            logger.info(() -> "Won auctions retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(auctions);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving won auctions: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }

    }
    @Transactional
    public Response<List<PurchaseType>> getPurchaseTypes(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Shop shop = this.shopRepository.getShopById(shopID);
            List<PurchaseType> purchaseTypes = shop.getPurchaseTypes();
            logger.info(() -> "Purchase types retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(purchaseTypes);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving purchase types: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public Response<List<DiscountType>> getDiscountTypes(String sessionToken, int shopID) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Shop shop = this.shopRepository.getShopById(shopID);
            List<DiscountType> discountTypes = shop.getDiscountTypes();
            logger.info(() -> "Discount types retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(discountTypes);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error retrieving discount types: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30000) // Runs every 30 seconds
    @Transactional
    public void checkAndNotifyAuctions() {
        executeSkippableOperation(() -> {
            List<Shop> shops = shopRepository.getAllShops().values().stream().toList();
            for (Shop shop : shops) {
                HashMap<Integer,String> notifications = shop.auctionMessages();
                for (Map.Entry<Integer, String> entry : notifications.entrySet()) {
                    int userId = entry.getKey();
                    String message = entry.getValue();
                    notificationHandler.notifyUser(String.valueOf(userId), message);
                }
            }
        }); 
    }

    //view shop order history
    @Transactional
    public Response<List<BasketDTO>> getShopOrderHistory(String sessionToken, int shopID) {
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            Shop shop = this.shopRepository.getShopById(shopID);
            managementService.getShopOrderHistory(user,shop);
            HashMap<Integer,List<ItemDTO>> orderHistory = orderRepository.getOrdersByShopId(shopID);
            List<BasketDTO> orderHistoryFormatted = new ArrayList<>();
            for(Map.Entry<Integer, List<ItemDTO>> entry : orderHistory.entrySet()) {
                String username = userRepository.getUserById(orderRepository.getOrder(entry.getKey()).getUserID()).getUsername();
                username = username == null ? "Guest" : username;
                BasketDTO basket = new BasketDTO(entry.getKey(),username, entry.getValue());
                orderHistoryFormatted.add(basket);
            }
            logger.info(() -> "Order history retrieved in shop: " + shop.getName() + " by user: " + userID);
            return Response.ok(orderHistoryFormatted);
        } catch (Exception e) {
            logger.error(() -> "Error retrieving order history: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }

}

