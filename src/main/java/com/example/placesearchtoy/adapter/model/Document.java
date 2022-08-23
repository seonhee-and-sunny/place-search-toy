
package com.example.placesearchtoy.adapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

  @JsonProperty("place_name")
  private String placeName;

  @JsonProperty("road_address_name")
  private String roadAddress;
}
