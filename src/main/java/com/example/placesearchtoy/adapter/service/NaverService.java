
package com.example.placesearchtoy.adapter.service;

import com.example.placesearchtoy.adapter.client.NaverClient;
import com.example.placesearchtoy.adapter.model.Item;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverService {

  private final NaverClient client;

  public List<Item> searchLocalItems(String query) {
    try {
      var maxSize = 5;
      return client.searchLocals(query, maxSize).getItems();
    } catch (RuntimeException e) {
      log.warn("naver search local service error", e);
      return List.of();
    }
  }
}
