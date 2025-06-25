package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.*;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.User.Permission;

public class ComprehensiveDiscountTests extends BaseAcceptanceTests {

    private String ownerToken;
    private String managerToken;
    private String customerToken;
    private ShopDTO shop;
    private List<ItemDTO> items;
    private PaymentDetailsDTO payment;
    private ShipmentDetailsDTO shipment;

    @BeforeEach
    public void setUp() {
        super.setUp();
        
        // Create standard users for tests
        ownerToken = fixtures.generateRegisteredUserSession("DiscountOwner", "Pwd123");
        managerToken = fixtures.generateRegisteredUserSession("DiscountManager", "Pwd123");
        customerToken = fixtures.generateRegisteredUserSession("DiscountCustomer", "Pwd123");
        
        // Create a shop with items
        shop = fixtures.generateShopAndItems(ownerToken, "DiscountTestShop");
        items = shopService.showShopItems(ownerToken, shop.getId()).getData();
        
        // Add manager with permissions
        Set<Permission> managerPermissions = new HashSet<>();
        managerPermissions.add(Permission.UPDATE_DISCOUNT_POLICY);
        shopService.addShopManager(ownerToken, shop.getId(), "DiscountManager", 
               managerPermissions); 
        
        // Set up payment and shipment details for purchases
        payment = new PaymentDetailsDTO("1234567890123456", "Test User", "1", "123", "12", "25");
        shipment = new ShipmentDetailsDTO("1", "Test User", "", "123456789", "Country", "City", "Address", "12345");
        
        // Ensure items have sufficient quantity
        for (ItemDTO item : items) {
            shopService.changeItemQuantityInShop(ownerToken, shop.getId(), item.getItemID(), 10);
        }
    }

    // ============== SHOP-WIDE DISCOUNT TESTS ==============
    
    @Test
    public void testShopWideBaseDiscount() {
        // Create a 20% shop-wide discount
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE,  // Basic discount
            -1,                 // No specific item ID
            null,               // No specific category
            20,                 // 20% off
            null,               // No condition
            DiscountType.BASE   // Base discount type
        );
        
        // Add the discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add a shop-wide discount");
        
        // Get all discounts and verify
        Response<List<DiscountDTO>> discountsResp = shopService.getDiscounts(ownerToken, shop.getId());
        assertTrue(discountsResp.isOk(), "Should retrieve discounts successfully");
        assertEquals(1, discountsResp.getData().size(), "Should have one discount");
        assertEquals(20, discountsResp.getData().get(0).getPercentage(), "Discount percentage should be 20%");
        
        // Test the actual effect on purchase
        ItemDTO item = items.get(0);
        double originalPrice = item.getPrice();
        
