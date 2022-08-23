
package com.example.placesearchtoy.ui;

import com.example.placesearchtoy.application.service.KeywordHitsService;
import com.example.placesearchtoy.application.service.SearchQueryService;
import com.example.placesearchtoy.ui.dto.Response;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class SearchController {

  private final SearchQueryService searchQueryService;
  private final KeywordHitsService keywordHitsService;

  @GetMapping("/api/search/places")
  public Response searchPlaces(@RequestParam @NotBlank(message = "검색어가 존재하지 않습니다.") String query) {
    var filteredQuery = searchQueryService.filteredQuery(query);
    return Response.ofSuccess(searchQueryService.searchPlaces(filteredQuery));
  }

  @GetMapping("/api/search/top-keywords")
  public Response searchTop10KeywordHits() {
    return Response.ofSuccess(keywordHitsService.searchTop10KeywordHits());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public Response handleConstraintViolationException(ConstraintViolationException e) {
    return Response.ofFail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  public Response handleRuntimeException(RuntimeException e) {
    log.error("occur runtime exception", e);
    return Response.ofFail(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
  }
}
