package ticketBookingSystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    
    @Autowired
    private MinimalAuthFilter minimalAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Permit any public endpoints if needed
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/bookings/test").permitAll()
                .requestMatchers("/bookings/status").permitAll()
                .requestMatchers("/bookings/initiateBooking").permitAll() 
                // .requestMatchers("/bookings/process-booking").authenticated()
                .requestMatchers("/bookings/process-booking").permitAll()

                // .anyRequest().permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        // Add our minimal auth filter before the standard authentication filter
        http.addFilterBefore(minimalAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
