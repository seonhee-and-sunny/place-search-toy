
package com.example.placesearchtoy.application.service;

import com.example.placesearchtoy.application.dto.KeywordDto;
import com.example.placesearchtoy.application.model.KeywordHitEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordHitsService {

  private final RedisTemplate<String, String> redisTemplate;

  private ZSetOperations<String, String> zSetOperations;
  private ValueOperations<String, String> valueOperations;

  @Value("${keyword.redis-key-prefix.minute-ranking}")
  private String minuteKeyPrefix;

  @Value("${keyword.redis-key-prefix.accumulate-ranking}")
  private String accumulateKeyPrefix;

  @Value("${keyword.redis-key-prefix.view}")
  private String viewKey;

  private final DateTimeFormatter suffixFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private final Duration defaultTTL = Duration.ofMinutes(15);

  /**
   * 키워드 검색 횟수 증가
   *
   * @param keywordHitEvent
   */
  @Async
  @EventListener
  public void incrementKeywordHits(KeywordHitEvent keywordHitEvent) {
    var suffix = LocalDateTime.now().format(suffixFormatter);
    var keyword = keywordHitEvent.getKeyword();
    if (Strings.isBlank(keyword)) {
      return;
    }

    incrementScoreAtKey(accumulateKeyPrefix + suffix, keyword);
    incrementScoreAtKey(minuteKeyPrefix + suffix, keyword);
  }

  private void incrementScoreAtKey(String key, String keyword) {
    try {
      zSetOperations.incrementScore(key, keyword, 1);
      redisTemplate.expire(key, defaultTTL);
    } catch (RuntimeException e) {
      log.error("redis increment score fail : key - {}, keyword - {}, e - {}", key, keyword, e.getMessage());
    }
  }

  /**
   * 많이 검색된 상위 10개 키워드 목록 조회
   *
   * @return
   */
  public List<KeywordDto> searchTop10KeywordHits() {
    try {
      var keywords = Objects.requireNonNullElse(zSetOperations.reverseRangeWithScores(getCurrentViewKey(), 0, 9), Set.<TypedTuple<String>>of());
      return keywords.stream()
                     .map(keyword -> KeywordDto.of(keyword.getValue(), Objects.requireNonNullElse(keyword.getScore(), 0).longValue()))
                     .toList();
    } catch (RuntimeException e) {
      log.info("search top 10 keyword error, {}", e.getMessage());
      return List.of();
    }
  }

  private String getCurrentViewKey() {
    var accumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    try {
      var currentViewKey = valueOperations.get(viewKey);
      return currentViewKey != null ? currentViewKey : accumulateKey;
    } catch (RuntimeException e) {
      return accumulateKey;
    }
  }

  @PostConstruct
  public void post() {
    zSetOperations = redisTemplate.opsForZSet();
    valueOperations = redisTemplate.opsForValue();
  }
}