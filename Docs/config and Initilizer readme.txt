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
    registerUser(String username, String password, LocalDate birthDate);
    loginUser(String username, String password);
    logoutUser(String username);
    exitAsGuest();
    suspend(String username, LocalDateTime startDate, LocalDateTime endDate);
    unsuspend(String username);

Shop Management:
    createShop(String shopName, String shopDescription);
    closeShop(int shopId);
    addShopOwner(int shopId, String newOwnerUsername);
    addShopManager(int shopId, String managerUsername, Set<Permission> permissions);
    removeAppointment(int shopId, String username);
    addPermission(int shopId, String managerUsername, Permission permission);
    removePermission(int shopId, String managerUsername, Permission permission);

Inventory & Items:
    addItem(int shopId, String itemName, Category category, double price, String description, int quantity);
    removeItem(int shopId, int itemId);
    changeItemPrice(int shopId, int itemId, double newPrice);
    changeItemQuantity(int shopId, int itemId, int quantity);
    changeItemName(int shopId, int itemId, String newName);
    changeItemDescription(int shopId, int itemId, String newDescription);
    updateDiscountType(int shopId, DiscountType type);
    updatePurchaseType(int shopId, PurchaseType type);

Ratings:
    rateShop(int shopId, int rating);
    rateItem(int shopId, int itemId, int rating);

Messaging:
    sendMessage(int shopId, String subject, String content);
    respondToMessage(int shopId, int messageId, String subject, String content);

Cart:
    addToCart(int shopId, int itemId, int quantity);
    removeFromCart(int shopId, int itemId);


Auction & Bidding:
    openAuction(int shopId, int itemId, double startingPrice, LocalDate start, LocalDate end);
    answerBid(int shopId, int bidId, boolean accept);
    submitCounterBid(int shopId, int bidId, double newPrice);

