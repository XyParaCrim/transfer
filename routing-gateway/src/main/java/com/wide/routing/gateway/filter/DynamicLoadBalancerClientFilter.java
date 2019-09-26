package com.wide.routing.gateway.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;
import static com.wide.routing.gateway.support.ExchangeLogicUtils.*;


public class DynamicLoadBalancerClientFilter implements GlobalFilter, Ordered {

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
        if (matchScheme(url, schemePrefix, "dlb")) {
            // preserve the original url
            addOriginalRequestUrl(exchange, url);

            log.trace("DynamicLoadBalancerClientFilter url before: " + url);

            String newServiceId = normalizeServiceId(exchange);

            if (newServiceId.isEmpty()) {
                throwNotFoundException(url);
            }

            String newScheme = "lb";
            if (schemePrefix != null &&
                    schemePrefix.equals("dlb")) {
                exchange.getAttributes().put(GATEWAY_SCHEME_PREFIX_ATTR, "lb");
                newScheme = url.getScheme();
            }

            URI newUrl = newLoadBalancerUri(url, newScheme, newServiceId);

            log.trace("DynamicLoadBalancerClientFilter new url: " + newUrl);
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newUrl);
        }

        return chain.filter(exchange);
    }


    /**
     * 替换scheme，修改host为newServiceId（dbl://serviceId/path1?param1=1 => lb://newServiceId/path1?param1=1）
     */
    private URI newLoadBalancerUri(URI original, String scheme, String newServiceId) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://");
            if (StringUtils.hasLength(original.getRawUserInfo())) {
                sb.append(original.getRawUserInfo()).append("@");
            }
            if ("serviceId".equals(original.getHost())) {
                sb.append(newServiceId);
            } else {
                throwNotFoundException(original);
            }
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

    private void throwNotFoundException(URI uri) {
        throw NotFoundException.create(properties.isUse404(),
                "Unable to resolve service id for " + uri.getHost());
    }
}
