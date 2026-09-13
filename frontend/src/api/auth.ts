import { authFetch, getAccessToken, setAccessToken } from "./client";

type ApiResponse<T> = {
    code: string;
    message?: string;
    data?: T;
};

type TokenResponse = {
    accessToken: string;
};

export type Member = {
    id: number;
    email: string;
    name: string;
    phone: string;
    grade: "YELLOW" | "PURPLE" | "RED" | "GREEN";
};

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
    const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;

    if (!response.ok || !body || body.code !== "SUCCESS") {
        throw new Error(body?.message ?? `요청에 실패했습니다. (${response.status})`);
    }

    return body;
}

export async function signup(input: {
    name: string;
    email: string;
    password: string;
    phone: string;
}): Promise<void> {
    const response = await fetch("/api/auth/signup", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(input),
    });

    await parseApiResponse<void>(response);
}

export async function login(email: string, password: string): Promise<Member> {
    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ email, password }),
    });

    const body = await parseApiResponse<TokenResponse>(response);

    if (!body.data?.accessToken) {
        throw new Error("액세스 토큰을 받지 못했습니다.");
    }

    setAccessToken(body.data.accessToken);

    return getMe();
}

export async function logout(): Promise<void> {
    try {
        if (getAccessToken()) {
            const response = await authFetch("/api/auth/logout", {
                method: "POST",
            });

            if (!response.ok) {
                throw new Error("로그아웃에 실패했습니다.");
            }
        }
    } finally {
        // accessToken은 브라우저 메모리 값이므로 항상 제거
        setAccessToken(null);
    }
}

export async function getMe(): Promise<Member> {
    const response = await authFetch("/api/members/me");

    const body = await parseApiResponse<Member>(response);

    if (!body.data) {
        throw new Error("회원 정보를 불러오지 못했습니다.");
    }

    return body.data;
}
