
package com.example.placesearchtoy.application.service;

import com.example.placesearchtoy.adapter.service.KakaoService;
import com.example.placesearchtoy.adapter.service.NaverService;
import com.example.placesearchtoy.application.annotation.KeywordHit;
import com.example.placesearchtoy.application.dto.PlaceDto;
import com.example.placesearchtoy.application.mapper.PlaceMapper;
import com.example.placesearchtoy.application.model.type.ProviderType;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchQueryService {

  private final KakaoService kakaoService;
  private final NaverService naverService;

  private final PlaceMapper mapper;

  private final int maxSize = 10;

  /**
   * 필터링된 검색어 조회
   *
   * @param query
   * @return
   */
  public String filteredQuery(String query) {
    return query.replaceAll("\\s", "");
  }

  /**
   * 검색어와 관련된 장소 목록 조회
   *
   * @param query
   * @return
   */
  @KeywordHit
  public List<PlaceDto> searchPlaces(String query) {
    List<PlaceDto> naverPlaces = getNaverPlaces(query);
    List<PlaceDto> kakaoPlaces = getKakaoPlaces(query, Math.max(maxSize - naverPlaces.size(), 0));

    List<PlaceDto> places = Stream.of(naverPlaces, kakaoPlaces).flatMap(Collection::stream).toList();

    updateKeywordMatchCount(places);

    return getSortedPlaces(places);
  }

  private List<PlaceDto> getNaverPlaces(String query) {
    try {
      return mapper.itemsToPlaceDtos(naverService.searchLocalItems(query), ProviderType.NAVER);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private List<PlaceDto> getKakaoPlaces(String query, int size) {
    try {
      return mapper.documentsToPlaceDtos(kakaoService.searchKeywordDocuments(query, size), ProviderType.KAKAO);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private void updateKeywordMatchCount(List<PlaceDto> places) {
    Map<String, Integer> matchCounts = new HashMap<>();
    places.forEach(place -> {
      Consumer<PlaceDto> consumer = placeDto -> {
        matchCounts.putIfAbsent(placeDto.getNameWithAddress(), 0);
        matchCounts.computeIfPresent(placeDto.getNameWithAddress(), (key, value) -> value + 1);
      };
      handleExceptionWithWarnLogging(consumer, place, "create keyword match count map fail");
    });

    places.forEach(place -> {
      Consumer<PlaceDto> consumer = placeDto -> placeDto.updateMatchCount(matchCounts.get(placeDto.getNameWithAddress()));
      handleExceptionWithWarnLogging(consumer, place, "update keyword match count fail");
    });
  }

  private List<PlaceDto> getSortedPlaces(List<PlaceDto> places) {
    return places.stream()
                 .sorted(Comparator.comparing(place -> place.getProviderType().getPriority()))
                 .sorted(Comparator.comparing(PlaceDto::getMatchCount).reversed())
                 .distinct()
                 .toList();
  }

  private void handleExceptionWithWarnLogging(Consumer<PlaceDto> consumer, PlaceDto placeDto, String errorMessage) {
    try {
      consumer.accept(placeDto);
    } catch (Exception e) {
      log.warn(errorMessage + ": place - {}, e - {}", placeDto.toString(), e.getMessage());
    }
  }
}
