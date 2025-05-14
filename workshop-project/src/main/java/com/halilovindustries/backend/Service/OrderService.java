package com.halilovindustries.backend.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import com.halilovindustries.backend.Domain.User.Guest;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Shop.Shop;
import com.halilovindustries.backend.Domain.User.ShoppingBasket;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IAuthentication;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DomainServices.PurchaseService;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;
import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private PurchaseService purchaseService = new PurchaseService();
    private IUserRepository userRepository;
    private IShopRepository shopRepository;
    private IOrderRepository orderRepository;
    private IAuthentication authenticationAdapter;
    private IPayment payment;
    private IShipment shipment;

    private final ConcurrencyHandler ConcurrencyHandler;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    public OrderService(IUserRepository userRepository, IShopRepository shopRepository, IOrderRepository orderRepository, IAuthentication authenticationAdapter, IPayment payment, IShipment shipment,  ConcurrencyHandler concurrencyHandler) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.orderRepository = orderRepository;
        this.authenticationAdapter = authenticationAdapter;
        this.payment = payment;
        this.shipment = shipment;
        this.ConcurrencyHandler = concurrencyHandler;
    }

    /**
     * Retrieves the contents of the user's shopping cart.
     *
     * @param sessionToken current session token
     * @return list of ItemDTOs in the cart, or null on error
     */
    public Response<List<ItemDTO>> checkCartContent(String sessionToken) {
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int userID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            
            Guest guest = userRepository.getUserById(userID);
            List<ItemDTO> itemDTOs = purchaseService.checkCartContent(guest);
            // List<ItemDTO> itemDTOs = items.stream()
            //         .map(item -> new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), item.getShopId(), item.getId(), item.getQuantity(), item.getRating()))
            //         .toList(); // Convert Item to ItemDTO

            logger.info(() -> "Cart contents: All items were listed successfully");
            return Response.ok(itemDTOs);
        } 
        catch (Exception e) {
            logger.error(() -> "Error viewing cart: " + e.getMessage());
            return Response.error("Error viewing cart: " + e.getMessage());
        }
    }

    /**
     * Adds items to the user's shopping cart.
     *
     * @param sessionToken current session token
     * @param itemDTOs list of items to add
     */
    // items = shopId, itemID
    public Response<Void> addItemsToCart(String sessionToken, HashMap<Integer, HashMap<Integer, Integer>> userItems) {
        List<Lock> acquiredLocks = new ArrayList<>();


        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest guest = userRepository.getUserById(cartID); // Get the guest user by ID
            if(guest.isSuspended()) {
                throw new Exception("User is suspended");
            }


            
            Set<Integer> shopIds = userItems.keySet(); // Get the set of shop IDs
            
            List<Integer> sortedShopIds = new ArrayList<>(shopIds);
            Collections.sort(sortedShopIds);

            // Lock all needed shops
            for (int shopId : sortedShopIds) {
                Lock shopRead = ConcurrencyHandler.getShopReadLock(shopId);
                shopRead.lock();
                acquiredLocks.add(shopRead);
            }

            HashMap<Shop, HashMap<Integer, Integer>> items = new HashMap<>(); 
            for (int shopId : sortedShopIds) {
                HashMap<Integer, Integer> itemIDs = userItems.get(shopId); // Get the item IDs for the current shop
                Shop shop = shopRepository.getShopById(shopId); // Get the shop by ID
                items.put(shop, itemIDs); // Add the shop and its items to the map
            }
            

            purchaseService.addItemsToCart(guest, items); // Add items to the cart

            logger.info(() -> "Items added to cart successfully");
            return Response.ok();
        } 
        catch (Exception e) {
            logger.error(() -> "Error adding items to cart: " + e.getMessage());
            return Response.error("Error adding items to cart: " + e.getMessage());
        }
        finally {
            // Unlock in reverse order
            Collections.reverse(acquiredLocks);
            for (Lock lock : acquiredLocks) {
                lock.unlock();
            }
        }
    }

    /**
     * Removes items from the user's shopping cart.
     *
     * @param sessionToken current session token
     * @param itemDTOs list of items to remove
     */
    // userItems = shopID, itemID
    public Response<Void> removeItemsFromCart(String sessionToken, HashMap<Integer, List<Integer>> userItems) {

        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest guest = userRepository.getUserById(cartID); // Get the guest user by I
            if(guest.isSuspended()) {
                throw new Exception("User is suspended");
            }
            purchaseService.removeItemsFromCart(guest, userItems); // Save the updated cart to the repository
            
            
            logger.info(() -> "Items removed from cart successfully");
            return Response.ok();
        } catch (Exception e) {
            logger.error(() -> "Error removing items from cart: " + e.getMessage());
            return Response.error("Error removing items from cart: " + e.getMessage());
        }
    }

    /**
     * Executes purchase of all items in the cart, creates and records an Order.
     *
     * @param sessionToken current session token
     * @return the created Order, or null on failure
     */
    public Response<Order> buyCartContent(String sessionToken, PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails) {
        List<Lock> acquiredLocks = new ArrayList<>();
        
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            if (!payment.validatePaymentDetails(paymentDetails)) {
                throw new Exception("Payment details invalid");
            }
            if (!shipment.validateShipmentDetails(shipmentDetails)) {
                throw new Exception("Shipment details invalid");
            }
            int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest guest = userRepository.getUserById(cartID); // Get the guest user by I
            if(guest.isSuspended()) {
                throw new Exception("User is suspended");
            }
            
            List<Pair<Integer, Integer>> locksToAcquire = new ArrayList<>();
        
            // First add shops (with itemID = -1 to indicate shop lock)
            Set<Integer> shopIds = guest.getCart().getBaskets().stream()
                    .map(ShoppingBasket::getShopID)
                    .collect(Collectors.toSet());
            for (int shopId : shopIds) {
                locksToAcquire.add(new Pair<>(shopId, -1));
            }

            // Then add items
            for (ShoppingBasket basket : guest.getCart().getBaskets()) {
                int shopID = basket.getShopID();
                for (ItemDTO item : basket.getItems()) {
                    locksToAcquire.add(new Pair<>(shopID, item.getItemID()));
                }
            }

            // Sort: shop locks first (itemID == -1), then by itemID
            locksToAcquire.sort(Comparator
                .comparing(Pair<Integer, Integer>::getKey)
                .thenComparing(Pair::getValue)
            );

            // Lock all
            for (Pair<Integer, Integer> pair : locksToAcquire) {
                Lock lock;
                if (pair.getValue() == -1) {
                    lock = ConcurrencyHandler.getShopReadLock(pair.getKey());
                } else {
                    lock = ConcurrencyHandler.getItemLock(pair.getKey(), pair.getValue());
                }
                lock.lockInterruptibly();
                acquiredLocks.add(lock);
            }

            // all needed shops and items are locked
            // Now we can proceed with the purchase
            List<Shop> shops = new ArrayList<>();
            for (int i = 0; i < guest.getCart().getBaskets().size(); i++) {
                int shopID = guest.getCart().getBaskets().get(i).getShopID();
                Shop shop = shopRepository.getShopById(shopID); // Get the shop by ID
                shops.add(shop); // Add the shop to the list of shops
            }
            Order order = purchaseService.buyCartContent(guest, shops, shipment, payment,orderRepository.getAllOrders().size(), paymentDetails, shipmentDetails); // Buy the cart content
            orderRepository.addOrder(order); // Save the order to the repository
            logger.info(() -> "Purchase completed successfully for cart ID: " + cartID);
            return Response.ok(order); 
        } 
        
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(() -> "Thread interrupted during cart purchase");
            return Response.error("Thread interrupted during cart purchase");
        }
        catch (Exception e) {
            logger.error(() -> "Error buying cart content: " + e.getMessage());
            return Response.error("Error buying cart content: " + e.getMessage());
        } finally {
            Collections.reverse(acquiredLocks);
            for (Lock lock : acquiredLocks) {
                lock.unlock();
            }
        }
    }

    /**
     * Submits a bid offer for a specific item.
     *
     * @param sessionToken current session token
     * @param itemID the item to bid on
     * @param offerPrice the bid amount
     */
    public Response<Void> submitBidOffer(String sessionToken, int shopId, int itemID, double offerPrice) {

        Lock shopRead = ConcurrencyHandler.getShopReadLock(shopId);
        ReentrantLock itemLock = ConcurrencyHandler.getItemLock(shopId, itemID);

        shopRead.lock();     
        try {
            itemLock.lockInterruptibly();

            try {
                if (!authenticationAdapter.validateToken(sessionToken)) {
                    throw new Exception("User not logged in");
                }
                int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
                Guest guest = userRepository.getUserById(cartID); // Get the guest user by ID
                if(guest.isSuspended()) {
                    throw new Exception("User is suspended");
                }
                Shop shop = shopRepository.getShopById(shopId); // Get the shop by ID
                purchaseService.submitBidOffer(guest,shop ,itemID, offerPrice);
    
                logger.info(() -> "Bid offer submitted successfully for item ID: " + itemID);
                return Response.ok();

            } 
            catch (Exception e) {
                logger.error(() -> "Error submitting bid offer: " + e.getMessage());
                return Response.error("Error submitting bid offer: " + e.getMessage());
            }
            finally {
                itemLock.unlock();
            }
        } 
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // handle interruption…
        } finally {
            shopRead.unlock();
        }
        return null;
    }

    /**
     * Performs a direct purchase of a single item.
     *
     * @param sessionToken current session token
     * @param itemID the item to purchase
     */
    // public Response<Void> directPurchase(String sessionToken, int shopId, int itemID) {
    //     Lock shopRead = ConcurrencyHandler.getShopReadLock(shopId);
    //     ReentrantLock itemLock = ConcurrencyHandler.getItemLock(shopId, itemID);

    //     shopRead.lock();     
    //     try {
    //         itemLock.lockInterruptibly();
    //         try {
    //             if (!authenticationAdapter.validateToken(sessionToken)) {
    //                 throw new Exception("User not logged in");
    //             }
    //             int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
    //             Guest guest = userRepository.getUserById(cartID); // Get the guest user by I
    //             purchaseService.directPurchase(guest, itemID);
    
    //             logger.info(() -> "Direct purchase completed successfully for item ID: " + itemID);
    //             return Response.ok();
    //         } catch (Exception e) {
    //             logger.error(() -> "Error completing direct purchase: " + e.getMessage());
    //             return Response.error("Error completing direct purchase: " + e.getMessage());
    //         }
    //         finally {
    //             itemLock.unlock();
    //         }
    //     } 
    //     catch (InterruptedException ie) {
    //         Thread.currentThread().interrupt();
    //         logger.error(() -> "Thread was interrupted during direct purchase");
    //         return Response.error("Thread was interrupted during direct purchase");
    //     }
    //     finally {
    //         shopRead.unlock();
    //     }
    // }

     /**
     * Retrieves the personal order history for the user.
     *
     * @param sessionToken current session token
     * @return list of past Orders, or null on error
     */
    public Response<List<Order>> viewPersonalOrderHistory(String sessionToken) {
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int userId = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            List<Order> orders = orderRepository.getOrdersByCustomerId(userId);
            logger.info(() -> "Personal search history viewed successfully for user ID: " + userId);
            return Response.ok(orders);
        } catch (Exception e) {
            logger.error(() -> "Error viewing personal search history: " + e.getMessage());
            return Response.error("Error viewing personal search history: " + e.getMessage());
        }
    }
    public Response<Void> purchaseBidItem(String sessionToken,int shopId,int bidId, PaymentDetailsDTO paymentDetalis, ShipmentDetailsDTO shipmentDetalis) {
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int userId = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest guest = userRepository.getUserById(userId); // Get the guest user by ID
            if(guest.isSuspended()) {
                throw new Exception("User is suspended");
            }
            Registered user = userRepository.getUserByName(guest.getUsername());
            Shop shop = shopRepository.getShopById(shopId); // Get the shop by ID
            List<Integer> accepetingMembers=userRepository.getAllRegisteredsByShopAndPermission(shopId, Permission.ANSWER_BID);
            Order order = purchaseService.purchaseBidItem(user,shop,bidId, orderRepository.getAllOrders().size(),payment, shipment, paymentDetalis, shipmentDetalis,accepetingMembers);
            orderRepository.addOrder(order); // Save the order to the repository
            logger.info(() -> "Bid item purchased successfully for bid ID: " + bidId);
            return Response.ok();
        } catch (Exception e) {
            logger.error(() -> "Error purchasing bid item: " + e.getMessage());
            return Response.error("Error purchasing bid item: " + e.getMessage());
        }
    }

    public Response<Void> submitAuctionOffer(String sessionToken, int shopId, int auctionID, double offerPrice) {

            try {
                if (!authenticationAdapter.validateToken(sessionToken)) {
                    throw new Exception("User not logged in");
                }
                int cartID = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
                Guest guest = userRepository.getUserById(cartID); // Get the guest user by ID
                if(guest.isSuspended()) {
                    throw new Exception("User is suspended");
                }
                Registered user = userRepository.getUserByName(guest.getUsername());
                Shop shop = shopRepository.getShopById(shopId); // Get the shop by ID
                purchaseService.submitAuctionOffer(user,shop ,auctionID, offerPrice);
    
                logger.info(() -> "Auction offer submitted successfully for item ID: " + auctionID);
                return Response.ok();

            } 
            catch (Exception e) {
                logger.error(() -> "Error submitting auction offer: " + e.getMessage());
                return Response.error("Error submitting auction offer: " + e.getMessage());
            }
    }
    public Response<Void> purchaseAuctionItem(String sessionToken,int shopId,int auctionID, PaymentDetailsDTO paymentDetalis, ShipmentDetailsDTO shipmentDetalis) {
        try {
            if (!authenticationAdapter.validateToken(sessionToken)) {
                throw new Exception("User not logged in");
            }
            int userId = Integer.parseInt(authenticationAdapter.getUsername(sessionToken));
            Guest guest = userRepository.getUserById(userId); // Get the guest user by ID
            if(guest.isSuspended()) {
                throw new Exception("User is suspended");
            }
            Registered registered = userRepository.getUserByName(guest.getUsername());
            Shop shop = shopRepository.getShopById(shopId); // Get the shop by ID
            Order order = purchaseService.purchaseAuctionItem(registered,shop,auctionID, orderRepository.getAllOrders().size(), payment, shipment, paymentDetalis, shipmentDetalis);
            orderRepository.addOrder(order); // Save the order to the repository
            logger.info(() -> "Auction item purchased successfully for auction ID: " + auctionID);
            return Response.ok();
        } catch (Exception e) {
            logger.error(() -> "Error purchasing auction item: " + e.getMessage());
            return Response.error("Error purchasing auction item: " + e.getMessage());
        }
    }
}
