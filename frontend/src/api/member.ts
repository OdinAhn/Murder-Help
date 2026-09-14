import { authFetch } from "./client";

type ApiResponse<T> = {
    code: string;
    message?: string;
    data: T;
};

export type MemberInfo = {
    id: number;
    email: string;
    name: string;
    phone: string;
    grade: "YELLOW" | "PURPLE" | "RED" | "GREEN";
    profileImageUrl: string | null;
};

async function request<T>(
    url: string,
    options: RequestInit = {},
): Promise<T> {
    const response = await authFetch(url, options);

    const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;

    if (!response.ok) {
        throw new Error(body?.message ?? "요청 처리에 실패했습니다.");
    }

    if (!body) {
        throw new Error("서버 응답을 확인할 수 없습니다.");
    }

    return body.data;
}

export function getMyProfile() {
    return request<MemberInfo>("/api/members/me");
}

export function updateMyProfile(name: string, phone: string) {
    return request<MemberInfo>("/api/members/me", {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ name, phone }),
    });
}

export function deleteProfileImage() {
    return request<MemberInfo>("/api/members/me/profile-image", {
        method: "DELETE",
    });
}

export function uploadProfileImage(image: File) {
    const formData = new FormData();
    formData.append("image", image);

    return request<MemberInfo>("/api/members/me/profile-image", {
        method: "PATCH",
        body: formData,
    });
    
    
}