package yingshop.common.logging;

import javax.servlet.*;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String rid = UUID.randomUUID().toString();
        MDC.put("requestId", rid);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear(); // 避免 ThreadLocal 殘留
        }
    }
}
