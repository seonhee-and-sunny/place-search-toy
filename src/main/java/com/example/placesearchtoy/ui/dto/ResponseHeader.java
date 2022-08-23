
package com.example.placesearchtoy.ui.dto;

import lombok.Getter;

@Getter
public class ResponseHeader {

  private final Boolean isSuccessful;
  private final int code;
  private final String message;

  private ResponseHeader(boolean isSuccessful, int code, String message) {
    this.isSuccessful = isSuccessful;
    this.code = code;
    this.message = message;
  }

  public static ResponseHeader of(boolean isSuccessful, int code, String message) {
    return new ResponseHeader(isSuccessful, code, message);
  }
}
