
package com.example.placesearchtoy.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class KeywordSchedulerTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private KeywordScheduler keywordScheduler;

  private final String minuteKeyPrefix = "ranking:minute:";
  private final String accumulateKeyPrefix = "ranking:accumulate:";
  private final String viewKey = "ranking:view";
  private final String distributedLockKey = "ranking:distributed-lock:";

  private final int accumulateStandardMinute = 10;
  private final Duration defaultTTL = Duration.ofMinutes(15);
  private final DateTimeFormatter suffixFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final String keyword = "떡볶이";
  private final Double score = 4D;
  private final Double updateScore = 2D;

  private final String secondKeyword = "감자빵";
  private final Double secondScore = 2D;

  private final RuntimeException testRuntimeException = new RuntimeException("ERROR");

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(keywordScheduler, "minuteKeyPrefix", minuteKeyPrefix);
    ReflectionTestUtils.setField(keywordScheduler, "accumulateKeyPrefix", accumulateKeyPrefix);
    ReflectionTestUtils.setField(keywordScheduler, "viewKey", viewKey);
    ReflectionTestUtils.setField(keywordScheduler, "distributedLockKey", distributedLockKey);

    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    keywordScheduler.post();
  }

  @Test
  @DisplayName("누적 키워드 조회 정보 저장 - 1분 전 누적 정보 조회 오류 발생 시, 로깅 후 종료")
  public void createAccumulateRanking_1() {
    tryLock_success();

    var key = accumulateKeyPrefix + LocalDateTime.now().minusMinutes(1).format(suffixFormatter);
    when(zSetOperations.reverseRangeWithScores(key, 0, 30)).thenThrow(testRuntimeException);

    keywordScheduler.createAccumulateRanking();

    verify(zSetOperations, times(1)).reverseRangeWithScores(key, 0, 30);
    verify(zSetOperations, times(0)).rangeWithScores(anyString(), eq(0), eq(-1));
  }

  @Test
  @DisplayName("누적 키워드 조회 정보 저장 - 10분 전 분간 정보 조회 오류 발생 시, 지난 1분간 정보로만 누적 정보 업데이트")
  public void createAccumulateRanking_2() {
    tryLock_success();

    var previousAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().minusMinutes(1).format(suffixFormatter);

    TypedTuple<String> mockTuple = firstTuple();
    when(zSetOperations.reverseRangeWithScores(previousAccumulateKey, 0, 30)).thenReturn(Set.of(mockTuple));

    var minuteAgoKey = minuteKeyPrefix + LocalDateTime.now().minusMinutes(accumulateStandardMinute).format(suffixFormatter);
    when(zSetOperations.rangeWithScores(minuteAgoKey, 0, -1)).thenThrow(testRuntimeException);

    var currentAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    when(zSetOperations.incrementScore(currentAccumulateKey, mockTuple.getValue(), mockTuple.getScore())).thenReturn(score + updateScore);

    doNothing().when(valueOperations).set(viewKey, currentAccumulateKey);

    keywordScheduler.createAccumulateRanking();

    verify(zSetOperations, times(1)).reverseRangeWithScores(previousAccumulateKey, 0, 30);
    verify(zSetOperations, times(1)).rangeWithScores(minuteAgoKey, 0, -1);
    verify(zSetOperations, times(1)).incrementScore(currentAccumulateKey, keyword, updateScore);
    verify(valueOperations, times(1)).set(viewKey, currentAccumulateKey);
  }

  private TypedTuple<String> firstTuple() {
    TypedTuple<String> mockTuple = mock(TypedTuple.class);
    when(mockTuple.getValue()).thenReturn(keyword);
    when(mockTuple.getScore()).thenReturn(updateScore);

    return mockTuple;
  }

  @Test
  @DisplayName("누적 키워드 조회 정보 저장 - 누적 정보 업데이트에서 오류 발생 시, 에러 로깅 후 다음 정보 업데이트 진행")
  public void createAccumulateRanking_3() {
    tryLock_success();

    var previousAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().minusMinutes(1).format(suffixFormatter);

    TypedTuple<String> mockTuple = firstTuple();
    TypedTuple<String> mockTuple2 = secondTuple();
    when(zSetOperations.reverseRangeWithScores(previousAccumulateKey, 0, 30)).thenReturn(Set.of(mockTuple, mockTuple2));

    var minuteAgoKey = minuteKeyPrefix + LocalDateTime.now().minusMinutes(accumulateStandardMinute).format(suffixFormatter);
    when(zSetOperations.rangeWithScores(minuteAgoKey, 0, -1)).thenReturn(Set.of());

    var currentAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    when(zSetOperations.incrementScore(currentAccumulateKey, mockTuple.getValue(), mockTuple.getScore())).thenThrow(testRuntimeException);
    when(zSetOperations.incrementScore(currentAccumulateKey, mockTuple2.getValue(), mockTuple2.getScore())).thenReturn(2D);

    doNothing().when(valueOperations).set(viewKey, currentAccumulateKey);

    keywordScheduler.createAccumulateRanking();

    verify(zSetOperations, times(1)).reverseRangeWithScores(previousAccumulateKey, 0, 30);
    verify(zSetOperations, times(1)).rangeWithScores(minuteAgoKey, 0, -1);
    verify(zSetOperations, times(1)).incrementScore(currentAccumulateKey, keyword, updateScore);
    verify(valueOperations, times(1)).set(viewKey, currentAccumulateKey);

    verify(mockTuple, times(2)).getValue();
    verify(mockTuple2, times(2)).getScore();
  }

  private TypedTuple<String> secondTuple() {
    TypedTuple<String> mockTuple2 = mock(TypedTuple.class);
    when(mockTuple2.getValue()).thenReturn(secondKeyword);
    when(mockTuple2.getScore()).thenReturn(secondScore);

    return mockTuple2;
  }

  @Test
  @DisplayName("누적 키워드 조회 정보 저장 - 조회 키 업데이트 실패 시, 에러 로깅 후 종료")
  public void createAccumulateRanking_4() {
    tryLock_success();

    var previousAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().minusMinutes(1).format(suffixFormatter);

    TypedTuple<String> mockTuple = firstTuple();
    when(zSetOperations.reverseRangeWithScores(previousAccumulateKey, 0, 30)).thenReturn(Set.of(mockTuple));

    var minuteAgoKey = minuteKeyPrefix + LocalDateTime.now().minusMinutes(accumulateStandardMinute).format(suffixFormatter);
    when(zSetOperations.rangeWithScores(minuteAgoKey, 0, -1)).thenReturn(Set.of());

    var currentAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    when(zSetOperations.incrementScore(currentAccumulateKey, mockTuple.getValue(), mockTuple.getScore())).thenReturn(score + updateScore);

    doThrow(testRuntimeException).when(valueOperations).set(viewKey, currentAccumulateKey);

    keywordScheduler.createAccumulateRanking();

    verify(zSetOperations, times(1)).reverseRangeWithScores(previousAccumulateKey, 0, 30);
    verify(zSetOperations, times(1)).rangeWithScores(minuteAgoKey, 0, -1);
    verify(zSetOperations, times(1)).incrementScore(currentAccumulateKey, keyword, updateScore);
    verify(valueOperations, times(1)).set(viewKey, currentAccumulateKey);
  }

  @Test
  @DisplayName("누적 키워드 조회 정보 저장 - 신규 누적 정보 생성")
  public void createAccumulateRanking_5() {
    tryLock_success();

    var previousAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().minusMinutes(1).format(suffixFormatter);

    TypedTuple<String> mockTuple = firstTuple();
    when(zSetOperations.reverseRangeWithScores(previousAccumulateKey, 0, 30)).thenReturn(Set.of(mockTuple));

    var minuteAgoKey = minuteKeyPrefix + LocalDateTime.now().minusMinutes(accumulateStandardMinute).format(suffixFormatter);
    when(zSetOperations.rangeWithScores(minuteAgoKey, 0, -1)).thenReturn(Set.of());

    var currentAccumulateKey = accumulateKeyPrefix + LocalDateTime.now().format(suffixFormatter);
    when(zSetOperations.incrementScore(currentAccumulateKey, mockTuple.getValue(), mockTuple.getScore())).thenReturn(score + updateScore);

    doNothing().when(valueOperations).set(viewKey, currentAccumulateKey);

    keywordScheduler.createAccumulateRanking();

    verify(zSetOperations, times(1)).reverseRangeWithScores(previousAccumulateKey, 0, 30);
    verify(zSetOperations, times(1)).rangeWithScores(minuteAgoKey, 0, -1);
    verify(zSetOperations, times(1)).incrementScore(currentAccumulateKey, keyword, updateScore);
    verify(valueOperations, times(1)).set(viewKey, currentAccumulateKey);
  }

  private void tryLock_success() {
    var key = distributedLockKey + LocalDateTime.now().format(suffixFormatter);
    when(valueOperations.increment(key)).thenReturn(1L);
    when(redisTemplate.expire(key, defaultTTL)).thenReturn(true);
  }

  @Test
  @DisplayName("키워드 스케쥴러 분산락 잠금 - 분산락 키 조회 오류 발생 시, Exception 발생")
  public void tryLock_1() {
    var key = distributedLockKey + LocalDateTime.now().format(suffixFormatter);
    when(valueOperations.increment(key)).thenThrow(testRuntimeException);

    RuntimeException e = assertThrows(RuntimeException.class, () -> keywordScheduler.tryLock(key));
    assertEquals(testRuntimeException, e);

    verify(valueOperations, times(1)).increment(key);
    verify(redisTemplate, times(0)).expire(key, defaultTTL);
  }

  @Test
  @DisplayName("키워드 스케쥴러 분산락 잠금 - 분산락 키 만료 등록 오류 발생 시, Exception 발생")
  public void tryLock_2() {
    var key = distributedLockKey + LocalDateTime.now().format(suffixFormatter);
    when(valueOperations.increment(key)).thenReturn(1L);
    when(redisTemplate.expire(key, defaultTTL)).thenThrow(testRuntimeException);

    RuntimeException e = assertThrows(RuntimeException.class, () -> keywordScheduler.tryLock(key));
    assertEquals(testRuntimeException, e);

    verify(valueOperations, times(1)).increment(key);
    verify(redisTemplate, times(1)).expire(key, defaultTTL);
  }

  @Test
  @DisplayName("키워드 스케쥴러 분산락 잠금 - 잠금 실패 시, false 반환")
  public void tryLock_3() {
    var key = distributedLockKey + LocalDateTime.now().format(suffixFormatter);
    when(valueOperations.increment(key)).thenReturn(null);
    when(redisTemplate.expire(key, defaultTTL)).thenReturn(true);

    var actual = keywordScheduler.tryLock(key);
    assertFalse(actual);

    verify(valueOperations, times(1)).increment(key);
    verify(redisTemplate, times(1)).expire(key, defaultTTL);
  }

  @Test
  @DisplayName("키워드 스케쥴러 분산락 잠금 - 잠금 성공 시, true 반환")
  public void tryLock_4() {
    var key = distributedLockKey + LocalDateTime.now().format(suffixFormatter);
    when(valueOperations.increment(key)).thenReturn(1L);
    when(redisTemplate.expire(key, defaultTTL)).thenReturn(true);

    var actual = keywordScheduler.tryLock(key);
    assertTrue(actual);

    verify(valueOperations, times(1)).increment(key);
    verify(redisTemplate, times(1)).expire(key, defaultTTL);
  }
}
