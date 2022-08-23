
package com.example.placesearchtoy.adapter.client;

import com.example.placesearchtoy.adapter.model.SearchKeywordResponse;
import com.example.placesearchtoy.configuration.KakaoFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kakao", url = "${kakao.search-api.url}", configuration = KakaoFeignConfiguration.class)
public interface KaKaoClient {

  @GetMapping(value = "/v2/local/search/keyword.json")
  SearchKeywordResponse searchKeywordLocal(@RequestParam String query, @RequestParam int size);
}
