package Tests.AcceptanceTests;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import io.jsonwebtoken.lang.Assert;
import jakarta.validation.constraints.AssertTrue;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;

public class DiscountsTests extends BaseAcceptanceTests {
    @BeforeEach
    public void setUp() {
        super.setUp();
    }
    
    // Test case for updating the discount type
    @Test
    public void testUpdateDiscountType_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
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
            "1234567890123456", "Some Name", "1","12/25", "123"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
        HashMap<Integer, Integer> itemMap = new HashMap<>();
        itemMap.put(List.of(toBuy).get(0).getItemID(), 1);
        itemsMap.put(shop.getId(), itemMap);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);

        // 3) Add to cart
        Response<Void> addResp = orderService.addItemsToCart(
            customerToken,
            itemsMap
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
            "1234567890123456", "Some Name", "1","12/25", "123"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        DiscountDTO discount = new DiscountDTO(DiscountKind.MAX,-1, Category.ELECTRONICS, 10, null,0,null,50,null ,DiscountType.BASE,DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");

        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
        HashMap<Integer, Integer> itemMap = new HashMap<>();
        itemMap.put(List.of(toBuy).get(0).getItemID(), 1);
        //apple
        itemMap.put(0,1);
        itemsMap.put(shop.getId(), itemMap);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);

        // 3) Add to cart
        Response<Void> addResp = orderService.addItemsToCart(
            customerToken,
            itemsMap
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        Order created = fixtures.successfulBuyCartContent(customerToken,p,s);
        assertTrue(created.getTotalPrice() < toBuy.getPrice(), "Discount should be applied to the total price");
    }



    // Test case for removing a discount
    @Test
    public void testRemoveDiscount_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");
        
        System.out.println(shopRepository.getShopById(shop.getId()).getDiscountPolicy().getDiscountIds().get(0));
        Response<Void> removeResponse = shopService.removeDiscount(ownerToken, shop.getId(),shopRepository.getShopById(shop.getId()).getDiscountPolicy().getDiscountIds().get(0));
        // Assert
        assertTrue(removeResponse.isOk(), "Owner should be able to remove a discount");
    }
    @Test
    public void testRemoveDiscount_NotByOwner_ShouldFail() {
        // Arrange
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        DiscountDTO discount = new DiscountDTO(DiscountKind.BASE,-1, Category.ELECTRONICS, 10, null, DiscountType.BASE);
        // Act
        Response<Void> response = shopService.addDiscount(ownerToken, shop.getId(), discount);
        assertTrue(response.isOk(), "Owner should be able to add a discount");
        Response<Void> removeResponse = shopService.removeDiscount(customerToken, shop.getId(), 1);
        // Assert
        assertFalse(removeResponse.isOk(), "Customer should not be able to remove a discount");
    }
    @Test
    public void testRemoveDiscount_DiscountDoesntExists_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        Response<Void> removeResponse = shopService.removeDiscount(ownerToken, shop.getId(), 1);
        // Assert
        assertFalse(removeResponse.isOk(), "Owner should not be able to remove a discount that doesn't exist");
    }




    //Conditions
    @Test
    public void testAddPurchaseCondition_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, 100,-1,ConditionLimits.PRICE);
        Response<Void> addRsp=shopService.addPurchaseConditon(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
    }
    @Test
    public void testAddPurchaseANDCondition_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        ConditionDTO condition = new ConditionDTO(ConditionType.AND,-1, Category.ELECTRONICS, 100,-1,0,3,-1,null,ConditionLimits.PRICE,ConditionLimits.QUANTITY);
        Response<Void> addRsp=shopService.addPurchaseConditon(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
    }

    @Test
    public void testRemovePurchaseCondition_AsOwner_ShouldSucceed(){
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, 100,-1,ConditionLimits.PRICE);
        Response<Void> addRsp=shopService.addPurchaseConditon(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
        Response<Void> removeRsp=shopService.removePurchaseCondition(ownerToken, shop.getId(), 0);
        assertTrue(removeRsp.isOk(), "Owner should be able to remove a purchase condition");
    }

    @Test
    public void testPurchaseConditionBlocksPurchase_ShouldSucceed(){
        // Arrange
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1","12/25", "123"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String customerToken = fixtures.generateRegisteredUserSession("Customer", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        //should buy electronics worth 1000
        ConditionDTO condition = new ConditionDTO(-1, Category.ELECTRONICS, 1000,-1,ConditionLimits.PRICE);
        Response<Void> addRsp=shopService.addPurchaseConditon(ownerToken, shop.getId(), condition);
        assertTrue(addRsp.isOk(), "Owner should be able to add a purchase condition");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(2);
        HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
        HashMap<Integer, Integer> itemMap = new HashMap<>();
        itemMap.put(List.of(toBuy).get(0).getItemID(), 1);
        itemsMap.put(shop.getId(), itemMap);
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), 2, 2);
        // 3) Add to cart
        Response<Void> addResp = orderService.addItemsToCart(
            customerToken,
            itemsMap
        );
        assertTrue(addResp.isOk(), "Adding items to cart should succeed");
        // 4) Checkout
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        Response<Order> purchaseResp = orderService.buyCartContent(
            customerToken, p, s
        );
        assertFalse(purchaseResp.isOk(), "Discount should be applied to the total price");
    }

}
