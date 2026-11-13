// Next.js 16.4.2 does not expose a locale for its development menu.
const labels = {
  Insights: "Mục cần xem",
  Insight: "mục cần xem",
  Issues: "Lỗi",
  Issue: "lỗi",
  "No issues": "Không có lỗi",
  Bundler: "Công cụ đóng gói",
  "Turbopack is enabled.": "Turbopack đang hoạt động.",
  "Cache Components": "Lưu đệm thành phần",
  "Cache Components is enabled.": "Tính năng lưu đệm thành phần đang bật.",
  "Navigation Inspector": "Kiểm tra chuyển trang",
  "Test instant navigation behavior.": "Kiểm tra cách nội dung xuất hiện khi chuyển trang.",
  "Route Info": "Thông tin trang",
  "Request Insights": "Thông tin yêu cầu",
  Preferences: "Tùy chọn",
  "Waiting for navigation...": "Đang chờ chuyển trang…",
  "Click any link or refresh the page to inspect the shell.": "Bấm một liên kết hoặc tải lại trang để xem nội dung tải trước.",
  "Loading shell": "Nội dung tải trước",
  "Page load": "Tải trang",
  "Client nav": "Chuyển trang trong trình duyệt",
  "You're viewing the shell for this page's initial load.": "Đây là nội dung tải trước khi mở trang lần đầu.",
  "You're viewing the shell for the current navigation.": "Đây là nội dung tải trước khi chuyển trang.",
  "Pause on navigations": "Tạm dừng khi chuyển trang",
  "When enabled, every navigation will pause so you can inspect the loading shell before resuming.":
    "Khi bật, mỗi lần chuyển trang sẽ tạm dừng để bạn kiểm tra phần nội dung tải trước.",
  Resume: "Tiếp tục",
  Theme: "Giao diện",
  "Select your theme preference.": "Chọn chế độ màu của bảng công cụ.",
  System: "Theo hệ thống",
  Light: "Sáng",
  Dark: "Tối",
  Position: "Vị trí",
  "Adjust the placement of your dev tools.": "Chọn vị trí của bảng công cụ.",
  "Bottom Left": "Dưới bên trái",
  "Bottom Right": "Dưới bên phải",
  "Top Left": "Trên bên trái",
  "Top Right": "Trên bên phải",
  Size: "Kích thước",
  "Adjust the size of your dev tools.": "Điều chỉnh kích thước của bảng công cụ.",
  "Hide Dev Tools for this session": "Ẩn bảng công cụ trong phiên này",
  "Hide Dev Tools until you restart your dev server, or 1 day.": "Ẩn đến khi khởi động lại máy chủ phát triển hoặc sau một ngày.",
  Hide: "Ẩn",
  "Hide Dev Tools shortcut": "Phím tắt ẩn bảng công cụ",
  "Set a custom keyboard shortcut to toggle visibility.": "Đặt phím tắt để ẩn hoặc hiện bảng công cụ.",
  "Disable Dev Tools for this project": "Tắt bảng công cụ cho dự án này",
  "Restart Dev Server": "Khởi động lại máy chủ phát triển",
  "Restarts the development server without needing to leave the browser.": "Khởi động lại máy chủ phát triển ngay trong trình duyệt.",
  Restart: "Khởi động lại",
  "Reset Bundler Cache": "Xóa bộ nhớ đệm của công cụ đóng gói",
  "Clears the bundler cache and restarts the dev server. Helpful if you are seeing stale errors or changes are not appearing.":
    "Xóa bộ nhớ đệm và khởi động lại máy chủ khi lỗi cũ hoặc thay đổi chưa được cập nhật.",
  "Reset Cache": "Xóa bộ nhớ đệm",
  "Open Next.js Dev Tools": "Mở bảng công cụ Next.js",
  "Next.js Dev Tools Items": "Các mục trong bảng công cụ Next.js",
  "Open issues overlay": "Mở chi tiết thông báo",
  "Collapse issues badge": "Thu gọn thông báo",
};

module.exports = function localizeNextDevtools(source) {
  for (const [english, vietnamese] of Object.entries(labels)) {
    source = source.replaceAll(JSON.stringify(english), JSON.stringify(vietnamese));
  }

  return source
    .replaceAll('"Issues \\xb7 Insights"', '"Lỗi · Mục cần xem"')
    .replaceAll('${k?"Close":"Open"} Next.js Dev Tools', '${k?"Đóng":"Mở"} bảng công cụ Next.js')
    .replaceAll('?"insight":"insights"', '?"mục cần xem":"mục cần xem"')
    .replaceAll('?"issue":"issues"', '?"lỗi":"lỗi"')
    .replaceAll(' found. Click to view details in the dev overlay.', '. Bấm để xem chi tiết.')
    .replaceAll('value:"Enabled"', 'value:"Đang bật"')
    .replaceAll('label:"Source",value:', 'label:"Trang nguồn",value:')
    .replaceAll('label:"Target",value:', 'label:"Trang đích",value:')
    .replaceAll(/("data-issues-count-plural":!0,"data-animate":\w+,children:)"s"/g, '$1""');
};
