package com.bank.admin.common.aspect;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.util.IpUtil;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String[] SENSITIVE_FIELDS = {
            "password", "cvv",
            "phone", "mobile", "tel", "telephone",
            "idCard", "id_card", "identityNo", "identity_no", "certNo", "cert_no",
            "cardNo", "card_no", "cardNumber", "card_number", "bankCardNo", "bank_card_no", "cardNoLast4", "card_no_last4",
            "token", "accessToken", "access_token", "refreshToken", "refresh_token", "authorization",
            "secret", "secretId", "secret_id", "secretKey", "secret_key", "apiKey", "api_key", "key"
    };

    private final OperationLogService operationLogService;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        OperationLog operationLog = new OperationLog();

        operationLog.setModule(logAnnotation.module());
        operationLog.setAction(logAnnotation.type().getDesc());

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setRequestPath(request.getRequestURI());
            operationLog.setRequestMethod(request.getMethod());
        }
        operationLog.setOperator(getCurrentUsername(joinPoint));
        operationLog.setClientIp(IpUtil.getClientIp());
        operationLog.setRequestParams(filterSensitiveParams(joinPoint));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            operationLog.setResult(0);
            operationLog.setDescription(buildDescription(logAnnotation.description(), joinPoint));
            return result;
        } catch (Exception e) {
            operationLog.setResult(1);
            String errorMsg = e.getMessage();
            operationLog.setErrorMsg(errorMsg != null && errorMsg.length() > 500
                    ? errorMsg.substring(0, 500)
                    : errorMsg);
            operationLog.setDescription(buildDescription(logAnnotation.description(), joinPoint));
            throw e;
        } finally {
            operationLog.setDuration(System.currentTimeMillis() - startTime);
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception ex) {
                log.error(
                        "Failed to save operation log: module={}, action={}, path={}, operator={}",
                        operationLog.getModule(),
                        operationLog.getAction(),
                        operationLog.getRequestPath(),
                        operationLog.getOperator(),
                        ex
                );
            }
        }
    }

    private String getCurrentUsername(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "SYSTEM";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        HttpServletRequest request = attributes.getRequest();
        String username = request.getParameter("username");
        if (username != null && !username.isEmpty()) {
            return username;
        }
        username = extractUsernameFromArgs(joinPoint);
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return "ANONYMOUS";
    }

    private String extractUsernameFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            String username = extractUsername(arg);
            if (username != null && !username.isEmpty()) {
                return username;
            }
        }
        return null;
    }

    private String extractUsername(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof Map<?, ?> map) {
            Object value = map.get("username");
            return value != null ? value.toString() : null;
        }

        try {
            Method method = arg.getClass().getMethod("getUsername");
            Object value = method.invoke(arg);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception ignored) {
            // Fall back to direct field access for simple DTOs without a public getter.
        }

        try {
            Field field = arg.getClass().getDeclaredField("username");
            field.setAccessible(true);
            Object value = field.get(arg);
            return value != null ? value.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String filterSensitiveParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }

            List<Object> safeArgs = new ArrayList<>();
            for (Object arg : args) {
                Object sanitized = sanitizeLogArg(arg);
                if (sanitized != null) {
                    safeArgs.add(sanitized);
                }
            }
            return maskSensitiveFieldsForLog(MAPPER.writeValueAsString(safeArgs));
        } catch (Exception e) {
            log.warn("Failed to serialize request params", e);
            return "[serialization error]";
        }
    }

    private Object sanitizeLogArg(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof MultipartFile) {
            MultipartFile file = (MultipartFile) arg;
            return buildMultipartFileLog(file);
        }
        if (arg instanceof MultipartFile[]) {
            MultipartFile[] files = (MultipartFile[]) arg;
            List<Object> fileLogs = new ArrayList<>();
            for (MultipartFile file : files) {
                fileLogs.add(buildMultipartFileLog(file));
            }
            return fileLogs;
        }
        if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse || arg instanceof HttpSession) {
            return null;
        }
        if (arg instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) arg;
            List<Object> items = new ArrayList<>();
            for (Object item : collection) {
                Object sanitized = sanitizeLogArg(item);
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        if (arg instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) arg;
            Map<String, Object> safeMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object sanitized = sanitizeLogArg(entry.getValue());
                if (sanitized != null) {
                    safeMap.put(String.valueOf(entry.getKey()), sanitized);
                }
            }
            return safeMap;
        }
        if (arg.getClass().isArray()) {
            int length = Array.getLength(arg);
            List<Object> items = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Object sanitized = sanitizeLogArg(Array.get(arg, i));
                if (sanitized != null) {
                    items.add(sanitized);
                }
            }
            return items;
        }
        return arg;
    }

    private Map<String, Object> buildMultipartFileLog(MultipartFile file) {
        Map<String, Object> fileInfo = new LinkedHashMap<>();
        fileInfo.put("fileName", file.getOriginalFilename());
        fileInfo.put("size", file.getSize());
        fileInfo.put("contentType", file.getContentType());
        return fileInfo;
    }

    static String maskSensitiveFieldsForLog(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String result = json;
        for (String field : SENSITIVE_FIELDS) {
            result = result.replaceAll(
                    "(?i)(\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")",
                    "$1****$3"
            );
        }
        return result;
    }

    private String buildDescription(String template, ProceedingJoinPoint joinPoint) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (!template.contains("#")) {
            return template;
        }

        String result = template;
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(result);
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String value = resolveParamValue(paramName, paramNames, args);
            result = result.replace("#" + paramName, value);
        }
        return result;
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Parameter[] parameters = method.getParameters();
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].getName();
            }
            return names;
        } catch (Exception e) {
            return new String[0];
        }
    }

    private String resolveParamValue(String paramName, String[] paramNames, Object[] args) {
        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && i < args.length) {
                return args[i] != null ? args[i].toString() : "null";
            }
        }
        return "#" + paramName;
    }
}
