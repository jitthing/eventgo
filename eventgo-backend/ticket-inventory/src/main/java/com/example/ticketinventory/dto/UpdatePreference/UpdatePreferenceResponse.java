package com.example.ticketinventory.dto.UpdatePreference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;

@Getter
@Setter
@Data
@Schema(description = "Response object for updating ticket preference")
public class UpdatePreferenceResponse {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;
    @Schema(description = "Response message", example = "Preference updated successfully", required = true)
    private String message;
}
