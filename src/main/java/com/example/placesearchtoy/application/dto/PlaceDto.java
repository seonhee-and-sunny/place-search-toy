
package com.example.placesearchtoy.application.dto;

import com.example.placesearchtoy.application.model.type.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlaceDto {

  private String name;

  private String roadAddress;

  @JsonIgnore
  private int matchCount;
  @JsonIgnore
  private ProviderType providerType;

  @JsonIgnore
  public String getNameWithAddress() {
    final String filterBracket = "\\(.*\\)|\\[.*]|<.*>";
    final String filterSuffix = "특별시|광역시|특별자치도|도|시|지하";

    var filteredName = name.split(" ")[0].replaceAll(filterBracket, "");
    var filteredAddress = roadAddress.replaceAll(filterSuffix, "");

    var concatKey = filteredName + " " + filteredAddress;
    String[] splitKeys = concatKey.split("\\s+");
    String[] keys = splitKeys.length > 5 ? Arrays.copyOfRange(splitKeys, 0, 5) : splitKeys;

    return Arrays.stream(keys).filter(address -> !hasExceptWord(address)).collect(Collectors.joining());
  }

  private boolean hasExceptWord(String roadAddress) {
    final String[] exceptWordInRoadAddress = {"빌딩", "층", "호"};
    for (String wordInRoadAddress : exceptWordInRoadAddress) {
      if (roadAddress.contains(wordInRoadAddress)) {
        return true;
      }
    }
    return false;
  }

  public void updateMatchCount(int matchCount) {
    this.matchCount = matchCount;
  }

  public ProviderType getProviderType() {
    return providerType != null ? providerType : ProviderType.NOT_DEFINED;
  }

  public void setName(String name) {
    this.name = name.replaceAll("<b>|</b>", "");
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof PlaceDto) {
      return getNameWithAddress().equals(((PlaceDto) object).getNameWithAddress());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getNameWithAddress().hashCode();
  }
}
