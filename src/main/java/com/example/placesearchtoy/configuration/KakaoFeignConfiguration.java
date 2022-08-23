
package com.example.placesearchtoy.configuration;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KakaoFeignConfiguration {

  @Value("${kakao.search-api.app-key}")
  private String appKey;



  @Bean
  public RequestInterceptor kakaoRequestInterceptor() {
    var authorization = "KakaoAK " + appKey;
    return request -> request.header("Authorization", authorization);
  }
}
