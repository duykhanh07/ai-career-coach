import { Suspense } from "react";
import { BarLoader } from "react-spinners";
import { Outlet } from "react-router-dom"; // <--- Import Outlet

export default function InterviewLayout() {
  return (
    <div className="px-5">

      {/* Suspense sẽ hiện Loading khi chuyển trang nếu bạn dùng Lazy Load route */}
      <Suspense
        fallback={<BarLoader className="mt-4" width={"100%"} color="gray" />}
      >
        {/* Nơi hiển thị InterviewPage hoặc MockInterviewPage */}
        <Outlet />
      </Suspense>
    </div>
  );
}