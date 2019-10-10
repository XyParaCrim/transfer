package com.wide.routing.gateway.filter;

import com.wide.routing.gateway.support.ExchangeLogicUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;
import static com.wide.routing.gateway.support.ExchangeLogicUtils.*;


public class DynamicLoadBalancerClientFilter implements GlobalFilter, Ordered {

    private static final String DYNAMIC_SCHEME = "dlb";

    private static final String REPLACE_SCHEME = "lb";

    /**
     * 在LoadBalancerClientFilter之前执行
     */
    private static final int BEFORE_LOAD_BALANCE_PRECEDENCE = LoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;

    private static final Log log = LogFactory.getLog(DynamicLoadBalancerClientFilter.class);

    private final LoadBalancerProperties properties;

    public DynamicLoadBalancerClientFilter(LoadBalancerProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return BEFORE_LOAD_BALANCE_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (matchScheme(url, schemePrefix, DYNAMIC_SCHEME)) {
            // preserve the original url
            addOriginalRequestUrl(exchange, url);

            log.trace("DynamicLoadBalancerClientFilter url before: " + url);

            // identify how to reconstruct serviceId
            ReconstructServiceId reconstructServiceId = serviceIdReconstructed.get(url.getHost());
            if (Objects.isNull(reconstructServiceId)) {
                throwNotFoundException(url);
            }

            // verify  serviceId Simply
            String newServiceId = reconstructServiceId.apply(exchange);
            if (newServiceId.isEmpty()) {
                throwNotFoundException(url);
            }

            String newScheme = REPLACE_SCHEME;
            if (schemePrefix != null &&
                    schemePrefix.equals(DYNAMIC_SCHEME)) {
                exchange.getAttributes().put(GATEWAY_SCHEME_PREFIX_ATTR, REPLACE_SCHEME);
                newScheme = url.getScheme();
            }

            URI newUrl = ExchangeLogicUtils.replaceURISchemeAndHost(url, newScheme, newServiceId);

            log.trace("DynamicLoadBalancerClientFilter new url: " + newUrl);
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newUrl);
        }

        return chain.filter(exchange);
    }

    private void throwNotFoundException(URI uri) {
        throw NotFoundException.create(properties.isUse404(),
                "Unable to resolve service id for " + uri.getHost());
    }

    private interface ReconstructServiceId extends Function<ServerWebExchange, String> {}

    private static Map<String, ReconstructServiceId> serviceIdReconstructed;

    static {
        // 解析serviceId的几种方式
        serviceIdReconstructed = new HashMap<>(2);
        // 从请求路径中的临时变量获取serviceId
        serviceIdReconstructed.put("fromPath", ExchangeLogicUtils::normalizeServiceIdFromPath);
        // 从Cookie中获取
        serviceIdReconstructed.put("fromCookie", ExchangeLogicUtils::normalizeServiceIdFromCookie);
    }
}
