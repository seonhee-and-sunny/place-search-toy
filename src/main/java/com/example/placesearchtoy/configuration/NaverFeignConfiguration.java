
package com.example.placesearchtoy.configuration;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NaverFeignConfiguration {

  @Value("${naver.search-api.client.id}")
  private String clientId;

  @Value("${naver.search-api.client.secret}")
  private String clientSecret;

  @Bean
  public RequestInterceptor naverRequestInterceptor() {
    return request -> request.header("X-Naver-Client-Id", clientId)
                             .header("X-Naver-Client-Secret", clientSecret);
  }
}
