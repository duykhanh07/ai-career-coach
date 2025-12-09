import { fetchAuthSession } from 'aws-amplify/auth';

// URL API Gateway của bạn (Lấy từ lệnh sam deploy --guided hoặc file aws-exports cũ nếu có)
// Ví dụ: https://xyz.execute-api.ap-southeast-1.amazonaws.com
const BASE_URL = "https://lpxkgif81j.execute-api.ap-southeast-1.amazonaws.com";

/**
 * Hàm gọi API chung cho toàn bộ ứng dụng (Wrapper for fetch)
 * Tự động thêm Token và xử lý lỗi chuẩn.
 * * @param {string} endpoint - Đường dẫn API (vd: "/profile", "/resume")
 * @param {object} options - Các tùy chọn fetch (method, body...)
 * @returns {Promise<any>} - Dữ liệu JSON trả về từ Server
 */
export const apiClient = async (endpoint, options = {}) => {
  try {
    // 1. Lấy Token
    const session = await fetchAuthSession();
    const token = session.tokens?.idToken?.toString();

    if (!token) {
      throw new Error("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
    }

    // 2. Gọi API
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
      },
    });

    // 3. Xử lý lỗi HTTP
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || errorData.error || `Lỗi API: ${response.status}`);
    }

    // 4. Đọc dữ liệu trả về
    const text = await response.text();
    const data = text ? JSON.parse(text) : {};

    // --- FIX QUAN TRỌNG: TỰ ĐỘNG BÓC TÁCH BODY ---
    // Nếu Backend trả về dạng { statusCode: 200, body: "string_json" }
    // Ta cần parse cái "string_json" đó ra để lấy dữ liệu thật.
    if (data.body && typeof data.body === 'string') {
        try {
            return JSON.parse(data.body);
        } catch (e) {
            // Nếu body không phải JSON (ví dụ text thường), trả về nguyên gốc
            return data;
        }
    }
    // ---------------------------------------------

    return data;

  } catch (error) {
    console.error(`API Call Failed [${endpoint}]:`, error);
    throw error;
  }
};

/**
 * --- CÁC HÀM TIỆN ÍCH CHO TỪNG MODULE (Gợi ý) ---
 * Bạn có thể dùng trực tiếp apiClient() trong component,
 * hoặc định nghĩa các hàm con tại đây để tái sử dụng.
 */

// Module User
export const getUserProfile = () => apiClient("/profile");
export const updateUserProfile = (data) => apiClient("/profile", { method: "POST", body: JSON.stringify(data) });
export const checkOnboardingStatus = () => apiClient("/onboarding");

// Module Industry
export const getIndustryInsights = () => apiClient("/industry-insights");

// Module Resume
export const getResume = () => apiClient("/resume");
export const saveResume = (content) => apiClient("/resume", { method: "POST", body: JSON.stringify({ content }) });
export const improveResumeWithAI = (currentContent, type) => apiClient("/resume/improve", {
    method: "POST",
    body: JSON.stringify({ current: currentContent, type })
});

// Module Cover Letter
export const getCoverLetters = () => apiClient("/cover-letters");
export const getCoverLetterById = (id) => apiClient(`/cover-letters/${id}`);
export const createCoverLetter = (data) => apiClient("/cover-letters", { method: "POST", body: JSON.stringify(data) });
export const deleteCoverLetter = (id) => apiClient(`/cover-letters/${id}`, { method: "DELETE" });

// Module Interview
export const getInterviewHistory = () => apiClient("/interview/history");
export const generateQuiz = () => apiClient("/interview/generate", { method: "POST" });
export const submitQuiz = (data) => apiClient("/interview/save", { method: "POST", body: JSON.stringify(data) });