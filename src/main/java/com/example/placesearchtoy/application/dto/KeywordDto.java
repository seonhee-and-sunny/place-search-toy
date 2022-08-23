
package com.example.placesearchtoy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class KeywordDto {

  private String title;

  private Long hits;
}
