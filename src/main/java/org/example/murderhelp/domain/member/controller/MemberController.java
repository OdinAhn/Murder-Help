package org.example.murderhelp.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.dto.MemberResponse;
import org.example.murderhelp.domain.member.dto.UpdateMyInfoRequest;
import org.example.murderhelp.domain.member.service.MemberProfileService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberProfileService memberProfileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.getMyProfile(memberId))
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMyInfo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateMyInfoRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.updateMyProfile(memberId, request))
        );
    }

    @PatchMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MemberResponse>> uploadProfileImage(
            @AuthenticationPrincipal Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.uploadProfileImage(memberId, image))
        );
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<MemberResponse>> deleteProfileImage(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.deleteProfileImage(memberId))
        );
    }
}