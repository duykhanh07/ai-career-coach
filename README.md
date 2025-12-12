# 🚀 AI Career Coach - Trợ lý Sự nghiệp Ảo

**AI Career Coach** là một ứng dụng Fullstack Serverless hiện đại, sử dụng sức mạnh của **Generative AI (Amazon Bedrock - Claude 3)** để hỗ trợ người tìm việc tối ưu hóa quy trình ứng tuyển.

Dự án này áp dụng kiến trúc **Microservices** với **Java Spring Cloud Function** trên AWS Lambda và **React (Vite)** cho Frontend.

![Project Banner](/assets/dashboard.png)

---

## ✨ Tính năng Chính

* **📊 Dashboard:** Theo dõi tiến độ, thống kê lịch sử hoạt động và điểm số phỏng vấn.
* **📝 AI Resume Builder:** Trình soạn thảo CV định dạng Markdown. Tích hợp AI để viết lại (rephrase) các câu mô tả kinh nghiệm sao cho chuyên nghiệp và chuẩn ATS.
* **💌 Cover Letter Generator:** Tự động tạo thư xin việc (Cover Letter) được cá nhân hóa dựa trên JD (Job Description) và hồ sơ người dùng.
* **🎤 Mock Interview:** Phỏng vấn thử với bộ câu hỏi trắc nghiệm được AI sinh ra dựa trên ngành nghề cụ thể. Hệ thống tự động chấm điểm và lưu lịch sử.
* **🔐 Authentication:** Đăng ký/Đăng nhập bảo mật với Amazon Cognito (JWT).

---

## 📸 Video Demo

[Xem tại](https://www.youtube.com/watch?v=I2NEzLo2n7s)

---

## 🏗️ Kiến trúc Hệ thống

Hệ thống được xây dựng hoàn toàn trên nền tảng AWS Serverless

![architecture](/assets/AI_Career_Coach_Architecture.png)

---

## Tech stack

| Thành phần | Công nghệ | Chi tiết |
| :--- | :--- | :--- |
| **Frontend** | React | Vite, Tailwind CSS, Shadcn UI, Lucide React |
| **Auth & API** | AWS Amplify | Kết nối Cognito, apiClient wrapper |
| **Backend** | Java 17 | Spring Boot 3, Spring Cloud Function |
| **Compute** | AWS Lambda | Serverless Compute, SnapStart enabled |
| **Database** | DynamoDB | Single Table Design (PK/SK patterns) |
| **AI Model** | Amazon Bedrock | Anthropic Claude 3 Haiku |
| **IaC** | AWS SAM | Infrastructure as Code (template.yaml) |

---

## 🚀 Cài đặt & Chạy dự án

1. Yêu cầu tiên quyết
* Java 17 (JDK)
* Node.js (v18+) & npm
* AWS CLI (Đã cấu hình aws configure)
* AWS SAM CLI
* Maven
* Tài khoản AWS đã kích hoạt quyền truy cập model Claude 3 Haiku trong Amazon Bedrock.

2. Triển khai Backend (AWS)
* Tại thư mục gốc, build project Java
```
cd backend
mvn clean package -DskipTests
```
* Quay lại thư mục gốc và deploy với SAM
```
cd ..
sam deploy --guided
```
Làm theo hướng dẫn trên màn hình. Sau khi deploy xong, hãy lưu lại các thông số Output:

* ApiEndpoint
* CognitoUserPoolId
* CognitoClientId
* S3BucketName
* WebsiteURL

3. Cấu hình & Chạy Frontend
* Mở file `frontend/src/lib/api.js` và cập nhật `BASE_URL`:

```JavaScript

const BASE_URL = "https://<API_ID>.execute-api.<REGION>.amazonaws.com";
```
* Mở file `frontend/src/main.jsx` và cập nhật cấu hình Amplify:

```JavaScript

Amplify.configure({
Auth: {
Cognito: {
userPoolId: '...',
userPoolClientId: '...',
// ...
}
}
});
```
* Chạy thử dưới local:

```
cd frontend
npm install
npm run dev
```

---

## 🌐 Triển khai Frontend lên S3 & CloudFront

Để đưa website ra Internet:

* Build bản production
```
cd frontend
npm run build
```

* Đồng bộ lên S3 (Thay tên bucket của bạn vào)

```
aws s3 sync dist s3://career-coach-frontend-YOUR-ACCOUNT-ID --delete
```
* Xóa cache CloudFront để cập nhật code mới (Thay ID CloudFront vào)
```
aws cloudfront create-invalidation --distribution-id YOUR_DIST_ID --paths "/*"
```
Truy cập vào link CloudFront URL để sử dụng ứng dụng.

---

## 📂 Cấu trúc Thư mục
#### 1. Backend
![BE](/assets/backend.png)
#### 2. Frontend
![FE](/assets/frontend.png)

---

## ⚠️ Lưu ý quan trọng
* Chi phí: Dự án sử dụng các dịch vụ có Free Tier (Lambda, DynamoDB), nhưng Bedrock sẽ tính phí theo số lượng token. Hãy nhớ sam delete để dọn dẹp tài nguyên khi không sử dụng.
* Prompt Engineering: Các prompt AI nằm trong folder backend/.../service. Bạn có thể tùy chỉnh để AI trả lời hay hơn.
* Bảo mật: Không commit file .env hoặc các key nhạy cảm lên Git public.
---
## 🤝 Đóng góp

Dự án này được xây dựng cho mục đích học tập và làm Portfolio. Mọi đóng góp (Pull Request) đều được hoan nghênh!

---

Author: Duy Khanh | Contact: ldk11072003@gmail.com
