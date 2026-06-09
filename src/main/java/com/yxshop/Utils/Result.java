package com.yxshop.Utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : hym
 * @date : 2024/7/13 16:48
 * @Version: 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public Result(Integer code, String msg) {
    }

    public static Result success() {
        return new Result(200, "success", null);
    }

    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success(String msg) {
        return new Result(200, msg, null);
    }

    public static Result success(Integer code, String msg) {
        return new Result(code, msg);
    }

    public static Result success(String msg, Object data) {
        return new Result(200, msg, data);
    }

    public static Result error(String msg) {
        return new Result(0, msg, null);
    }

}
