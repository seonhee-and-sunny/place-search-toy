
package com.example.placesearchtoy.adapter.client;

import com.example.placesearchtoy.adapter.model.SearchLocalResponse;
import com.example.placesearchtoy.configuration.NaverFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "naver", url = "${naver.search-api.url}", configuration = NaverFeignConfiguration.class)
public interface NaverClient {

  @GetMapping(value = "/v1/search/local.json")
  SearchLocalResponse searchLocals(@RequestParam String query, @RequestParam int display);
}
