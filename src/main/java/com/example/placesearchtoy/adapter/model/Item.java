
package com.example.placesearchtoy.adapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {

  private String title;

  private String roadAddress;
}
