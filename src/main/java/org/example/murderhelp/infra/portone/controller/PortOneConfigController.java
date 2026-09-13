package org.example.murderhelp.infra.portone.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.infra.portone.config.PortOneProperties;
import org.example.murderhelp.infra.portone.dto.PortOneConfigResponse;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PortOneConfigController {

    private final PortOneProperties portOneProperties;

    @GetMapping("/api/config/portone")
    public ResponseEntity<ApiResponse<PortOneConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(new PortOneConfigResponse(
                portOneProperties.getStoreId(),
                portOneProperties.getChannelKey()
        )));
    }
}