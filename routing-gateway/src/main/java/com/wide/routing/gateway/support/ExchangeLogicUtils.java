package com.wide.routing.gateway.support;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;

public final class ExchangeLogicUtils {

    private static final String WEBIDE_PRODUCT_ID = "productId";

    private static final String WEBIDE_PRODUCT_TYPE = "productType";

    public static boolean matchScheme(URI url, @NotNull String schemePrefix, @NotNull String scheme) {
        return url != null
                && (scheme.equals(url.getScheme()) || scheme.equals(schemePrefix));
    }

    @NotNull
    public static String normalizeServiceId(ServerWebExchange exchange) {
        Map<String, String> uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
        String productType = uriVariables.get(WEBIDE_PRODUCT_TYPE);
        String productId = uriVariables.get(WEBIDE_PRODUCT_ID);

        return StringUtils.hasLength(productType) && StringUtils.hasLength(productId) ?
                productType + "-" + productId :
                "";
    }
}
