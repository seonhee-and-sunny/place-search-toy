
package com.example.placesearchtoy.application.model.type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ProviderType {
  KAKAO("카카오", 1),
  NAVER("네이버", 2),
  NOT_DEFINED("", Integer.MAX_VALUE);

  private final String name;
  private final int priority;
}
