package com.bank.admin.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Login response")
public class LoginVO {

    @Schema(description = "Access token")
    private String token;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Nickname")
    private String nickname;

    @Schema(description = "Role")
    private String role;

    @Schema(description = "Data scope: ALL or SELF")
    private String dataScope;
}
