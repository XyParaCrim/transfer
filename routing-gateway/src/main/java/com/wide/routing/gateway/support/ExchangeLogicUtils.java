package com.wide.routing.gateway.support;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ExchangeLogicUtils {

    private static final String WEBIDE_PRODUCT_ID = "productId";

    private static final String WEBIDE_PRODUCT_TYPE = "productType";

    private static final String VIP_ADDRESS_FROM_COOKIE = "pid";

    public static boolean matchScheme(URI url, @NotNull String schemePrefix, @NotNull String scheme) {
        return url != null
                && (scheme.equals(url.getScheme()) || scheme.equals(schemePrefix));
    }

    @NotNull
    public static String normalizeServiceIdFromPath(ServerWebExchange exchange) {
        Map<String, String> uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
        String productType = uriVariables.get(WEBIDE_PRODUCT_TYPE);
        String productId = uriVariables.get(WEBIDE_PRODUCT_ID);

        return StringUtils.hasLength(productType) && StringUtils.hasLength(productId) ?
                productType + "-" + productId :
                "";
    }

    @NotNull
    public static String normalizeServiceIdFromCookie(ServerWebExchange exchange) {
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get(VIP_ADDRESS_FROM_COOKIE);
        if (!CollectionUtils.isEmpty(cookies)) {
            for (HttpCookie cookie : cookies) {
                if (StringUtils.hasLength(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return "";
    }

    public static URI replaceURISchemeAndHost(URI original, String scheme, String host) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://");
            if (StringUtils.hasLength(original.getRawUserInfo())) {
                sb.append(original.getRawUserInfo()).append("@");
            }
            sb.append(host);
            sb.append(original.getRawPath());
            if (StringUtils.hasLength(original.getRawQuery())) {
                sb.append("?").append(original.getRawQuery());
            }
            if (StringUtils.hasLength(original.getRawFragment())) {
                sb.append("#").append(original.getRawFragment());
            }
            return new URI(sb.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
