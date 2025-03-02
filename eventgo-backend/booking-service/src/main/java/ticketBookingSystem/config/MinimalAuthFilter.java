package ticketBookingSystem.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ticketBookingSystem.dto.Authentication.AuthenticatedUserDTO;
import ticketBookingSystem.dto.Authentication.TokenRequestDTO;

@Component
public class MinimalAuthFilter extends OncePerRequestFilter {
    @Value("${AUTH_SERVICE_URL}")
    private String authServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Call auth service to validate token
                TokenRequestDTO tokenRequestDTO = new TokenRequestDTO(token);
                ResponseEntity<AuthenticatedUserDTO> authResponse = restTemplate.postForEntity(authServiceUrl + "/validate-token", tokenRequestDTO, AuthenticatedUserDTO.class);

                if(authResponse.getStatusCode() == HttpStatus.OK && authResponse.getBody() != null) {
                    AuthenticatedUserDTO user = authResponse.getBody();
                    // Create auth object and set it in securitycontext

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, token, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }


            } catch(Exception ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token validation failed");
                return;
            }
        }  else {
            // No token provided; can reject
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        filterChain.doFilter(request, response);
    } 
}
