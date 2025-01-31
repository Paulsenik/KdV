package de.unipassau.fim.fsinfo.prost.service.statistics;

import de.unipassau.fim.fsinfo.prost.data.dao.ProstUser;
import de.unipassau.fim.fsinfo.prost.data.dao.ShopItem;
import de.unipassau.fim.fsinfo.prost.data.dao.ShopItemHistoryEntry;
import de.unipassau.fim.fsinfo.prost.data.repositories.ShopItemHistoryRepository;
import de.unipassau.fim.fsinfo.prost.data.repositories.ShopItemRepository;
import de.unipassau.fim.fsinfo.prost.data.repositories.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricService {

  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");

  private final UserRepository userRepository;
  private final ShopItemRepository shopItemRepository;
  private final ShopItemHistoryRepository shopItemHistoryRepository;
  private static final Long UPDATE_INTERVAL = 1000L * 60 * 60;

  @Autowired
  public MetricService(UserRepository userRepository, ShopItemRepository shopItemRepository,
      ShopItemHistoryRepository shopItemHistoryRepository) {
    this.userRepository = userRepository;
    this.shopItemRepository = shopItemRepository;
    this.shopItemHistoryRepository = shopItemHistoryRepository;

    Timer t = new Timer();
    t.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        resetMetric();
      }
    }, UPDATE_INTERVAL, UPDATE_INTERVAL);
  }

  public long resetMetric() {
    System.out.println(
        "[MS] :: Resetting Metrics :: started at " + dateTimeFormatter.format(
            LocalDateTime.now()));
    long currentTime = System.currentTimeMillis();
    AbstractMetricCollector.initAllCollectors(ProstUser.class, userRepository.findByHidden(false));
    AbstractMetricCollector.initAllCollectors(ShopItem.class, shopItemRepository.findAll());
    AbstractMetricCollector.initAllCollectors(ShopItemHistoryEntry.class,
        shopItemHistoryRepository.findAll());
    long dur = (System.currentTimeMillis() - currentTime);
    System.out.println("[MS] :: Resetting Metrics :: finished after " + dur + " ms");
    return dur;
  }

  public void removeFromMetrics(ProstUser user) {
    AbstractMetricCollector.removeAllEntriesFor(ProstUser.class, user);

    List<ShopItemHistoryEntry> entry = shopItemHistoryRepository.findByUserId(user.getId());
    for (ShopItemHistoryEntry e : entry) {
      AbstractMetricCollector.removeAllEntriesFor(ShopItemHistoryEntry.class, e);
    }
  }

  public void addToMetrics(ProstUser user) {
    AbstractMetricCollector.updateAllEntriesFor(ProstUser.class, user);

    List<ShopItemHistoryEntry> entry = shopItemHistoryRepository.findByUserId(user.getId());
    for (ShopItemHistoryEntry e : entry) {
      AbstractMetricCollector.updateAllEntriesFor(ShopItemHistoryEntry.class, e);
    }
  }

}
