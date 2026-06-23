package com.openquartz.easyarchive.core.connection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC URL 增强器
 * <p>
 * 在创建数据源时自动追加必要的 MySQL 连接参数，
 * 用户无需在 URL 中手动填写这些参数。
 * </p>
 *
 * <p>自动追加的参数（仅在 URL 中不存在时追加）：</p>
 * <ul>
 *   <li>{@code useUnicode=true}</li>
 *   <li>{@code characterEncoding=UTF-8}</li>
 *   <li>{@code useSSL=false}</li>
 *   <li>{@code allowPublicKeyRetrieval=true}</li>
 *   <li>{@code serverTimezone=Asia/Shanghai}</li>
 *   <li>{@code zeroDateTimeBehavior=convertToNull}</li>
 *   <li>{@code nullCatalogMeansCurrent=true}</li>
 * </ul>
 *
 * @author svnee
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcUrlEnhancer {

    /**
     * 需要自动追加的参数及默认值（保持插入顺序）
     */
    private static final Map<String, String> DEFAULT_PARAMS = new LinkedHashMap<>();

    static {
        DEFAULT_PARAMS.put("useUnicode", "true");
        DEFAULT_PARAMS.put("characterEncoding", "UTF-8");
        DEFAULT_PARAMS.put("useSSL", "false");
        DEFAULT_PARAMS.put("allowPublicKeyRetrieval", "true");
        DEFAULT_PARAMS.put("serverTimezone", "Asia/Shanghai");
        DEFAULT_PARAMS.put("zeroDateTimeBehavior", "convertToNull");
        DEFAULT_PARAMS.put("nullCatalogMeansCurrent", "true");
    }

    /**
     * 对 MySQL JDBC URL 自动追加必要参数。
     * <p>
     * 若 URL 中已包含某参数则保留用户原值，仅追加缺失的参数。
     * 非 MySQL JDBC URL 将原样返回。
     * </p>
     *
     * @param jdbcUrl 原始 JDBC URL
     * @return 增强后的 JDBC URL
     */
    public static String enhance(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return jdbcUrl;
        }

        // 仅处理 MySQL JDBC URL
        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            return jdbcUrl;
        }

        try {
            // 拆分 URL 和查询参数部分
            int queryStart = jdbcUrl.indexOf('?');
            String baseUrl = queryStart >= 0 ? jdbcUrl.substring(0, queryStart) : jdbcUrl;
            String queryPart = queryStart >= 0 ? jdbcUrl.substring(queryStart + 1) : "";

            // 解析已有参数（保持原始顺序）
            Map<String, String> existingParams = new LinkedHashMap<>();
            if (!queryPart.isEmpty()) {
                for (String pair : queryPart.split("&")) {
                    if (pair.isEmpty()) {
                        continue;
                    }
                    int eqIdx = pair.indexOf('=');
                    if (eqIdx > 0) {
                        String key = pair.substring(0, eqIdx);
                        String value = pair.substring(eqIdx + 1);
                        existingParams.put(key, value);
                    } else {
                        // 无值的参数（如 &serverTimezone）保留占位
                        existingParams.put(pair, null);
                    }
                }
            }

            // 构建增强后的参数：已有参数保持原值，缺失参数追加默认值
            Map<String, String> mergedParams = new LinkedHashMap<>(existingParams);
            for (Map.Entry<String, String> entry : DEFAULT_PARAMS.entrySet()) {
                String key = entry.getKey();
                if (!mergedParams.containsKey(key)) {
                    mergedParams.put(key, entry.getValue());
                    log.debug("[JdbcUrlEnhancer] auto append parameter: {}={}", key, entry.getValue());
                }
            }

            // 如果没有任何参数需要追加，直接返回原 URL
            if (mergedParams.equals(existingParams)) {
                return jdbcUrl;
            }

            // 拼接最终 URL
            StringBuilder sb = new StringBuilder(baseUrl);
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : mergedParams.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(entry.getKey());
                if (entry.getValue() != null) {
                    sb.append('=').append(entry.getValue());
                }
            }

            String enhanced = sb.toString();
            log.info("[JdbcUrlEnhancer] enhanced jdbc url: {}", enhanced);
            return enhanced;

        } catch (Exception e) {
            log.warn("[JdbcUrlEnhancer] failed to enhance url, using original: {}", jdbcUrl, e);
            return jdbcUrl;
        }
    }
}
