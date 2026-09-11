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

let accessToken: string | null = null;

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

    accessToken = body.data.accessToken;

    return getMe();
}

export async function getMe(): Promise<Member> {
    if (!accessToken) {
        throw new Error("로그인이 필요합니다.");
    }

    const response = await fetch("/api/members/me", {
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
        credentials: "include",
    });

    const body = await parseApiResponse<Member>(response);

    if (!body.data) {
        throw new Error("회원 정보를 불러오지 못했습니다.");
    }

    return body.data;
}

export function getAccessToken(): string | null {
    return accessToken;
}