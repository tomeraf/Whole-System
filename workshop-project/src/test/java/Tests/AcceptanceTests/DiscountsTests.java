package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import java.util.List;

public class DiscountsTests extends BaseAcceptanceTests {
    @BeforeEach
    public void setUp() {
        super.setUp();
    }


    // --- OR Condition Tests ---
    @Test
    public void testORCondition_AllowsPurchaseIfEitherConditionMet() {
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        // Add OR condition: Either total price >= 100 or quantity >= 3
        ConditionDTO orCondition = new ConditionDTO(
            ConditionType.OR, -1, Category.ELECTRONICS, ConditionLimits.PRICE, 100, -1, -1, -1,
            ConditionLimits.QUANTITY, 0, -1, -1, 1, 3, null
        );
        Response<Void> addRsp = shopService.addPurchaseCondition(ownerToken, shop.getId(), orCondition);
        assertTrue(addRsp.isOk(), "Owner should be able to add an OR purchase condition");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);
        // Add to cart (only 1, so quantity condition not met, but price may be)
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        Response<Order> buyRsp = orderService.buyCartContent(
            customerToken, p, s
        );
        assertTrue(buyRsp.isOk(), "Purchase should succeed because PRICE condition is met");
        // Invariant: order recorded in history
        List<Order> history = orderService.viewPersonalOrderHistory(customerToken).getData();
        assertEquals(1, history.size(), "Order history should contain the successful purchase");
    }

    @Test
    public void testORCondition_BlocksPurchaseIfNoneConditionsMet() {
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        // Add OR condition: Either total price >= 10000 or quantity >= 10
        ConditionDTO orCondition = new ConditionDTO(
            ConditionType.OR, -1, Category.ELECTRONICS, ConditionLimits.PRICE, 10000, -1, -1, -1,
            ConditionLimits.QUANTITY, 0, -1, -1, 1, 10, null
        );
        Response<Void> addRsp = shopService.addPurchaseCondition(ownerToken, shop.getId(), orCondition);
        assertTrue(addRsp.isOk(), "Owner should be able to add an OR purchase condition");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);
        // Add to cart (only 1, so neither price nor quantity condition met)
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        Response<Order> buyRsp = orderService.buyCartContent(
            customerToken, p, s
        );
        assertFalse(buyRsp.isOk(), "Purchase should be blocked because no OR condition is met");
        // Invariant: order history remains empty
        List<Order> history = orderService.viewPersonalOrderHistory(customerToken).getData();
        assertTrue(history.isEmpty(), "Order history should remain empty after blocked purchase");
    }

    
    // Test case for updating the discount type
    @Test
    public void testUpdateDiscountType_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Act
        Response<Void> response = shopService.updateDiscountType(ownerToken, shop.getId(), DiscountType.CONDITIONAL);

        // Assert
        assertTrue(response.isOk(), "Owner should be able to update the discount type");
    }
    @Test
    public void testUpdateDiscountType_NotByOwner_ShouldFail() {
        // Arrange
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        // Act
        Response<Void> response = shopService.updateDiscountType(customerToken, shop.getId(), DiscountType.CONDITIONAL);
        // Assert
        assertFalse(response.isOk(), "Customer should not be able to update the discount type");
    }

    //Test case for adding a discount
    @Test
    public void testAddDiscount_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);

        // Assert
        assertTrue(response.isOk(), "Owner should be able to add a discount");
    }
    @Test
    public void testDiscountAppliedToBasket_ShouldSucceed() {
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);

        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        Order created = fixtures.successfulBuyCartContent(customerToken,p,s);
        assertTrue(created.getTotalPrice() < toBuy.getPrice(), "Discount should be applied to the total price");
    }
    @Test
    public void testMaxDiscountAppliedToBasket_ShouldSucceed(){
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.MAX,-1, Category.ELECTRONICS, 10, null,0,null,50,null ,DiscountType.BASE,DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);

        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        Order created = fixtures.successfulBuyCartContent(customerToken,p,s);
        assertTrue(created.getTotalPrice() < toBuy.getPrice(), "Discount should be applied to the total price");
    }
    @Test
    public void testCombinedDiscountAppliedToBasket_ShouldSucceed(){
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.COMBINE,-1, Category.ELECTRONICS, 10, null,-1,null,50,null ,DiscountType.BASE,DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);

        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        Order created = fixtures.successfulBuyCartContent(customerToken,p,s);
        System.out.println("Total price: " + created.getTotalPrice());
        System.out.println("Item price: " + toBuy.getPrice());
        assertTrue(created.getTotalPrice()==toBuy.getPrice()*0.45, "Discount should be applied to the total price");
    }



    // Test case for removing a discount
    @Test
    public void testRemoveDiscount_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");
        
        Response<Void> removeResponse = shopService.removeDiscount(ownerToken, shop.getId(),shopRepository.getShopById(shop.getId()).getDiscountPolicy().getDiscountIds().get(0));
        // Assert
        assertTrue(removeResponse.isOk(), "Owner should be able to remove a discount");
    }
    @Test
    public void testRemoveDiscount_NotByOwner_ShouldFail() {
        // Arrange
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");
        Response<List<DiscountDTO>> res=shopService.getDiscounts(ownerToken, shop.getId());
        Response<Void> removeResponse = shopService.removeDiscount(customerToken, shop.getId(), res.getData().get(0).getId());
        // Assert
        assertFalse(removeResponse.isOk(), "Customer should not be able to remove a discount");
    }
    @Test
    public void testRemoveDiscount_DiscountDoesntExists_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        Response<Void> removeResponse = shopService.removeDiscount(ownerToken, shop.getId(), "");
        // Assert
        assertFalse(removeResponse.isOk(), "Owner should not be able to remove a discount that doesn't exist");
    }




    //Conditions
    @Test
    public void testAddPurchaseCondition_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, ConditionLimits.PRICE, 100, -1, -1, -1);
        Response<Void> addRsp=shopService.addPurchaseCondition(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
    }
    @Test
    public void testAddPurchaseANDCondition_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        ConditionDTO condition = new ConditionDTO(ConditionType.AND, -1, Category.ELECTRONICS, ConditionLimits.PRICE, 100, -1, -1, -1, ConditionLimits.QUANTITY, 0, -1, -1, 1, 3, null);
        Response<Void> addRsp=shopService.addPurchaseCondition(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
    }

    @Test
    public void testRemovePurchaseCondition_AsOwner_ShouldSucceed(){
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS,
            ConditionLimits.PRICE, 100, -1, -1, -1);
        Response<Void> addRsp = shopService.addPurchaseCondition(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");

        // Fetch current conditions and remove the one we just added
        List<ConditionDTO> conditions = shopService.getPurchaseConditions(ownerToken, shop.getId()).getData();
        assertFalse(conditions.isEmpty(), "There should be at least one condition");
        String condId = conditions.get(0).getId();

        // Act: remove by actual condition ID
        Response<Void> removeRsp = shopService.removePurchaseCondition(ownerToken, shop.getId(), condId);

        assertTrue(removeRsp.isOk(), "Owner should be able to remove a purchase condition");
        // Invariant: no conditions remain
        List<ConditionDTO> afterRemove = shopService.getPurchaseConditions(ownerToken, shop.getId()).getData();
        assertTrue(afterRemove.isEmpty(), "Condition list should be empty after removal");

    }

    @Test
    public void testPurchaseConditionBlocksPurchase_ShouldSucceed(){
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        //should buy electronics worth 1000
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, ConditionLimits.PRICE, 1000, -1, -1, -1);
        Response<Void> addRsp=shopService.addPurchaseCondition(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);
        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            customerToken,
            shop.getId(),
            List.of(toBuy).get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        Response<Order> purchaseResp = orderService.buyCartContent(
            customerToken, p, s
        );
        assertFalse(purchaseResp.isOk(), "Purchase should be blocked by the condition");
    }

    @Test
    public void testAddDiscountAndCondition_CompositeConditionalDiscount() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        // Create a condition - minimum quantity of 2
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, ConditionLimits.QUANTITY, 0, -1, -1, 2);
        
        // Create a conditional discount - 15% off electronics if quantity >= 2
        DiscountDTO discount = new DiscountDTO(
            DiscountKind.BASE, 
            -1, 
            Category.ELECTRONICS, 
            15, 
            condition, 
            DiscountType.CONDITIONAL
        );

        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        
        // Assert
        assertTrue(response.isOk(), "Owner should be able to add a conditional discount");
        
        // Verify discount was added correctly
        Response<List<DiscountDTO>> discountsResp = shopService.getDiscounts(ownerToken, shop.getId());
        assertTrue(discountsResp.isOk(), "Should be able to retrieve discounts");
        List<DiscountDTO> discounts = discountsResp.getData();

        assertEquals(1, discounts.size(), "Should have exactly one discount");
        assertEquals(DiscountType.CONDITIONAL, discounts.get(0).getDiscountType(), "Should be a conditional discount");
        assertEquals(15, discounts.get(0).getPercentage(), "Discount percentage should be 15%");
        assertFalse(discounts.get(0).getId().isEmpty(), "Discount ID should be assigned");
    }

}
