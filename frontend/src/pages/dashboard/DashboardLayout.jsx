import { BarLoader } from "react-spinners";
import { Suspense } from "react";
import { Outlet } from "react-router-dom"; // <--- Thay children bằng Outlet

const DashboardLayout = () => {
  return (
    <div className="px-5">
      <div className="flex items-center justify-between mb-5">
        {/* Class 'gradient-title' phải có trong index.css nhé */}
        <h1 className="text-6xl font-bold gradient-title">Industry Insights</h1>
      </div>

      {/* Suspense bao bọc Outlet */}
      <Suspense
        fallback={<BarLoader className="mt-4" width={"100%"} color="gray" />}
      >
        {/* Outlet là nơi DashboardPage sẽ được render vào */}
        <Outlet />
      </Suspense>
    </div>
  );
};

export default DashboardLayout;