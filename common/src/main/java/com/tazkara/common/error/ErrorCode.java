package com.tazkara.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String code();

    HttpStatus status();
}
