
package com.example.placesearchtoy.adapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchLocalResponse {

  private List<Item> items;
}
