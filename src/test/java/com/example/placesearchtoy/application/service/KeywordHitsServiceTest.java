
package com.example.placesearchtoy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.placesearchtoy.application.model.KeywordHitEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class KeywordHitsServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private KeywordHitsService keywordHitsService;

  private final KeywordHitEvent keywordHitEvent = KeywordHitEvent.of("떡볶이");

  private final String minuteKeyPrefix = "ranking:minute:";
  private final String accumulateKeyPrefix = "ranking:accumulate:";
  private final String viewKey = "ranking:view";

  private final String testViewKeyValue = "ranking:accumulate:schedule-complete";

  private final DateTimeFormatter suffixFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private final Duration defaultTTL = Duration.ofMinutes(15);

  private final RuntimeException testRuntimeException = new RuntimeException("ERROR");

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(keywordHitsService, "minuteKeyPrefix", minuteKeyPrefix);
    ReflectionTestUtils.setField(keywordHitsService, "accumulateKeyPrefix", accumulateKeyPrefix);
    ReflectionTestUtils.setField(keywordHitsService, "viewKey", viewKey);

    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    keywordHitsService.post();
  }

  @Test
  @DisplayName("키워드 검색 횟수 증가 - 키워드가 공백일 경우, 종료")
  public void incrementKeywordHits_1() {
    KeywordHitEvent emptyKeyword = KeywordHitEvent.of("   ");

    keywordHitsService.incrementKeywordHits(emptyKeyword);

    verify(zSetOperations, times(0)).incrementScore(anyString(), anyString(), anyDouble());
    verify(redisTemplate, times(0)).expire(anyString(), any());
  }

  @Test
  @DisplayName("키워드 검색 횟수 증가 - 정상 키워드일 경우, 레디스 내 키워드 스코어 증가")
  public void incrementKeywordHits_2() {
    var suffix = LocalDateTime.now().format(suffixFormatter);

    var accumulateKey = accumulateKeyPrefix + suffix;
    when(zSetOperations.incrementScore(accumulateKey, keywordHitEvent.getKeyword(), 1)).thenReturn(1D);
    when(redisTemplate.expire(accumulateKey, defaultTTL)).thenReturn(true);

    var minuteKey = minuteKeyPrefix + suffix;
    when(zSetOperations.incrementScore(minuteKey, keywordHitEvent.getKeyword(), 1)).thenReturn(1D);
    when(redisTemplate.expire(minuteKey, defaultTTL)).thenReturn(true);

    keywordHitsService.incrementKeywordHits(keywordHitEvent);

    verify(zSetOperations, times(1)).incrementScore(accumulateKey, keywordHitEvent.getKeyword(), 1);
    verify(zSetOperations, times(1)).incrementScore(minuteKey, keywordHitEvent.getKeyword(), 1);
    verify(redisTemplate, times(1)).expire(accumulateKey, defaultTTL);
    verify(redisTemplate, times(1)).expire(minuteKey, defaultTTL);
  }

  @Test
  @DisplayName("키워드 검색 횟수 증가 - 키워드 스코어 증가 시에 오류 발생할 경우, 에러 로깅")
  public void incrementKeywordHits_3() {
    var suffix = LocalDateTime.now().format(suffixFormatter);

    var accumulateKey = accumulateKeyPrefix + suffix;
    doThrow(testRuntimeException).when(zSetOperations).incrementScore(accumulateKey, keywordHitEvent.getKeyword(), 1);

    var minuteKey = minuteKeyPrefix + suffix;
    when(zSetOperations.incrementScore(minuteKey, keywordHitEvent.getKeyword(), 1)).thenReturn(1D);
    when(redisTemplate.expire(minuteKey, defaultTTL)).thenReturn(true);

    keywordHitsService.incrementKeywordHits(keywordHitEvent);

    verify(zSetOperations, times(1)).incrementScore(accumulateKey, keywordHitEvent.getKeyword(), 1);
    verify(zSetOperations, times(1)).incrementScore(minuteKey, keywordHitEvent.getKeyword(), 1);
    verify(redisTemplate, times(0)).expire(accumulateKey, defaultTTL);
    verify(redisTemplate, times(1)).expire(minuteKey, defaultTTL);
  }

  @Test
  @DisplayName("많이 검색된 상위 10개 키워드 목록 조회 - 조회 키 획득에서 오류 발생 시, 실시간 누적 키로 직접 조회")
  public void searchTop10KeywordHits_1() {
    doThrow(testRuntimeException).when(valueOperations).get(viewKey);

    var accumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    when(zSetOperations.reverseRangeWithScores(accumulateKey, 0, 9)).thenReturn(mock(Set.class));

    keywordHitsService.searchTop10KeywordHits();

    verify(valueOperations, times(1)).get(viewKey);
    verify(zSetOperations, times(1)).reverseRangeWithScores(accumulateKey, 0, 9);
  }

  @Test
  @DisplayName("많이 검색된 상위 10개 키워드 목록 조회 - 상위 목록 조회에서 오류 발생 시, 로깅 후 빈 목록 반환")
  public void searchTop10KeywordHits_2() {
    when(valueOperations.get(viewKey)).thenReturn(testViewKeyValue);
    when(zSetOperations.reverseRangeWithScores(testViewKeyValue, 0, 9)).thenThrow(testRuntimeException);

    var actuals = keywordHitsService.searchTop10KeywordHits();

    assertEquals(List.of(), actuals);

    verify(valueOperations, times(1)).get(viewKey);
    verify(zSetOperations, times(1)).reverseRangeWithScores(testViewKeyValue, 0, 9);
  }

  @Test
  @DisplayName("많이 검색된 상위 10개 키워드 목록 조회 - 화면 노출을 위한 DTO 변환에서 오류 발생 시, 로깅 후 빈 목록 반환")
  public void searchTop10KeywordHits_3() {
    when(valueOperations.get(viewKey)).thenReturn(testViewKeyValue);

    var mockTuple = mock(TypedTuple.class);
    when(zSetOperations.reverseRangeWithScores(testViewKeyValue, 0, 9)).thenReturn(Set.of(mockTuple));

    when(mockTuple.getValue()).thenThrow(testRuntimeException);

    var actuals = keywordHitsService.searchTop10KeywordHits();

    assertEquals(List.of(), actuals);

    verify(valueOperations, times(1)).get(viewKey);
    verify(zSetOperations, times(1)).reverseRangeWithScores(testViewKeyValue, 0, 9);
  }

  @Test
  @DisplayName("많이 검색된 상위 10개 키워드 목록 조회 - 조회 정상 완료 시, 키워드 DTO 목록 반환")
  public void searchTop10KeywordHits_4() {
    when(valueOperations.get(viewKey)).thenReturn(testViewKeyValue);

    var mockTuple = mock(TypedTuple.class);
    when(zSetOperations.reverseRangeWithScores(testViewKeyValue, 0, 9)).thenReturn(Set.of(mockTuple));

    when(mockTuple.getValue()).thenReturn("떡볶이");
    when(mockTuple.getScore()).thenReturn(4D);

    var actuals = keywordHitsService.searchTop10KeywordHits();

    assertEquals(1, actuals.size());

    var actual = actuals.get(0);
    assertEquals(mockTuple.getValue(), actual.getTitle());
    assertEquals(mockTuple.getScore().longValue(), actual.getHits());

    verify(valueOperations, times(1)).get(viewKey);
    verify(zSetOperations, times(1)).reverseRangeWithScores(testViewKeyValue, 0, 9);
  }
}
