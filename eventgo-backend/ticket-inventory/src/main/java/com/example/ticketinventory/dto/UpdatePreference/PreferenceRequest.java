package com.example.ticketinventory.dto.UpdatePreference;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Getter
@Setter
@Data
@Schema(description = "Request object for updating ticket preference")
public class PreferenceRequest {
    @Schema(description = "ID of the ticket to update preference", example = "1", required = true)
    private Long ticketId;
    @Schema(description = "Preference to update", example = "keep", required = true)
    private String preference;
    
}