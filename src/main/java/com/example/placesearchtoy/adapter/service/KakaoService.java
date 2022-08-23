
package com.example.placesearchtoy.adapter.service;

import com.example.placesearchtoy.adapter.client.KaKaoClient;
import com.example.placesearchtoy.adapter.model.Document;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

  private final KaKaoClient client;

  public List<Document> searchKeywordDocuments(String query, int size) {
    try {
      return size <= 0 ? List.of() : client.searchKeywordLocal(query, size).getDocuments();
    } catch (RuntimeException e) {
      log.warn("kakao search  service error", e);
      return List.of();
    }
  }
}
