// package ticketBookingSystem.config;

// import java.io.IOException;
// import java.util.Arrays;
// import java.util.List;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.util.AntPathMatcher;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.filter.OncePerRequestFilter;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.extern.slf4j.Slf4j;
// import ticketBookingSystem.dto.Authentication.AuthenticatedUserDTO;
// import ticketBookingSystem.dto.Authentication.TokenRequestDTO;

// @Slf4j
// @Component
// public class MinimalAuthFilter extends OncePerRequestFilter {

//     @Value("${AUTH_SERVICE_URL}")
//     private String authServiceUrl;

//     private final RestTemplate restTemplate = new RestTemplate();

//     // List of paths to exclude from authentication
//     private final List<String> excludedPaths = Arrays.asList(
//         "/public/**",
//         "/bookings/test",
//         "/bookings/status",
//         "/bookings/initiateBooking",
//         "/bookings/process-booking"
//     );

//     private final AntPathMatcher pathMatcher = new AntPathMatcher();

//     @Override
//     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//             throws ServletException, IOException {

//         String path = request.getRequestURI();
//         log.debug("Request URI: {}", path);

//         // Check if the request path is in the excluded list
//         boolean isExcluded = excludedPaths.stream().anyMatch(p -> pathMatcher.match(p, path));

//         if (isExcluded) {
//             log.debug("Request URI {} is excluded from authentication", path);
//             filterChain.doFilter(request, response);
//             return;
//         }

//         String authHeader = request.getHeader("Authorization");
//         log.debug("Authorization header: {}", authHeader);

//         if (authHeader != null && authHeader.startsWith("Bearer ")) {
//             String token = authHeader.substring(7);
//             log.debug("Extracted token: {}", token);
//             try {
//                 TokenRequestDTO tokenRequestDTO = new TokenRequestDTO(token);
//                 ResponseEntity<AuthenticatedUserDTO> authResponse = restTemplate.postForEntity(
//                         authServiceUrl + "/validate-token", tokenRequestDTO, AuthenticatedUserDTO.class);
//                 log.debug("Auth service response status: {}", authResponse.getStatusCode());
//                 log.debug("Auth service response body: {}", authResponse.getBody());

//                 if (authResponse.getStatusCode() == HttpStatus.OK && authResponse.getBody() != null) {
//                     AuthenticatedUserDTO user = authResponse.getBody();
//                     // Optionally add roles if needed
//                     List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
//                     UsernamePasswordAuthenticationToken authentication =
//                             new UsernamePasswordAuthenticationToken(user, token, authorities);
//                     SecurityContextHolder.getContext().setAuthentication(authentication);
//                     log.debug("Authentication set in SecurityContext: {}", authentication);
//                 } else {
//                     log.warn("Token validation returned non-OK status or empty body");
//                     response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
//                     return;
//                 }
//             } catch (Exception ex) {
//                 log.error("Exception during token validation", ex);
//                 response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token validation failed");
//                 return;
//             }
//         } else {
//             log.warn("Missing or invalid Authorization header");
//             response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
//             return;
//         }
//         filterChain.doFilter(request, response);
//     }
// }
