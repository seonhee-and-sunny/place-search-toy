
package com.example.placesearchtoy.application.annotation.aspect;

import com.example.placesearchtoy.application.annotation.KeywordHit;
import com.example.placesearchtoy.application.model.KeywordHitEvent;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class KeywordHitAspect {

  private final ApplicationEventPublisher publisher;

  @Before(value = "@annotation(keywordHit) && args(query, ..)")
  public void incrementKeywordHit(KeywordHit keywordHit, String query) {
    var event = KeywordHitEvent.of(query);
    publisher.publishEvent(event);
  }
}
