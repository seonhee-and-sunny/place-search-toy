
package com.example.placesearchtoy.application.mapper;

import com.example.placesearchtoy.adapter.model.Document;
import com.example.placesearchtoy.adapter.model.Item;
import com.example.placesearchtoy.application.dto.PlaceDto;
import com.example.placesearchtoy.application.model.type.ProviderType;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface PlaceMapper {

  @Mappings({
      @Mapping(target = "name", source = "document.placeName")
  })
  PlaceDto toPlaceDto(Document document, ProviderType providerType);

  default List<PlaceDto> documentsToPlaceDtos(List<Document> documents, ProviderType providerType) {
    return documents.stream().map(document -> toPlaceDto(document, providerType)).toList();
  }

  @Mappings({
      @Mapping(target = "name", source = "item.title")
  })
  PlaceDto toPlaceDto(Item item, ProviderType providerType);

  default List<PlaceDto> itemsToPlaceDtos(List<Item> items, ProviderType providerType) {
    return items.stream().map(item -> toPlaceDto(item, providerType)).toList();
  }
}
