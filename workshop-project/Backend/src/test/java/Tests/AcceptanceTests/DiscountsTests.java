package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Domain.Shop.Policies.Discount.DiscountType;

import Domain.Response;

import Domain.DTOs.ShopDTO;

public class DiscountsTests extends BaseAcceptanceTests {
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testUpdateDiscountType_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // Act
        Response<Void> response = shopService.updateDiscountType(ownerToken, shop.getId(), DiscountType.HIDDEN);

        // Assert
        assertTrue(response.isOk(), "Owner should be able to update the discount type");
    }
}
