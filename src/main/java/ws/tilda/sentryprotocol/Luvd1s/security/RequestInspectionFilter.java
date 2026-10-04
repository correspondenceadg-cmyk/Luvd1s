package ws.tilda.sentryprotocol.Luvd1s.security;

import ws.tilda.sentryprotocol.Luvd1s.service.RecentRequestsBuffer;
import ws.tilda.sentryprotocol.Luvd1s.service.RequestRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class RequestInspectionFilter extends OncePerRequestFilter {

    private final RecentRequestsBuffer buffer;

    public RequestInspectionFilter(RecentRequestsBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();
            if (!isIgnored(path)) {
                long duration = System.currentTimeMillis() - start;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String user = (auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getName()))
                        ? auth.getName()
                        : null;

                buffer.add(new RequestRecord(
                        Instant.now(),
                        request.getMethod(),
                        path,
                        user,
                        response.getStatus(),
                        duration
                ));
            }
        }
    }

    private boolean isIgnored(String path) {
        return path.startsWith("/VAADIN/")
                || path.startsWith("/frontend/")
                || path.startsWith("/sw.js")
                || path.startsWith("/manifest.webmanifest")
                || path.startsWith("/icons/")
                || path.startsWith("/styles/")
                || path.startsWith("/fonts/")
                || path.equals("/favicon.ico");
    }
}