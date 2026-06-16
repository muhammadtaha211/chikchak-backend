package com.chikchak.dto;

import com.chikchak.model.BookingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingUpdateRequest {
    private BookingStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledDatetime;

    @Size(max = 2000, message = "Notes are too long")
    private String technicianNotes;

    @Size(max = 500, message = "Reason is too long")
    private String reason;
}
