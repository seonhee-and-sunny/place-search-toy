
package com.example.placesearchtoy.application.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordScheduler {

  private final RedisTemplate<String, String> redisTemplate;

  private ZSetOperations<String, String> zSetOperations;
  private ValueOperations<String, String> valueOperations;

  @Value("${keyword.redis-key-prefix.minute-ranking}")
  private String minuteKeyPrefix;

  @Value("${keyword.redis-key-prefix.accumulate-ranking}")
  private String accumulateKeyPrefix;

  @Value("${keyword.redis-key-prefix.view}")
  private String viewKey;

  @Value("${keyword.redis-key-prefix.distributed-lock}")
  private String distributedLockKey;

  private final int accumulateStandardMinute = 10;
  private final DateTimeFormatter suffixFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  /**
   * 누적 키워드 조회 정보 저장
   */
  @Scheduled(cron = "0 0/1 * * * *")
  public void createAccumulateRanking() {
    var now = LocalDateTime.now();
    if (!tryLock(distributedLockKey + now.format(suffixFormatter))) {
      return;
    }

    var previousTop30s = getPreviousAccumulateTop30s(now, 1);
    if (previousTop30s == null || previousTop30s.size() <= 0) {
      return;
    }

    var tenMinuteAgos = getMinuteAgos(now, accumulateStandardMinute);
    var currentAccumulateKey = accumulateKeyPrefix + now.format(suffixFormatter);
    previousTop30s.forEach(element -> incrementScore(currentAccumulateKey, element, tenMinuteAgos));

    updateViewKey(currentAccumulateKey);
  }

  private Set<TypedTuple<String>> getPreviousAccumulateTop30s(LocalDateTime now, int minute) {
    var previousAccumulateKey = accumulateKeyPrefix + now.minusMinutes(minute).format(suffixFormatter);

    try {
      return zSetOperations.reverseRangeWithScores(previousAccumulateKey, 0, 30);
    } catch (RuntimeException e) {
      log.warn("get previous top 30s fail, key - {}, e - {}", previousAccumulateKey, e.getMessage());
      return Set.of();
    }
  }

  private Map<String, Long> getMinuteAgos(LocalDateTime now, int minute) {
    var minuteAgoKey = minuteKeyPrefix + now.minusMinutes(minute).format(suffixFormatter);

    try {
      return Objects.requireNonNullElse(zSetOperations.rangeWithScores(minuteAgoKey, 0, -1), Set.<TypedTuple<String>>of())
                    .stream()
                    .collect(Collectors.toMap(TypedTuple::getValue, it -> Objects.requireNonNullElse(it.getScore(), 0).longValue()));
    } catch (RuntimeException e) {
      log.warn("get minute agos fail, key - {}, e - {}", minuteAgoKey, e.getMessage());
      return Map.of();
    }
  }

  private void incrementScore(String key, TypedTuple<String> element, Map<String, Long> minuteAgos) {
    try {
      var score = minuteAgos.size() <= 0 ? element.getScore() : element.getScore() - minuteAgos.getOrDefault(element.getValue(), 0L);
      if (score > 0) {
        zSetOperations.incrementScore(key, element.getValue(), score);
      }
    } catch (RuntimeException e) {
      log.error("scheduler increment fail : e - {}", e.getMessage());
    }
  }

  private void updateViewKey(String currentAccumulateKey) {
    try {
      valueOperations.set(viewKey, currentAccumulateKey);
    } catch (RuntimeException e) {
      log.error("update view key fail : key - {}, e - {}", currentAccumulateKey, e.getMessage());
    }
  }

  /**
   * 키워드 스케쥴러 분산락 잠금
   *
   * @param key
   * @return
   */
  public boolean tryLock(String key) {
    var increment = valueOperations.increment(key);
    redisTemplate.expire(key, Duration.ofMinutes(15));
    return increment != null && increment == 1;
  }

  @PostConstruct
  public void post() {
    zSetOperations = redisTemplate.opsForZSet();
    valueOperations = redisTemplate.opsForValue();
  }
}
