
package com.example.placesearchtoy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.placesearchtoy.adapter.model.Document;
import com.example.placesearchtoy.adapter.model.Item;
import com.example.placesearchtoy.adapter.service.KakaoService;
import com.example.placesearchtoy.adapter.service.NaverService;
import com.example.placesearchtoy.application.dto.PlaceDto;
import com.example.placesearchtoy.application.mapper.PlaceMapper;
import com.example.placesearchtoy.application.model.type.ProviderType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SearchQueryServiceTest {

  @Mock
  private KakaoService kakaoService;

  @Mock
  private NaverService naverService;

  @Mock
  private PlaceMapper mapper;

  @InjectMocks
  private SearchQueryService searchQueryService;

  private final int maxSize = 10;

  private final String query = "떡볶이";

  private final String placeName = "동대문엽기떡볶이 동대문본점";
  private final String roadAddress = "서울 중구 다산로 265 럭키프라자";
  private final String nameWithAddress = "동대문엽기떡볶이서울중구다산로265럭키프라자";

  private final String secondPlaceName = "호랑이분식 서현본점";
  private final String secondRoadAddress = "경기 성남시 분당구 서현로 204 엘지에클라트2차 2층 호랑이분식";
  private final String secondNameWithAddress = "호랑이분식경기성남분당구서현로204";

  private final RuntimeException testRuntimeException = new RuntimeException("ERROR");

  @Test
  @DisplayName("필터링된 검색어 조회 - 공백이 포함된 키워드일 경우, 공백 제거하여 반환")
  public void filteredQuery_1() {
    var hasBlankQuery = "  엽 기 떡볶이";

    var actual = searchQueryService.filteredQuery(hasBlankQuery);

    assertFalse(actual.contains(" "));
  }

  @Test
  @DisplayName("필터링된 문자열 조회 - 공백이 미포함된 키워드일 경우, 그대로 반환")
  public void filteredQuery_2() {
    var actual = searchQueryService.filteredQuery(query);

    assertEquals(query, actual);
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 네이버 검색 API에서 오류 발생 시, 카카오 장소 목록 반환")
  public void searchPlaces_1() {
    doThrow(testRuntimeException).when(naverService).searchLocalItems(query);

    var mockDocuments = List.of(mock(Document.class));
    when(kakaoService.searchKeywordDocuments(query, maxSize)).thenReturn(mockDocuments);

    PlaceDto mockPlace = mock(PlaceDto.class);
    doNothing().when(mockPlace).updateMatchCount(1);
    when(mapper.documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO)).thenReturn(List.of(mockPlace));

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(1, actuals.size());

    var actual = actuals.get(0);
    assertEquals(mockPlace, actual);

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(0)).itemsToPlaceDtos(any(), any());
    verify(kakaoService, times(1)).searchKeywordDocuments(query, maxSize);
    verify(mapper, times(1)).documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO);
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 카카오 검색 API에서 오류 발생 시, 네이버 장소 목록 반환")
  public void searchPlaces_2() {
    var mockItems = List.of(mock(Item.class));
    when(naverService.searchLocalItems(query)).thenReturn(mockItems);

    doThrow(testRuntimeException).when(kakaoService).searchKeywordDocuments(query, 9);

    var mockPlace = mock(PlaceDto.class);
    doNothing().when(mockPlace).updateMatchCount(1);
    when(mapper.itemsToPlaceDtos(mockItems, ProviderType.NAVER)).thenReturn(List.of(mockPlace));

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(1, actuals.size());

    var actual = actuals.get(0);
    assertEquals(mockPlace, actual);

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(1)).itemsToPlaceDtos(mockItems, ProviderType.NAVER);
    verify(kakaoService, times(1)).searchKeywordDocuments(query, 9);
    verify(mapper, times(0)).documentsToPlaceDtos(any(), any());
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 카카오/네이버 검색 API 모두 오류 발생 시, 빈 장소 목록 반환")
  public void searchPlaces_3() {
    doThrow(testRuntimeException).when(naverService).searchLocalItems(query);
    doThrow(testRuntimeException).when(kakaoService).searchKeywordDocuments(query, maxSize);

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(0, actuals.size());

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(0)).itemsToPlaceDtos(any(), any());
    verify(kakaoService, times(1)).searchKeywordDocuments(query, maxSize);
    verify(mapper, times(0)).documentsToPlaceDtos(any(), any());
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 카카오/네이버 모두 검색된 장소가 존재하지 않을 경우, 카카오-네이버 순으로 정렬된 목록 반환")
  public void searchPlaces_4() {
    var mockItems = List.of(mock(Item.class));
    when(naverService.searchLocalItems(query)).thenReturn(mockItems);

    var mockNaverPlace = firstMockPlace(ProviderType.NAVER);
    doNothing().when(mockNaverPlace).updateMatchCount(1);
    when(mapper.itemsToPlaceDtos(mockItems, ProviderType.NAVER)).thenReturn(List.of(mockNaverPlace));

    var mockDocuments = List.of(mock(Document.class));
    when(kakaoService.searchKeywordDocuments(query, 9)).thenReturn(mockDocuments);

    var mockKakaoPlace = secondMockPlace(ProviderType.KAKAO);
    doNothing().when(mockKakaoPlace).updateMatchCount(1);
    when(mapper.documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO)).thenReturn(List.of(mockKakaoPlace));

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(2, actuals.size());

    var kakaoActual = actuals.get(0);
    assertEquals(mockKakaoPlace.getName(), kakaoActual.getName());
    assertEquals(mockKakaoPlace.getRoadAddress(), kakaoActual.getRoadAddress());
    assertEquals(ProviderType.KAKAO, kakaoActual.getProviderType());

    var naverActual = actuals.get(1);
    assertEquals(mockNaverPlace.getName(), naverActual.getName());
    assertEquals(mockNaverPlace.getRoadAddress(), naverActual.getRoadAddress());
    assertEquals(ProviderType.NAVER, naverActual.getProviderType());

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(1)).itemsToPlaceDtos(mockItems, ProviderType.NAVER);
    verify(kakaoService, times(1)).searchKeywordDocuments(query, 9);
    verify(mapper, times(1)).documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO);
  }

  private PlaceDto firstMockPlace(ProviderType providerType) {
    var mockPlace = mock(PlaceDto.class);
    when(mockPlace.getName()).thenReturn(placeName);
    when(mockPlace.getRoadAddress()).thenReturn(roadAddress);
    when(mockPlace.getNameWithAddress()).thenReturn(nameWithAddress);
    when(mockPlace.getProviderType()).thenReturn(providerType);

    return mockPlace;
  }

  private PlaceDto secondMockPlace(ProviderType providerType) {
    var mockPlace = mock(PlaceDto.class);
    when(mockPlace.getName()).thenReturn(secondPlaceName);
    when(mockPlace.getRoadAddress()).thenReturn(secondRoadAddress);
    when(mockPlace.getNameWithAddress()).thenReturn(secondNameWithAddress);
    when(mockPlace.getProviderType()).thenReturn(providerType);

    return mockPlace;
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 카카오/네이버 모두 검색된 장소가 존재하는 경우, 모두 검색된 장소가 우선 정렬된 목록 반환")
  public void searchPlaces_5() {
    var mockItems = List.of(mock(Item.class), mock(Item.class));
    when(naverService.searchLocalItems(query)).thenReturn(mockItems);

    var oneMatchedPlace = firstMockPlace(ProviderType.NAVER);
    doNothing().when(oneMatchedPlace).updateMatchCount(1);
    when(oneMatchedPlace.getMatchCount()).thenReturn(1);
    var twoMatchedPlace = secondMockPlace(ProviderType.NAVER);
    doNothing().when(twoMatchedPlace).updateMatchCount(2);
    when(twoMatchedPlace.getMatchCount()).thenReturn(2);

    when(mapper.itemsToPlaceDtos(mockItems, ProviderType.NAVER)).thenReturn(List.of(oneMatchedPlace, twoMatchedPlace));

    var mockDocuments = List.of(mock(Document.class));
    when(kakaoService.searchKeywordDocuments(query, 8)).thenReturn(mockDocuments);

    twoMatchedPlace.setProviderType(ProviderType.KAKAO);
    when(mapper.documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO)).thenReturn(List.of(twoMatchedPlace));

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(2, actuals.size());

    var twoMatched = actuals.get(0);
    assertEquals(twoMatchedPlace.getName(), twoMatched.getName());
    assertEquals(twoMatchedPlace.getRoadAddress(), twoMatched.getRoadAddress());

    var oneMatched = actuals.get(1);
    assertEquals(oneMatchedPlace.getName(), oneMatched.getName());
    assertEquals(oneMatchedPlace.getRoadAddress(), oneMatched.getRoadAddress());

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(1)).itemsToPlaceDtos(mockItems, ProviderType.NAVER);
    verify(kakaoService, times(1)).searchKeywordDocuments(query, 8);
    verify(mapper, times(1)).documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO);
  }

  @Test
  @DisplayName("검색어와 관련된 장소 목록 조회 - 카카오/네이버 검색 결과에 장소가 있는지 판단할 때 오류 발생 시, 에러 로깅 후 로직 반복 및 목록 반환")
  public void searchPlaces_6() {
    when(naverService.searchLocalItems(query)).thenReturn(List.of());
    when(mapper.itemsToPlaceDtos(List.of(), ProviderType.NAVER)).thenReturn(List.of());

    var mockDocuments = List.of(mock(Document.class));
    when(kakaoService.searchKeywordDocuments(query, maxSize)).thenReturn(mockDocuments);

    var mockPlace = mock(PlaceDto.class);
    when(mockPlace.getName()).thenReturn(secondPlaceName);
    when(mockPlace.getRoadAddress()).thenReturn(secondRoadAddress);
    doThrow(testRuntimeException).when(mockPlace).getNameWithAddress();
    when(mapper.documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO)).thenReturn(List.of(mockPlace));

    var actuals = searchQueryService.searchPlaces(query);

    assertEquals(1, actuals.size());

    var actual = actuals.get(0);
    assertEquals(mockPlace.getName(), actual.getName());
    assertEquals(mockPlace.getRoadAddress(), actual.getRoadAddress());

    verify(naverService, times(1)).searchLocalItems(query);
    verify(mapper, times(1)).itemsToPlaceDtos(List.of(), ProviderType.NAVER);
    verify(kakaoService, times(1)).searchKeywordDocuments(query, maxSize);
    verify(mapper, times(1)).documentsToPlaceDtos(mockDocuments, ProviderType.KAKAO);

    verify(mockPlace, times(2)).getNameWithAddress();
  }
}
