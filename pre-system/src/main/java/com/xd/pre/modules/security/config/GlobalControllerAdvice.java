package com.xd.pre.modules.security.config;

import cn.hutool.http.HttpStatus;
import com.xd.pre.common.utils.R;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalControllerAdvice {
    private static final String BAD_REQUEST_MSG = "客户端请求参数错误";

    // <1> 处理 form data方式调用接口校验失败抛出的异常
    @ExceptionHandler(BindException.class)
    public R bindExceptionHandler(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> collect = fieldErrors.stream()
                .map(o -> o.getDefaultMessage())
                .collect(Collectors.toList());
        return R.error(HttpStatus.HTTP_INTERNAL_ERROR, BAD_REQUEST_MSG);
    }

    // <2> 处理 json 请求体调用接口校验失败抛出的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R methodArgumentNotValidExceptionHandler(HttpServletResponse httpServletResponse, MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> collect = fieldErrors.stream()
                .map(o -> o.getDefaultMessage())
                .collect(Collectors.toList());
        return R.error(HttpStatus.HTTP_INTERNAL_ERROR, BAD_REQUEST_MSG);
    }

    // <3> 处理单个参数校验失败抛出的异常
    @ExceptionHandler(ConstraintViolationException.class)
    public R constraintViolationExceptionHandler(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        List<String> collect = constraintViolations.stream()
                .map(o -> o.getMessage())
                .collect(Collectors.toList());
        return R.error(HttpStatus.HTTP_INTERNAL_ERROR, BAD_REQUEST_MSG);
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Object handle(Exception e) {
        Map<String, Object> map = new HashMap<>();
        if (e instanceof IllegalArgumentException) {
            IllegalArgumentException myException = (IllegalArgumentException) e;
            map.put("code", 500);
            map.put("msg", myException.getMessage());
            return map;
        } else {
            e.printStackTrace();
            map.put("code", 500);
            map.put("msg", "出错啦");
            return map;
        }
    }

}