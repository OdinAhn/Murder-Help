package org.example.murderhelp.domain.member.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.dto.MemberResponse;
import org.example.murderhelp.domain.member.dto.UpdateMyInfoRequest;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.net.URI;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final MemberRepository memberRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.profile-prefix:profiles}")
    private String profilePrefix;

    /*
     * 예시:
     * https://dxxxxx.cloudfront.net
     *
     * 비어 있으면 S3 URL을 저장한다.
     * S3를 private으로 운영한다면 반드시 CloudFront 주소를 설정해야 한다.
     */
    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Value("${spring.cloud.aws.region.static:ap-northeast-2}")
    private String region;

    public MemberResponse getMyProfile(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    @Transactional
    public MemberResponse updateMyProfile(Long memberId, UpdateMyInfoRequest request) {
        Member member = findMember(memberId);
        member.updateProfile(request.name(), request.phone());

        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse uploadProfileImage(Long memberId, MultipartFile image) {
        validateImage(image);

        String contentType = image.getContentType();
        String extension = EXTENSIONS.get(contentType);
        String key = profilePrefix + "/" + memberId + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(image.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(image.getBytes())
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 파일을 읽을 수 없습니다.");
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드에 실패했습니다."
            );
        }

        Member member = findMember(memberId);
        member.changeProfileImage(makeImageUrl(key));

        return MemberResponse.from(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일을 선택해주세요.");
        }

        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "프로필 이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        if (!EXTENSIONS.containsKey(image.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "JPG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String makeImageUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/$", "") + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    @Transactional
    public MemberResponse deleteProfileImage(Long memberId) {
        Member member = findMember(memberId);
        String profileImageUrl = member.getProfileImageUrl();

        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return MemberResponse.from(member);
        }

        String key = extractProfileImageKey(profileImageUrl, memberId);

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "기존 프로필 이미지 삭제에 실패했습니다."
            );
        }

        member.changeProfileImage(null);

        return MemberResponse.from(member);
    }

    private String extractProfileImageKey(String profileImageUrl, Long memberId) {
        try {
            URI uri = URI.create(profileImageUrl);

            String key = uri.getPath().replaceFirst("^/", "");
            String allowedPrefix = profilePrefix + "/" + memberId + "/";

            if (!key.startsWith(allowedPrefix)) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "삭제할 수 없는 프로필 이미지 경로입니다."
                );
            }

            return key;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "프로필 이미지 URL 형식이 올바르지 않습니다."
            );
        }
    }
}