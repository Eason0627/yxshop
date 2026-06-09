package com.yxshop.Config;

import com.yxshop.Utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception ex) {
        log.error(ex.toString());
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        Result result = new Result();
        if (msg.contains("Bad Request")) {
            result.setCode(400);
            result.setMsg("Bad Request");
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } else if (msg.contains("Unauthorized") || msg.contains("JWT expired")) {
            result.setCode(401);
            result.setMsg("Unauthorized");
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
        } else if (msg.contains("Forbidden")) {
            result.setCode(403);
            result.setMsg("Forbidden");
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        } else if (msg.contains("Not Found")) {
            result.setCode(404);
            result.setMsg("Not Found");
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else if (msg.contains("密码") || msg.contains("用户名") || msg.contains("登录")) {
            result.setCode(400);
            result.setMsg(msg);
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } else {
            result.setCode(500);
            result.setMsg(!msg.isEmpty() ? msg : "Internal Server Error");
            result.setData(null);
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