        // Add item to cart
        orderService.addItemToCart(customerToken, shop.getId(), item.getItemID(), 1);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify discount was applied
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedDiscountedPrice = originalPrice * 0.8; // 20% off
        assertEquals(expectedDiscountedPrice, orderResp.getData().getTotalPrice(), 0.01, 
            "Price should reflect 20% shop-wide discount");
    }
    
    // ============== CATEGORY-SPECIFIC DISCOUNT TESTS ==============
    
    @Test
    public void testCategoryDiscount() {
        // Create a 15% discount on Electronics category
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE,
            -1,                     // No specific item
            Category.ELECTRONICS,   // Electronics category  
            15,                     // 15% off
            null,                   // No condition
            DiscountType.BASE
        );
        
        // Add the discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add a category discount");
        
        // Add two items to cart - one Electronics and one non-Electronics
        ItemDTO electronics = items.stream()
            .filter(i -> i.getCategory() == Category.ELECTRONICS)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No Electronics item found"));
        
        ItemDTO other = items.stream()
            .filter(i -> i.getCategory() != Category.ELECTRONICS)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No non-Electronics item found"));
        
        orderService.addItemToCart(customerToken, shop.getId(), electronics.getItemID(), 1);
        orderService.addItemToCart(customerToken, shop.getId(), other.getItemID(), 1);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify discount was applied only to Electronics
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = (electronics.getPrice() * 0.85) + other.getPrice();
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "Discount should apply only to Electronics category");
    }
    
    // ============== ITEM-SPECIFIC DISCOUNT TESTS ==============
    
    @Test
    public void testItemSpecificDiscount() {
        // Get the first item
        ItemDTO targetItem = items.get(0);
        
        // Create a 25% discount on the specific item
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE,
            targetItem.getItemID(),  // Specific item ID
            null,                    // No category
            25,                      // 25% off
            null,                    // No condition
            DiscountType.BASE
        );
        
        // Add the discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add an item-specific discount");
        
        // Add target item and another item to cart
        orderService.addItemToCart(customerToken, shop.getId(), targetItem.getItemID(), 1);
        orderService.addItemToCart(customerToken, shop.getId(), items.get(1).getItemID(), 1);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify discount was applied only to the specific item
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = (targetItem.getPrice() * 0.75) + items.get(1).getPrice();
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "Discount should apply only to the specific item");
    }
    
    // ============== CONDITIONAL DISCOUNT TESTS ==============
    
    @Test
    public void testConditionalDiscount_ConditionMet() {
        // Create a condition: minimum quantity of 3
        ConditionDTO condition = new ConditionDTO(
            -1,                    // No specific item
            Category.ELECTRONICS,  // Electronics category
            ConditionLimits.QUANTITY,  // Quantity condition
            0,                     // Min price (not used)
            -1,                    // Max price (not used)
            -1,                    // Min quantity
            3                      // Min quantity of 3
        );
        
        // Create a 30% discount if condition is met
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.CONDITIONAL,
            -1,
            Category.ELECTRONICS,
            30,
            condition,
            DiscountType.CONDITIONAL
        );
        
        // Add the discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add a conditional discount");
        
        // Add electronics item to cart with sufficient quantity to meet condition
        ItemDTO electronics = items.stream()
            .filter(i -> i.getCategory() == Category.ELECTRONICS)
            .findFirst()
            .orElseThrow();
            
        orderService.addItemToCart(customerToken, shop.getId(), electronics.getItemID(), 3);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify discount was applied as condition was met
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = electronics.getPrice() * 3 * 0.7;
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "Discount should be applied when condition is met");
    }
    
    @Test
    public void testConditionalDiscount_ConditionNotMet() {
        // Create a condition: minimum quantity of 5
        ConditionDTO condition = new ConditionDTO(
            -1,
            Category.ELECTRONICS,
            ConditionLimits.QUANTITY,
            0,
            -1,
            5, // Minimum quantity of 5
            -1 
        );
        
        // Create a 30% discount if condition is met
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.CONDITIONAL,
            -1,
            Category.ELECTRONICS,
            30,
            condition,
            DiscountType.CONDITIONAL
        );
        
        // Add the discount
        shopService.addDiscount(ownerToken, shop.getId(), discount);
        
        // Add electronics item to cart with insufficient quantity to meet condition
        ItemDTO electronics = items.stream()
            .filter(i -> i.getCategory() == Category.ELECTRONICS)
            .findFirst()
            .orElseThrow();
            
        orderService.addItemToCart(customerToken, shop.getId(), electronics.getItemID(), 2);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify discount was NOT applied as condition was not met
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = electronics.getPrice() * 2; // No discount
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "Discount should not be applied when condition is not met");
    }
    
    // ============== COMPOSITE DISCOUNT TESTS ==============
    
    @Test
    public void testMaxDiscount() {
        // Create two discounts for MAX composition
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.MAX,
            -1,                // No specific item for first discount
            Category.ELECTRONICS,    // Books category for first discount
            10,                // 10% off books
            null,              // No condition
            -1,                // No specific item for second discount
            null,              // No specific category for second discount
            20,                // 20% off shop-wide in second discount
            null,              // No condition for second discount
            DiscountType.BASE, // First discount type
            DiscountType.BASE  // Second discount type
        );
        
        // Add the composite discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add a MAX discount");
        
        // Add items to cart
        ItemDTO laptop = items.stream()
            .filter(i -> i.getCategory() == Category.ELECTRONICS)
            .findFirst()
            .orElseThrow();
            
        orderService.addItemToCart(customerToken, shop.getId(), laptop.getItemID(), 1);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify the higher discount (20%) was applied
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = laptop.getPrice() * 0.8;  // 20% off
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "MAX discount should apply the higher discount (20%)");
    }
    
    @Test
    public void testCombinedDiscount() {
        // Create a combined discount: 10% shop-wide AND 15% on Electronics
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.COMBINE,
            -1,                 // No specific item for first discount
            null,               // No category for first discount
            10,                 // 10% off shop-wide
            null,               // No condition
            -1,                 // No specific item for second discount
            Category.ELECTRONICS, // Electronics category for second discount
            15,                 // 15% off electronics
            null,               // No condition
            DiscountType.BASE,  // First discount type
            DiscountType.BASE   // Second discount type
        );
        
        // Add the combined discount
        Response<Void> addResp = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Owner should be able to add a COMBINE discount");
        
        // Add an electronics item to cart
        ItemDTO electronics = items.stream()
            .filter(i -> i.getCategory() == Category.ELECTRONICS)
            .findFirst()
            .orElseThrow();
            
        orderService.addItemToCart(customerToken, shop.getId(), electronics.getItemID(), 1);
        
        // Complete purchase
        fixtures.mockPositivePayment(payment);
        fixtures.mockPositiveShipment(shipment);
        Response<Order> orderResp = orderService.buyCartContent(customerToken, payment, shipment);
        
        // Verify combined discount was applied
        // For combined discounts, we apply both: (1-0.1)*(1-0.15) = 0.9*0.85 = 0.765
        // So effective discount is 23.5%
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        double expectedPrice = electronics.getPrice() * 0.765;
        assertEquals(expectedPrice, orderResp.getData().getTotalPrice(), 0.01,
            "COMBINED discount should apply both discounts multiplicatively");
    }
    
    // ============== PERMISSION TESTS ==============
    
    @Test
    public void testManagerPermissions() {
        // Create a basic discount
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE,
            -1,
            null,
            10,
            null,
            DiscountType.BASE
        );
        
        // Manager should be able to add the discount
        Response<Void> addResp = shopService.addDiscount(managerToken, shop.getId(), discount);
        assertTrue(addResp.isOk(), "Manager with permission should be able to add discount");
        
        // Get the discount ID
        Response<List<DiscountDTO>> discountsResp = shopService.getDiscounts(ownerToken, shop.getId());
        String discountId = discountsResp.getData().get(0).getId();
        
        // Remove manager's permissions
        Response<Void> updateResp = shopService.removeShopManagerPermission(
            ownerToken, shop.getId(), "DiscountManager", Permission.UPDATE_DISCOUNT_POLICY);
        assertTrue(updateResp.isOk(), "Owner should be able to update manager permissions");
        
        // Try to remove the discount - should fail now
        Response<Void> removeResp = shopService.removeDiscount(managerToken, shop.getId(), discountId);
        assertFalse(removeResp.isOk(), "Manager without permission should not be able to remove discount");
    }
    
    @Test
    public void testCustomerPermissions() {
        // Create a discount as owner
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE, -1, null, 10, null, DiscountType.BASE);
        shopService.addDiscount(ownerToken, shop.getId(), discount);
        
        // Get the discount ID
        Response<List<DiscountDTO>> discountsResp = shopService.getDiscounts(ownerToken, shop.getId());
        String discountId = discountsResp.getData().get(0).getId();
        
        // Customer should not be able to add or remove discounts
        Response<Void> addResp = shopService.addDiscount(customerToken, shop.getId(), discount);
        assertFalse(addResp.isOk(), "Customer should not be able to add discount");
        
        Response<Void> removeResp = shopService.removeDiscount(customerToken, shop.getId(), discountId);
        assertFalse(removeResp.isOk(), "Customer should not be able to remove discount");
    }
    
    
    // ============== GETTERS AND SETTERS TESTS ==============
    
    @Test
    public void testDiscountTypeUpdates() {
        Response<List<DiscountType>> typesResp1 = shopService.getDiscountTypes(ownerToken, shop.getId());
        assertTrue(typesResp1.isOk(), "Should be able to get discount types");
        List<DiscountType> types1 = typesResp1.getData();
        assertTrue(types1.contains(DiscountType.CONDITIONAL), "Types should include CONDITIONAL");

        // Update discount types
        // Verify the update
        Response<Void> updateResp = shopService.updateDiscountType(ownerToken, shop.getId(), DiscountType.CONDITIONAL);
        assertTrue(updateResp.isOk(), "Owner should be able to update discount types");
        
        Response<List<DiscountType>> typesResp = shopService.getDiscountTypes(ownerToken, shop.getId());
        assertTrue(typesResp.isOk(), "Should be able to get discount types");
        List<DiscountType> types = typesResp.getData();
        assertFalse(types.contains(DiscountType.CONDITIONAL), "Types should include CONDITIONAL");
        assertTrue(types.contains(DiscountType.BASE), "Types should include BASE (default)");
        
        // Test removing a type
        updateResp = shopService.updateDiscountType(ownerToken, shop.getId(), DiscountType.BASE);
        assertTrue(updateResp.isOk(), "Owner should be able to remove a discount type");
        
        // Verify the removal
        typesResp = shopService.getDiscountTypes(ownerToken, shop.getId());
        types = typesResp.getData();
        assertFalse(types.contains(DiscountType.BASE), "BASE should be removed");
        assertFalse(types.contains(DiscountType.CONDITIONAL), "CONDITIONAL should remain");
    }
}