
package com.example.placesearchtoy.ui.dto;

import lombok.Getter;

@Getter
public class Response {

  private static final ResponseHeader SUCCESS_HEADER = ResponseHeader.of(true, 200, "success");

  private final ResponseHeader header;
  private Object body;

  private Response(ResponseHeader header, Object body) {
    this.header = header;
    this.body = body;
  }

  private Response(ResponseHeader header) {
    this.header = header;
  }

  public static Response ofSuccess(Object body) {
    return new Response(SUCCESS_HEADER, body);
  }

  public static Response ofSuccess() {
    return new Response(SUCCESS_HEADER);
  }

  public static Response ofFail(int resultCode, String resultMessage, Object body) {
    ResponseHeader header = ResponseHeader.of(false, resultCode, resultMessage);
    return new Response(header, body);
  }

  public static Response ofFail(int resultCode, String resultMessage) {
    ResponseHeader header = ResponseHeader.of(false, resultCode, resultMessage);
    return new Response(header);
  }
}
