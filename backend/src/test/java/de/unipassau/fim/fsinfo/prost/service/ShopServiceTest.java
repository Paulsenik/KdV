package de.unipassau.fim.fsinfo.prost.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.unipassau.fim.fsinfo.prost.data.TransactionType;
import de.unipassau.fim.fsinfo.prost.data.UserAccessRole;
import de.unipassau.fim.fsinfo.prost.data.dao.ProstUser;
import de.unipassau.fim.fsinfo.prost.data.dao.ShopItem;
import de.unipassau.fim.fsinfo.prost.data.dao.TransactionEntry;
import de.unipassau.fim.fsinfo.prost.data.repositories.ShopItemHistoryRepository;
import de.unipassau.fim.fsinfo.prost.data.repositories.ShopItemRepository;
import de.unipassau.fim.fsinfo.prost.data.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShopServiceTest {

  @InjectMocks
  private ShopService shopService;

  @Mock
  private ShopItemRepository itemRepository;

  @Mock
  private ShopItemHistoryRepository historyRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TransactionService transactionService;

  private ShopItem shopItem;
  private ProstUser prostUser;
  private ProstUser kioskUser;

  @BeforeEach
  public void setUp() {
    shopItem = new ShopItem("item1", "category1", "Item 1", new BigDecimal("10.00"));
    prostUser = new ProstUser("user1", "Test User", "digga@test.com", true, false, true);
    kioskUser = new ProstUser("kiosk", "Kiosk", "kiosk@test.com", true, true, false);
  }

  @Test
  public void testConsume_InvalidAmount_ReturnsFalse() {
    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 0, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertFalse(result);
  }

  @Test
  public void testConsume_MissingEntities_ReturnsFalse() {
    when(itemRepository.findById(anyString())).thenReturn(Optional.empty());
    when(userRepository.findById(anyString())).thenReturn(Optional.empty());
    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertFalse(result);
  }

  @Test
  public void testConsume_DisabledItem_ReturnsFalse() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    shopItem.setEnabled(false);

    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertFalse(result);
  }

  @Test
  public void testConsume_DisabledUser_ReturnsFalse() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    prostUser.setEnabled(false);

    assertFalse(shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE));
  }

  @Test
  public void testConsume_EnabledKiosk_ReturnsTrue() {
    TransactionEntry transaction = new TransactionEntry(null, prostUser.getId(), kioskUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction));
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(kioskUser.getId())).thenReturn(Optional.of(kioskUser));

    assertTrue(shopService.consume(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
        UserAccessRole.KIOSK));
  }

  @Test
  public void testConsume_DisabledKiosk_ReturnsFalse() {
    prostUser.setKiosk(false);
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(kioskUser.getId())).thenReturn(Optional.of(kioskUser));

    assertFalse(shopService.consume(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
        UserAccessRole.KIOSK));
  }

  @Test
  public void testConsume_RegularUser_BuyHimself_ReturnsTrue() {
    TransactionEntry transaction = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction));
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));

    assertTrue(shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.FSINFO));
  }

  @Test
  public void testConsume_RegularUser_BuyOther_ReturnsFalse() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(kioskUser.getId())).thenReturn(Optional.of(kioskUser));

    assertFalse(shopService.consume(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
        UserAccessRole.FSINFO));
  }

  @Test
  public void testHasBearerPermissions_UnknownBearerRole_ReturnsFalse() {
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));

    assertFalse(
        shopService.hasBearerPermissions(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
            UserAccessRole.UNASSIGNED));
  }

  @Test
  public void testHasBearerPermissions_RegularUser() {
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));

    assertFalse(
        shopService.hasBearerPermissions(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
            UserAccessRole.FSINFO));
    assertTrue(
        shopService.hasBearerPermissions(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
            UserAccessRole.FSINFO));
  }

  @Test
  public void testHasBearerPermissions_AdminUser() {
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));

    assertTrue(
        shopService.hasBearerPermissions(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
            UserAccessRole.KAFFEEKASSE));
  }

  @Test
  public void testConsume_FailedTransaction_ReturnsFalse() {
    ShopItem invalidItem = new ShopItem("invaliditem", "null", "Invalid", BigDecimal.valueOf(-1.0));
    when(itemRepository.findById(invalidItem.getId())).thenReturn(Optional.of(invalidItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));

    boolean result = shopService.consume(invalidItem.getId(), prostUser.getId(), 1,
        prostUser.getId(), UserAccessRole.KAFFEEKASSE);
    assertFalse(result);
  }

  @Test
  public void testConsume_DisabledBearer_ReturnsFalse() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(kioskUser.getId())).thenReturn(Optional.of(kioskUser));

    kioskUser.setEnabled(false);

    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, kioskUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertFalse(result);
  }

  @Test
  public void testConsume_SuccessfulTransaction_ReturnsTrue() {
    TransactionEntry transaction = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction));

    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertTrue(result);
  }

  @Test
  public void testConsume_UnsuccessfulSecondTransactionWithoutPause_ReturnsFalse()
      throws InterruptedException {
    shopService.buyCooldownTime = 10L;

    TransactionEntry transaction = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction));

    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    assertTrue(result);
    assertTrue(shopService.hasBearerCooldown(prostUser.getId()));

    // second buy
    TransactionEntry transaction2 = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());

    boolean result2 = shopService.consume(shopItem.getId(), prostUser.getId(), 1,
        prostUser.getId(), UserAccessRole.KAFFEEKASSE);
    assertFalse(result2);
    assertTrue(shopService.hasBearerCooldown(prostUser.getId()));
  }

  @Test
  public void testConsume_SuccessfulSecondTransactionWithPause_ReturnsTrue()
      throws InterruptedException {
    shopService.buyCooldownTime = 10L;

    TransactionEntry transaction = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(userRepository.findById(prostUser.getId())).thenReturn(Optional.of(prostUser));
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction));

    boolean result = shopService.consume(shopItem.getId(), prostUser.getId(), 1, prostUser.getId(),
        UserAccessRole.KAFFEEKASSE);
    long lastTransaction = Instant.now().toEpochMilli();
    assertTrue(result);
    assertTrue(shopService.hasBearerCooldown(prostUser.getId()));

    Thread.sleep(shopService.buyCooldownTime + 1);
    assertFalse(shopService.hasBearerCooldown(prostUser.getId()));

    // second buy
    TransactionEntry transaction2 = new TransactionEntry(null, prostUser.getId(), prostUser.getId(),
        TransactionType.BUY, null, shopItem.getPrice());
    when(transactionService.moneyTransfer(any(), anyString(), anyString(), any(),
        any(TransactionType.class)))
        .thenReturn(Optional.of(transaction2));

    boolean result2 = shopService.consume(shopItem.getId(), prostUser.getId(), 1,
        prostUser.getId(), UserAccessRole.KAFFEEKASSE);
    assertTrue(result2);
    assertTrue(shopService.hasBearerCooldown(prostUser.getId()));
  }

  @Test
  public void testCreateItem_InvalidData_ReturnsEmpty() {
    Optional<ShopItem> result = shopService.createItem("", "Item 1", "Category 1",
        new BigDecimal("10.00"));
    assertTrue(result.isEmpty());
  }

  @Test
  public void testCreateItem_WrongMoneyPrecision_ReturnsEmpty() {
    Optional<ShopItem> result = shopService.createItem("moneyTest", "Money?", "Category 1",
        new BigDecimal("10.001"));
    assertTrue(result.isEmpty());
  }

  @Test
  public void testCreateItem_DuplicateIdentifier_ReturnsEmpty() {
    when(itemRepository.existsById(shopItem.getId())).thenReturn(true);
    Optional<ShopItem> result = shopService.createItem(shopItem.getId(), "Item 1", "Category 1",
        new BigDecimal("10.00"));
    assertTrue(result.isEmpty());
  }

  @Test
  public void testCreateItem_SuccessfulCreation_ReturnsItem() {
    when(itemRepository.existsById(shopItem.getId())).thenReturn(false);
    when(itemRepository.save(any(ShopItem.class))).thenReturn(shopItem);

    Optional<ShopItem> result = shopService.createItem(shopItem.getId(), shopItem.getDisplayName(),
        shopItem.getCategory(),
        shopItem.getPrice());
    assertTrue(result.isPresent());
    assertEquals(shopItem, result.get());
  }

  @Test
  public void testDelete_MissingItem_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.empty());
    Optional<ShopItem> result = shopService.delete(shopItem.getId());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testDelete_SuccessfulDeletion_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));
    Optional<ShopItem> result = shopService.delete(shopItem.getId());
    assertTrue(result.isPresent());
    assertEquals(shopItem.getId(), result.get().getId());
  }

  @Test
  public void testChangeDisplayName_InvalidData_ReturnsEmpty() {
    Optional<ShopItem> result = shopService.changeDisplayName(shopItem.getId(), "");
    assertTrue(result.isEmpty());

    result = shopService.changeDisplayName(shopItem.getId(), "no item found test");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangeDisplayName_SuccessfulChange_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changeDisplayName(shopItem.getId(), "New Name");
    assertTrue(result.isPresent());
    assertEquals("New Name", result.get().getDisplayName());
  }

  @Test
  public void testChangeCategory_InvalidData_ReturnsEmpty() {
    Optional<ShopItem> result = shopService.changeCategory(shopItem.getId(), "");
    assertTrue(result.isEmpty());

    result = shopService.changeCategory(shopItem.getId(), "testCategory");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangeCategory_SuccessfulChange_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changeCategory(shopItem.getId(), "New Category");
    assertTrue(result.isPresent());
    assertEquals("New Category", result.get().getCategory());
  }

  @Test
  public void testChangePrice_InvalidData_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(), "invalid");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangePrice_InvalidItem_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.empty());

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(), "invalid");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangePrice_PricePrecisionError_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(), "10.001");
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangePrice_PriceToHigh_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(),
        ShopService.MAX_PRICE.add(new BigDecimal("0.01")).toString());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangePrice_PriceToLow_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(),
        ShopService.MIN_PRICE.subtract(new BigDecimal("0.01")).toString());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testChangePrice_SuccessfulChange_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.changePrice(shopItem.getId(), "50.00");
    assertTrue(result.isPresent());
    assertEquals(new BigDecimal("50.00"), result.get().getPrice());
  }

  @Test
  public void testEnable_MissingItem_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.empty());
    Optional<ShopItem> result = shopService.enable(shopItem.getId());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testEnable_SuccessfulEnable_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.enable(shopItem.getId());
    assertTrue(result.isPresent());
    assertTrue(result.get().getEnabled());
  }

  @Test
  public void testDisable_MissingItem_ReturnsEmpty() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.empty());
    Optional<ShopItem> result = shopService.disable(shopItem.getId());
    assertTrue(result.isEmpty());
  }

  @Test
  public void testDisable_SuccessfulDisable_ReturnsItem() {
    when(itemRepository.findById(shopItem.getId())).thenReturn(Optional.of(shopItem));

    Optional<ShopItem> result = shopService.disable(shopItem.getId());
    assertTrue(result.isPresent());
    assertFalse(result.get().getEnabled());
  }
}
