this file explains how to work with the config and init

config:

the config has 3 areas:
1.    the database area: change what database the system works with.
      simply change the url and driver of the database you wish to work with.

2.    external: change what payment and shipment the system works with.

3.    startup: change what file the Initializer reads for the startup


init:
init is built with commands to the system to execute on after another.
it wont give feedback which means the changes happens only inside the system.
the whole thing happens in one session, which means i cant login into different accounts at the same time.
only service commands are viable here and those are the signatures:

User Management:
    register-user(String username, String password, LocalDate birthDate)
    login-user(String username, String password)
    logout-user(String username)
    exit-as-guest()
    suspend(String username, LocalDateTime startDate, LocalDateTime endDate)
    unsuspend(String username)


Shop Management:
    create-shop(String shopName, String shopDescription)
    close-shop(int shopId)
    add-shop-owner(int shopId, String newOwnerUsername)
    add-shop-manager(int shopId, String managerUsername, Set<Permission> permissions)
    remove-appointment(int shopId, String username)
    add-permission(int shopId, String managerUsername, Permission permission)
    remove-permission(int shopId, String managerUsername, Permission permission)


Inventory & Items:
    add-item(int shopId, String itemName, Category category, double price, String description, int quantity)
    remove-item(int shopId, int itemId)
    change-item-price(int shopId, int itemId, double newPrice)
    change-item-quantity(int shopId, int itemId, int quantity)
    change-item-name(int shopId, int itemId, String newName)
    change-item-description(int shopId, int itemId, String newDescription)
    update-discount-type(int shopId, DiscountType type)
    update-purchase-type(int shopId, PurchaseType type)


Ratings:
    rate-shop(int shopId, int rating)
    rate-item(int shopId, int itemId, int rating)


Messaging:
    send-message(int shopId, String subject, String content)
    respond-to-message(int shopId, int messageId, String subject, String content)


Cart:
    add-to-cart(int shopId, int itemId, int quantity)
    remove-from-cart(int shopId, int itemId)



Auction & Bidding:
    open-auction(int shopId, int itemId, double startingPrice, LocalDate start, LocalDate end)
    answer-bid(int shopId, int bidId, boolean accept)
    submit-counter-bid(int shopId, int bidId, double newPrice)


