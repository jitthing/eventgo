package com.example.ticketinventory.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI ticketInventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Inventory API")
                        .description("API for managing event tickets: reservations, purchases, and transfers")
                        .version("1.0")
                        .contact(new Contact()
                                .name("EventGo Team")
                                .email("support@eventgo.com")));
    }
}