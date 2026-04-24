package com.bank.admin.common.aspect;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.util.IpUtil;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 操作日志 AOP 切面
 * 拦截所有带 @Log 注解的 Controller 方法，自动采集并记录操作日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** 敏感字段名，序列化参数时会脱敏 */
    private static final String[] SENSITIVE_FIELDS = {"password", "cvv"};

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {

        OperationLog operationLog = new OperationLog();

        // 1. 前置收集基础信息（从注解中取）
        operationLog.setModule(logAnnotation.module());
        operationLog.setAction(logAnnotation.type().getDesc());

        // 2. 收集请求上下文信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setRequestPath(request.getRequestURI());
            operationLog.setRequestMethod(request.getMethod());
        }
        operationLog.setOperator(getCurrentUsername());
        operationLog.setClientIp(IpUtil.getClientIp());
        operationLog.setRequestParams(filterSensitiveParams(joinPoint));

        long startTime = System.currentTimeMillis();

        try {
            // 3. 执行目标方法
            Object result = joinPoint.proceed();

            // 4. 成功：填充结果 & 解析描述
            operationLog.setResult(0);
            operationLog.setDescription(buildDescription(logAnnotation.description(), joinPoint));

            return result;

        } catch (Exception e) {
            // 5. 失败：记录异常信息
            operationLog.setResult(1);
            String errorMsg = e.getMessage();
            operationLog.setErrorMsg(errorMsg != null && errorMsg.length() > 500
                    ? errorMsg.substring(0, 500) : errorMsg);
            operationLog.setDescription(buildDescription(logAnnotation.description(), joinPoint));
            throw e; // 不吞异常，继续抛出让 GlobalExceptionHandler 处理

        } finally {
            // 6. 无论成功失败都记录日志
            operationLog.setDuration(System.currentTimeMillis() - startTime);
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception ex) {
                log.error("保存操作日志失败", ex);
            }
        }
    }

    /**
     * 从 SecurityContext 获取当前登录用户名
     * 对于登录接口，SecurityContext 还没填充，尝试从请求参数中取
     */
    private String getCurrentUsername() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "SYSTEM";
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        // 兜底：从请求参数中提取用户名（适用于登录等尚未认证的接口）
        HttpServletRequest request = attributes.getRequest();
        String username = request.getParameter("username");
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return "ANONYMOUS";
    }

    /**
     * 序列化请求参数并过滤敏感字段
     */
    private String filterSensitiveParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }

            java.util.List<Object> safeArgs = new java.util.ArrayList<>();
            for (Object arg : args) {
                Object sanitized = sanitizeLogArg(arg);
                if (sanitized != null) {
                    safeArgs.add(sanitized);
                }
            }
            return maskSensitiveFields(MAPPER.writeValueAsString(safeArgs));
        } catch (Exception e) {
            log.warn("序列化请求参数失败", e);
            return "[serialization error]";
        }
    }

    private Object sanitizeLogArg(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof MultipartFile file) {
            return buildMultipartFileLog(file);
        }
        if (arg instanceof MultipartFile[] files) {
            java.util.List<Object> fileLogs = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                fileLogs.add(buildMultipartFileLog(file));
            }
            return fileLogs;
        }
        if (arg instanceof jakarta.servlet.http.HttpServletRequest
                || arg instanceof jakarta.servlet.http.HttpServletResponse
                || arg instanceof jakarta.servlet.http.HttpSession) {
            return null;
        }
        if (arg instanceof java.util.Collection<?> collection) {
            java.util.List<Object> items = new java.util.ArrayList<>();
            for (Object item : collection) {
                Object sanitized = sanitizeLogArg(item);
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        if (arg instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> safeMap = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                Object sanitized = sanitizeLogArg(entry.getValue());
                if (sanitized != null) {
                    safeMap.put(String.valueOf(entry.getKey()), sanitized);
                }
            }
            return safeMap;
        }
        if (arg.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(arg);
            java.util.List<Object> items = new java.util.ArrayList<>();
            for (int i = 0; i < length; i++) {
                Object sanitized = sanitizeLogArg(java.lang.reflect.Array.get(arg, i));
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        return arg;
    }

    private java.util.Map<String, Object> buildMultipartFileLog(MultipartFile file) {
        java.util.Map<String, Object> fileInfo = new java.util.LinkedHashMap<>();
        fileInfo.put("fileName", file.getOriginalFilename());
        fileInfo.put("size", file.getSize());
        fileInfo.put("contentType", file.getContentType());
        return fileInfo;
    }

    /**
     * 简单的敏感字段脱敏处理
     */
    private String maskSensitiveFields(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String result = json;
        for (String field : SENSITIVE_FIELDS) {
            // 简单替换：匹配 "field":"xxx" 为 "field":"****"
            result = result.replaceAll(
                    "(\"" + field + "\"\\s*:\\s*\")([^\"]*)(\")",
                    "$1****$3"
            );
        }
        return result;
    }

    /**
     * 构建操作描述，将 [id=#xxx] 等占位符替换为实际值
     * 支持格式：
     *   [id=#id]       → 取名为 id 的 @PathVariable / @RequestParam 参数
     *   [name=#dto.name] → 暂不支持深层嵌套，简化为固定描述 + 参数摘要
     */
    private String buildDescription(String template, ProceedingJoinPoint joinPoint) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        // 如果模板中没有占位符，直接返回
        if (!template.contains("#")) {
            return template;
        }
        // 简化版解析：只处理 #变量名 形式的占位符
        String result = template;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(result);

        // 尝试从方法参数中获取值
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String value = resolveParamValue(paramName, paramNames, args);
            result = result.replace("#" + paramName, value);
        }
        return result;
    }

    /**
     * 通过反射获取方法参数名
     */
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        try {
            java.lang.reflect.Method method = ((org.aspectj.lang.reflect.MethodSignature)
                    joinPoint.getSignature()).getMethod();
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].getName();
            }
            return names;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * 根据参数名查找对应参数值的字符串表示
     */
    private String resolveParamValue(String paramName, String[] paramNames, Object[] args) {
        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && i < args.length) {
                return args[i] != null ? args[i].toString() : "null";
            }
        }
        return "#" + paramName; // 找不到则保留原样
    }
}
