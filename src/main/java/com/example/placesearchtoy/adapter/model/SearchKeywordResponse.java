
package com.example.placesearchtoy.adapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchKeywordResponse {

  private List<Document> documents;
}
